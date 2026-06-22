package id.baundang.invitation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record GiftConfirmRequest(
        @NotBlank String senderName,
        @Positive long amount,
        String bankFrom,
        String proofUrl,
        String message
) {}
