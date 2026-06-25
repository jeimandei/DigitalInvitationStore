package id.baundang.order.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record CreateOrderRequest(
        UUID templateId,
        @NotNull Short tier,
        @NotBlank String coupleName,
        @NotBlank @Pattern(regexp = "^\\+?[0-9]{8,15}$") String contactWhatsapp,
        @NotBlank @Email String contactEmail,
        String notes
) {}
