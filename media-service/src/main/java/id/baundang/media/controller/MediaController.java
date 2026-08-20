package id.baundang.media.controller;

import id.baundang.common.ApiResponse;
import id.baundang.media.dto.PresignDownloadResponse;
import id.baundang.media.dto.PresignUploadRequest;
import id.baundang.media.dto.PresignUploadResponse;
import id.baundang.media.dto.UploadedObjectResponse;
import id.baundang.media.service.MinioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.time.Duration;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
public class MediaController {

    private final MinioService minioService;

    @PostMapping("/upload/presign")
    public ResponseEntity<ApiResponse<PresignUploadResponse>> presignUpload(
            @Valid @RequestBody PresignUploadRequest req) {
        PresignUploadResponse resp = minioService.presignUpload(req);
        return ResponseEntity
                .created(URI.create("/api/v1/media/download/" + resp.objectKey()))
                .body(ApiResponse.ok(resp));
    }

    /**
     * Public read for a couple's uploaded photos, so an invitation page can render them
     * for unauthenticated guests. Restricted to the {@code couples/} prefix in
     * {@link MinioService#streamPublicObject}; nothing else is reachable this way.
     */
    @GetMapping("/public/**")
    public ResponseEntity<Resource> publicObject(HttpServletRequest request) {
        String objectKey = new AntPathMatcher().extractPathWithinPattern(
                "/api/v1/media/public/**",
                request.getRequestURI().substring(request.getContextPath().length()));

        MinioService.PublicObject object = minioService.streamPublicObject(objectKey);
        MediaType contentType = object.contentType() != null
                ? MediaType.parseMediaType(object.contentType())
                : MediaType.APPLICATION_OCTET_STREAM;

        return ResponseEntity.ok()
                .contentType(contentType)
                .contentLength(object.size())
                // Keys are immutable (UUID-prefixed per upload), so these can cache hard.
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                .body(new InputStreamResource(object.stream()));
    }

    @GetMapping("/download/**")
    public ApiResponse<PresignDownloadResponse> presignDownload(
            @RequestParam String objectKey) {
        return ApiResponse.ok(minioService.presignDownload(objectKey));
    }

    @DeleteMapping("/**")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteObject(@RequestParam String objectKey) {
        minioService.deleteObject(objectKey);
        return ApiResponse.ok(null, "Object deleted: " + objectKey);
    }

    @PostMapping("/template/upload")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UploadedObjectResponse>> uploadTemplate(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "thumbnails") String subfolder) {
        UploadedObjectResponse resp = minioService.uploadTemplate(file, subfolder);
        return ResponseEntity
                .created(URI.create(resp.publicUrl()))
                .body(ApiResponse.ok(resp));
    }
}
