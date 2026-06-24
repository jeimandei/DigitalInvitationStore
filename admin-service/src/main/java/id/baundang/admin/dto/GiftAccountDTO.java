package id.baundang.admin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GiftAccountDTO(
        String bankName,
        String accountNumber,
        String accountHolder,
        String gopayNumber,
        String ovoNumber,
        String qrisImageUrl
) {}
