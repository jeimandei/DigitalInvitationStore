package id.baundang.media.controller;

import id.baundang.common.ApiResponse;
import id.baundang.media.dto.*;
import id.baundang.media.service.MinioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
public class MediaController {

    private final MinioService minioService;

    /**
     * Buyer requests a presigned PUT URL to upload directly to MinIO.
     * The client should PUT the file bytes to the returned presignedUrl
     * with the matching Content-Type header.
     */
    @PostMapping("/upload/presign")
    public ResponseEntity<ApiResponse<PresignUploadResponse>> presignUpload(
            @Valid @RequestBody PresignUploadRequest req) {
        PresignUploadResponse resp = minioService.presignUpload(req);
        return ResponseEntity
                .created(URI.create("/api/v1/media/download/" + resp.objectKey()))
                .body(ApiResponse.ok(resp));
    }

    /**
     * Buyer requests a presigned GET URL to download/view a private object.
     * The objectKey path may contain slashes (e.g. couples/slug/uuid-file.jpg).
     */
    @GetMapping("/download/**")
    public ApiResponse<PresignDownloadResponse> presignDownload(
            @RequestParam String objectKey) {
        return ApiResponse.ok(minioService.presignDownload(objectKey));
    }

    /**
     * Admin hard-deletes an object from whichever bucket owns it.
     */
    @DeleteMapping("/**")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteObject(@RequestParam String objectKey) {
        minioService.deleteObject(objectKey);
        return ApiResponse.ok(null, "Object deleted: " + objectKey);
    }

    /**
     * Admin uploads a file directly (server-side) to baundang-templates.
     * Subfolder defaults to "thumbnails"; pass ?subfolder=previews for preview images.
     */
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
