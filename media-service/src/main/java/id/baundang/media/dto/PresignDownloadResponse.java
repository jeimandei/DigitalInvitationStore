package id.baundang.media.dto;

public record PresignDownloadResponse(
        String presignedUrl,
        int expiresInHours
) {}
