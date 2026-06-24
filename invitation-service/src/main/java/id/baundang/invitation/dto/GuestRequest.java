package id.baundang.invitation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record GuestRequest(
        @NotBlank String name,
        String groupLabel,
        String tableNo,
        @Positive short allottedCount
) {}
