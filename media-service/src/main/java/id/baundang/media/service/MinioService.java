package id.baundang.media.service;

import id.baundang.common.exception.ValidationException;
import id.baundang.media.config.MediaProperties;
import id.baundang.media.dto.*;
import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient minioClient;
    private final MediaProperties mediaProperties;

    @Value("${app.minio.endpoint}")
    private String endpoint;

    @Value("${app.minio.bucket.templates}")
    private String templatesBucket;

    @Value("${app.minio.bucket.couples}")
    private String couplesBucket;

    @Value("${app.minio.bucket.admin}")
    private String adminBucket;

    public PresignUploadResponse presignUpload(PresignUploadRequest req) {
        validateContentType(req.contentType());

        String sanitized = sanitizeFilename(req.filename());
        String objectKey = req.folder() + "/" + UUID.randomUUID() + "-" + sanitized;

        Map<String, String> extraHeaders = Map.of(
                "Content-Type", req.contentType()
        );

        try {
            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(couplesBucket)
                            .object(objectKey)
                            .expiry(mediaProperties.getPresignPutExpiryMinutes(), TimeUnit.MINUTES)
                            .extraHeaders(extraHeaders)
                            .build()
            );
            return new PresignUploadResponse(url, objectKey, mediaProperties.getPresignPutExpiryMinutes());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate presigned upload URL", e);
        }
    }

    public PresignDownloadResponse presignDownload(String objectKey) {
        String bucket = bucketForKey(objectKey);
        try {
            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(objectKey)
                            .expiry(mediaProperties.getPresignGetExpiryHours(), TimeUnit.HOURS)
                            .build()
            );
            return new PresignDownloadResponse(url, mediaProperties.getPresignGetExpiryHours());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate presigned download URL", e);
        }
    }

    public void deleteObject(String objectKey) {
        String bucket = bucketForKey(objectKey);
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder().bucket(bucket).object(objectKey).build()
            );
            log.info("Deleted object {}/{}", bucket, objectKey);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete object: " + objectKey, e);
        }
    }

    public UploadedObjectResponse uploadTemplate(MultipartFile file, String subfolder) {
        validateContentType(file.getContentType());
        validateFileSize(file.getSize(), file.getContentType());

        String sanitized = sanitizeFilename(file.getOriginalFilename());
        String objectKey = subfolder + "/" + UUID.randomUUID() + "-" + sanitized;

        try (InputStream is = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(templatesBucket)
                            .object(objectKey)
                            .stream(is, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload template file", e);
        }

        String publicUrl = endpoint + "/" + templatesBucket + "/" + objectKey;
        return new UploadedObjectResponse(objectKey, publicUrl);
    }

    // --- helpers ---

    private void validateContentType(String contentType) {
        if (contentType == null || !mediaProperties.allowedTypeSet().contains(contentType)) {
            throw new ValidationException(
                    "Content type not allowed: " + contentType +
                    ". Allowed: " + mediaProperties.getAllowedTypes()
            );
        }
    }

    private void validateFileSize(long size, String contentType) {
        long max = mediaProperties.maxBytesFor(contentType);
        if (size > max) {
            throw new ValidationException(
                    "File size %d exceeds limit of %d bytes for type %s"
                    .formatted(size, max, contentType)
            );
        }
    }

    private String sanitizeFilename(String filename) {
        if (filename == null) return "file";
        return filename.replaceAll("[^a-zA-Z0-9.\\-_]", "_").toLowerCase();
    }

    private String bucketForKey(String objectKey) {
        if (objectKey.startsWith("couples/")) return couplesBucket;
        if (objectKey.startsWith("admin/"))   return adminBucket;
        return templatesBucket;
    }
}
