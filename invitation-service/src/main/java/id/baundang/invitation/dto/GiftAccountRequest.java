package id.baundang.invitation.dto;

public record GiftAccountRequest(
        String bankName,
        String accountNumber,
        String accountHolder,
        String gopayNumber,
        String ovoNumber,
        String qrisImageUrl
) {}
