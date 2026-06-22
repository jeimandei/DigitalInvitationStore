package id.baundang.admin.dto;

public record DashboardStats(
        long ordersToday,
        long revenueToday,
        long pendingOrders,
        long totalOrders,
        long totalRevenue,
        long activeInvitations
) {}
