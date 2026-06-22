package id.baundang.media.dto;

public record PresignUploadResponse(
        String presignedUrl,
        String objectKey,
        int expiresInMinutes
) {}
