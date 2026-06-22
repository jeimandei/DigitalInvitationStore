package id.baundang.payment.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record ChargeRequest(
        @NotNull UUID orderId,
        @NotNull @Positive Long amount,
        @NotBlank String coupleName,
        @NotBlank @Email String contactEmail,
        @NotBlank String contactWhatsapp
) {}
