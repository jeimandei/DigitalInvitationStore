package id.baundang.media.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PresignUploadRequest(
        @NotBlank String filename,
        @NotBlank String contentType,
        // must start with "couples/" to scope uploads per couple
        @NotBlank @Pattern(regexp = "couples/[a-z0-9\\-]+") String folder
) {}
