package id.baundang.invitation.dto;

import id.baundang.invitation.domain.GiftAccount;

public record GiftAccountDTO(
        String bankName,
        String accountNumber,
        String accountHolder,
        String gopayNumber,
        String ovoNumber,
        String qrisImageUrl
) {
    public static GiftAccountDTO from(GiftAccount a) {
        return new GiftAccountDTO(
                a.getBankName(), a.getAccountNumber(), a.getAccountHolder(),
                a.getGopayNumber(), a.getOvoNumber(), a.getQrisImageUrl()
        );
    }
}
