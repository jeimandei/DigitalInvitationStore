package id.baundang.media.controller;

import id.baundang.common.ApiResponse;
import id.baundang.media.dto.PresignDownloadResponse;
import id.baundang.media.dto.PresignUploadRequest;
import id.baundang.media.dto.PresignUploadResponse;
import id.baundang.media.dto.UploadedObjectResponse;
import id.baundang.media.service.MinioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;

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
