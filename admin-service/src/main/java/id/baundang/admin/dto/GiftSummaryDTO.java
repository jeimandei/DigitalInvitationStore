package id.baundang.admin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GiftSummaryDTO(
        long totalGifts,
        long totalAmount,
        List<GiftEntryDTO> entries
) {}
