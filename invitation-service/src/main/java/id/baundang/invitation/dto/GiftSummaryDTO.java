package id.baundang.invitation.dto;

import java.util.List;

public record GiftSummaryDTO(
        long totalGifts,
        long totalAmount,
        List<GiftEntryDTO> entries
) {}
