package id.baundang.notification.service;

public final class MessageTemplates {

    private MessageTemplates() {}

    public static String orderPaidBuyer(String orderNumber, String coupleName, String paymentUrl) {
        return """
                ✅ *Pembayaran Berhasil!*

                Halo, terima kasih telah mempercayai *baundang.id* 🎉

                Detail pesanan Anda:
                • No. Pesanan : %s
                • Nama Pasangan : %s

                Tim kami akan segera memproses undangan digital Anda. \
                Pantau status pesanan di: %s

                Terima kasih telah memilih baundang.id ❤️
                """.formatted(orderNumber, coupleName, paymentUrl);
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
        return """
                ✏️ *Permintaan Revisi #%d*

                • No. Pesanan : %s
                • Nama Pasangan : %s

                Pembeli telah mengajukan revisi. \
                Silakan cek detail revisi di dashboard admin.
                """.formatted(revisionCount, orderNumber, coupleName);
    }

    public static String rsvpSubmittedCouple(String guestName, String invitationTitle,
                                              String attendance, int guestCount) {
        String attendanceLabel = "hadir".equalsIgnoreCase(attendance) ? "akan *HADIR* 🎉" : "tidak dapat hadir 😔";
        return """
                💌 *Konfirmasi Kehadiran Baru!*

                Tamu *%s* %s di acara:
                _%s_

                Jumlah tamu: *%d orang*

                Pantau semua RSVP di dashboard undangan Anda.
                """.formatted(guestName, attendanceLabel, invitationTitle, guestCount);
    }

    public static String invitationExpiring(String coupleName, String invitationTitle, int daysLeft) {
        return """
                ⚠️ *Pengingat: Undangan Hampir Kedaluwarsa*

                Halo *%s*,

                Undangan digital Anda:
                _%s_

                akan kedaluwarsa dalam *%d hari* lagi.

                Hubungi kami jika ingin memperpanjang masa aktif undangan.
                baundang.id ❤️
                """.formatted(coupleName, invitationTitle, daysLeft);
    }
}
