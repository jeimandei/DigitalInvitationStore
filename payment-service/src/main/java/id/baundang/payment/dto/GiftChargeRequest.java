package id.baundang.payment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record GiftChargeRequest(
        @NotNull UUID invitationId,
        @NotBlank String senderName,
        String message,
        @NotNull @Min(20000) Long amount
) {}
