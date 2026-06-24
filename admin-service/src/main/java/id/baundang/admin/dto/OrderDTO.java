package id.baundang.admin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderDTO(
        UUID id,
        String orderNumber,
        UUID buyerId,
        UUID templateId,
        short tier,
        String coupleName,
        String contactWhatsapp,
        String contactEmail,
        String status,
        String midtransTransactionId,
        Instant paidAt,
        short revisionCount,
        short maxRevisions,
        String coupleSlug,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
    public boolean isPending() {
        return "PENDING".equals(status);
    }

    public boolean isPaid() {
        return "PAID".equals(status);
    }

    public boolean isCompleted() {
        return "COMPLETED".equals(status);
    }

    public boolean isCancelled() {
        return "CANCELLED".equals(status);
    }

    public String buyerName() {
        return coupleName;
    }

    public String buyerEmail() {
        return contactEmail;
    }
}
