package id.baundang.admin.service;

import id.baundang.admin.client.InvitationAdminClient;
import id.baundang.admin.client.OrderAdminClient;
import id.baundang.admin.dto.DashboardStats;
import id.baundang.admin.dto.OrderDTO;
import id.baundang.admin.dto.PagedResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final OrderAdminClient orderClient;
    private final InvitationAdminClient invitationClient;

    public DashboardStats buildStats() {
        PagedResult<OrderDTO> allOrders = orderClient.listOrders(0, 1000, null, null);
        List<OrderDTO> orders = allOrders.content();

        long ordersToday = orders.stream()
                .filter(o -> o.createdAt() != null &&
                        o.createdAt().isAfter(java.time.Instant.now().minus(1, java.time.temporal.ChronoUnit.DAYS)))
                .count();

        long revenueToday = orders.stream()
                .filter(o -> "PAID".equals(o.status()) && o.paidAt() != null &&
                        o.paidAt().isAfter(java.time.Instant.now().minus(1, java.time.temporal.ChronoUnit.DAYS)))
                .mapToLong(o -> o.amount() > 0 ? o.amount() : tierPrice(o.tier()))
                .sum();

        long pending = orders.stream().filter(OrderDTO::isPending).count();
        long totalRevenue = orders.stream()
                .filter(OrderDTO::isPaid)
                .mapToLong(o -> o.amount() > 0 ? o.amount() : tierPrice(o.tier()))
                .sum();

        PagedResult<?> invitations = invitationClient.listInvitations(0, 1);
        long activeInvitations = invitations.totalElements();

        return new DashboardStats(ordersToday, revenueToday, pending,
                allOrders.totalElements(), totalRevenue, activeInvitations);
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
