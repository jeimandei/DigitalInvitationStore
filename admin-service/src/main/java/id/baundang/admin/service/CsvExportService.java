package id.baundang.admin.service;

import id.baundang.admin.client.InvitationAdminClient;
import id.baundang.admin.client.OrderAdminClient;
import id.baundang.admin.dto.OrderDTO;
import id.baundang.admin.dto.PagedResult;
import id.baundang.admin.dto.RsvpEntryDTO;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CsvExportService {

    private final OrderAdminClient orderClient;
    private final InvitationAdminClient invitationClient;

    public void exportOrders(Writer writer, String status) throws IOException {
        PagedResult<OrderDTO> result = orderClient.listOrders(0, 10000, status, null);
        List<OrderDTO> orders = result.content();

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader("No. Order", "Nama Pembeli", "Email", "Tier", "Status",
                        "Tanggal Order", "Tanggal Bayar", "Jumlah")
                .build();

        try (CSVPrinter printer = new CSVPrinter(writer, format)) {
            for (OrderDTO o : orders) {
                printer.printRecord(
                        o.orderNumber(),
                        o.buyerName(),
                        o.buyerEmail(),
                        tierLabel(o.tier()),
                        o.status(),
                        o.createdAt() != null ? o.createdAt().toString() : "",
                        o.paidAt() != null ? o.paidAt().toString() : "",
                        tierPrice(o.tier())
                );
            }
        }
    }

    public void exportRsvp(Writer writer, UUID invitationId) throws IOException {
        List<RsvpEntryDTO> rsvpList = invitationClient.listRsvp(invitationId);

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader("Nama", "No. HP", "Kehadiran", "Jumlah Tamu", "Pesan", "Waktu")
                .build();

        try (CSVPrinter printer = new CSVPrinter(writer, format)) {
            for (RsvpEntryDTO rsvp : rsvpList) {
                printer.printRecord(
                        rsvp.guestName(),
                        rsvp.phone(),
                        rsvp.attendance(),
                        rsvp.guestCount(),
                        rsvp.message(),
                        rsvp.submittedAt() != null ? rsvp.submittedAt().toString() : ""
                );
            }
        }
    }

    private String tierLabel(short tier) {
        return switch (tier) {
            case 1 -> "Dasar";
            case 2 -> "Standar";
            case 3 -> "Premium";
            default -> "Unknown";
        };
    }

    private long tierPrice(short tier) {
        return switch (tier) {
            case 1 -> 99_000L;
            case 2 -> 199_000L;
            case 3 -> 349_000L;
            default -> 0L;
        };
    }
}
