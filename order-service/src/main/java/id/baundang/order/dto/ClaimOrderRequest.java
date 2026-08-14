package id.baundang.order.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Binds an order placed anonymously to the authenticated account. The caller proves
 * they own the order the same way the public tracker does: the order number plus the
 * email or WhatsApp number recorded on it.
 */
public record ClaimOrderRequest(
        @NotBlank String orderNumber,
        @NotBlank String contact
) {}
