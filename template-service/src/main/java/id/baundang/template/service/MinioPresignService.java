package id.baundang.template.service;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class MinioPresignService {

    private static final Logger LOG = LoggerFactory.getLogger(MinioPresignService.class);

    private final MinioClient minioClient;

    @Value("${app.minio.bucket}")
    private String bucket;

    @Value("${app.minio.presign-expiry-minutes:60}")
    private int expiryMinutes;

    public MinioPresignService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    public String presignedPreviewUrl(String objectKey) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(objectKey)
                            .expiry(expiryMinutes, TimeUnit.MINUTES)
                            .build());
        } catch (Exception e) {
            LOG.error("Failed to generate presigned URL for object {}: {}", objectKey, e.getMessage());
            throw new IllegalStateException("Could not generate preview URL", e);
        }
    }
}
