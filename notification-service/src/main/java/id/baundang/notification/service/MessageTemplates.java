package id.baundang.notification.service;

import java.util.Map;

public final class MessageTemplates {

    private MessageTemplates() {}

    public static final String ORDER_CONFIRMED =
            "✅ *Pesanan Dikonfirmasi!*\n\nHalo {coupleName}! Pesanan Anda *{orderNumber}* telah dikonfirmasi.\n\n"
            + "Lakukan pembayaran untuk memulai proses undangan:\n{paymentUrl}\n\nbaundang.id ❤️";

    public static final String ORDER_PAID =
            "💳 *Pembayaran Diterima!*\n\nHalo {coupleName}! Pembayaran untuk pesanan *{orderNumber}*"
            + " telah kami terima.\n\nTim kami akan segera memproses undangan digital Anda. Pantau status di:\n"
            + "{orderUrl}\n\nTerima kasih telah memilih baundang.id ❤️";

    public static final String REVISION_REQUESTED =
            "✏️ *Permintaan Revisi #{revisionCount}*\n\nPesanan: *{orderNumber}*\n"
            + "Nama Pasangan: *{coupleName}*\n\nPembeli telah mengajukan revisi."
            + " Silakan cek detail revisi di dashboard admin.";

    public static final String REVISION_COMPLETED =
            "✅ *Revisi Undangan Selesai!*\n\nHalo {coupleName}! Revisi untuk pesanan *{orderNumber}*"
            + " telah selesai.\n\nLihat undangan Anda yang sudah diperbarui:\n{invitationUrl}\n\nbaundang.id ❤️";

    public static final String RSVP_RECEIVED =
            "💌 *Konfirmasi Kehadiran Baru!*\n\nHalo {coupleName}! Tamu *{guestName}* {attendanceLabel}"
            + " di acara Anda.\n\nJumlah tamu: *{guestCount} orang*\n\nPantau semua RSVP di dashboard undangan Anda.";

    public static final String INVITATION_EXPIRING =
            "⚠️ *Undangan Hampir Kedaluwarsa!*\n\nHalo {coupleName}! Undangan digital Anda akan kedaluwarsa"
            + " dalam *{daysLeft} hari* lagi.\n\nHubungi kami jika ingin memperpanjang masa aktif undangan."
            + "\nbaundang.id ❤️";

    public static final String GIFT_CONFIRMED =
            "💝 *Konfirmasi Hadiah Diterima!*\n\nHalo {coupleName}! *{senderName}* telah mengirimkan"
            + " hadiah sebesar *Rp {amount}*{bankInfo}.\n\nCek dashboard undangan Anda untuk detail."
            + "\nbaundang.id ❤️";

    public static final String INVITATION_LIVE =
            "🎉 *Undangan Digital Siap!*\n\nHalo {coupleName}! Undangan digital Anda sudah aktif"
            + " dan bisa diakses di:\n{invitationUrl}\n\nBagikan link tersebut kepada tamu-tamu Anda."
            + "\nbaundang.id ❤️";

    public static String format(String template, Map<String, String> vars) {
        String result = template;
        for (Map.Entry<String, String> e : vars.entrySet()) {
            result = result.replace("{" + e.getKey() + "}", e.getValue() != null ? e.getValue() : "");
        }
        return result;
    }

    // ── Legacy helpers kept for backward-compat with existing consumers ────────

    public static String orderPaidBuyer(String orderNumber, String coupleName, String paymentUrl) {
        return format(ORDER_PAID, Map.of(
                "coupleName", coupleName,
                "orderNumber", orderNumber,
                "orderUrl", paymentUrl
        ));
    }

    public static String orderPaidEmailBuyer(String orderNumber, String coupleName,
                                              long amount, String dashboardUrl) {
        return """
                Halo %s,

                Pembayaran untuk pesanan Anda telah kami terima. Berikut detailnya:

                  No. Pesanan : %s
                  Total Bayar : Rp %,d

                Tim kami akan segera memproses undangan digital Anda dan menghubungi Anda via WhatsApp.
                Pantau status pesanan di: %s

                Terima kasih telah memilih baundang.id ❤️

                —
                Tim baundang.id
                """.formatted(coupleName, orderNumber, amount, dashboardUrl);
    }

    public static String orderPaidAdmin(String orderNumber, String coupleName,
                                        String contactEmail, String contactWhatsapp,
                                        long amount) {
        return """
                🔔 *Pesanan Baru Masuk!*

                • No. Pesanan : %s
                • Nama Pasangan : %s
                • Email : %s
                • WhatsApp : %s
                • Total : Rp %,d

                Segera proses pesanan ini di dashboard admin.
                """.formatted(orderNumber, coupleName, contactEmail, contactWhatsapp, amount);
    }

    public static String orderRevisedAdmin(String orderNumber, String coupleName, int revisionCount) {
        return format(REVISION_REQUESTED, Map.of(
                "revisionCount", String.valueOf(revisionCount),
                "orderNumber", orderNumber,
                "coupleName", coupleName
        ));
    }

    public static String rsvpSubmittedCouple(String guestName, String invitationTitle,
                                              String attendance, int guestCount) {
        String attendanceLabel = "hadir".equalsIgnoreCase(attendance) ? "akan *HADIR* 🎉" : "tidak dapat hadir 😔";
        return format(RSVP_RECEIVED, Map.of(
                "coupleName", invitationTitle,
                "guestName", guestName,
                "attendanceLabel", attendanceLabel,
                "guestCount", String.valueOf(guestCount)
        ));
    }

    public static String giftConfirmedCouple(String coupleName, String senderName,
                                              long amount, String bankFrom) {
        String bankInfo = (bankFrom != null && !bankFrom.isBlank()) ? " via " + bankFrom : "";
        return format(GIFT_CONFIRMED, Map.of(
                "coupleName", coupleName,
                "senderName", senderName,
                "amount", String.format("%,d", amount),
                "bankInfo", bankInfo
        ));
    }

    public static String revisionCompleted(String orderNumber, String invitationUrl) {
        return format(REVISION_COMPLETED, Map.of(
                "coupleName", "",
                "orderNumber", orderNumber,
                "invitationUrl", invitationUrl
        ));
    }

    public static String invitationExpiring(String coupleName, String invitationTitle, int daysLeft) {
        return format(INVITATION_EXPIRING, Map.of(
                "coupleName", coupleName,
                "daysLeft", String.valueOf(daysLeft)
        ));
    }
}
