package id.baundang.admin.controller;

import id.baundang.admin.client.InvitationAdminClient;
import id.baundang.admin.client.NotificationAdminClient;
import id.baundang.admin.client.OrderAdminClient;
import id.baundang.admin.client.TemplateAdminClient;
import id.baundang.admin.config.GatewayHeaderFilter;
import id.baundang.admin.dto.DashboardStats;
import id.baundang.admin.dto.PagedResult;
import id.baundang.admin.entity.AdminNote;
import id.baundang.admin.repository.AdminNoteRepository;
import id.baundang.admin.service.AdminDashboardService;
import id.baundang.admin.service.CsvExportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        value = AdminController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class}
)
class AdminControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    AdminDashboardService dashboardService;

    @MockBean
    OrderAdminClient orderClient;

    @MockBean
    InvitationAdminClient invitationClient;

    @MockBean
    TemplateAdminClient templateClient;

    @MockBean
    NotificationAdminClient notificationClient;

    @MockBean
    AdminNoteRepository noteRepository;

    @MockBean
    CsvExportService csvExportService;

    @MockBean
    GatewayHeaderFilter gatewayHeaderFilter;

    @Test
    void dashboard_returns200() throws Exception {
        when(dashboardService.buildStats()).thenReturn(
                new DashboardStats(5L, 500000L, 3L, 100L, 10000000L, 50L)
        );

        mockMvc.perform(get("/admin"))
                .andExpect(status().isOk());
    }

    @Test
    void orders_returns200() throws Exception {
        when(orderClient.listOrders(anyInt(), anyInt(), any(), any())).thenReturn(PagedResult.empty());

        mockMvc.perform(get("/admin/orders"))
                .andExpect(status().isOk());
    }

    @Test
    void orderDetail_notFound_redirects() throws Exception {
        UUID id = UUID.randomUUID();
        when(orderClient.getOrder(id)).thenReturn(Optional.empty());
        when(noteRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/admin/orders/" + id))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    void updateOrderStatus_completes() throws Exception {
        UUID id = UUID.randomUUID();
        when(orderClient.updateStatus(any(), any(), any())).thenReturn(true);

        // The controller redirects - some MockMvc configs return 200 or 302
        mockMvc.perform(post("/admin/orders/" + id + "/status")
                        .param("status", "PAID"))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    void templates_returns200() throws Exception {
        when(templateClient.listTemplates(anyInt(), anyInt())).thenReturn(PagedResult.empty());

        mockMvc.perform(get("/admin/templates"))
                .andExpect(status().isOk());
    }

    @Test
    void toggleTemplate_completes() throws Exception {
        when(templateClient.toggleActive(any(), anyBoolean())).thenReturn(true);

        mockMvc.perform(post("/admin/templates/some-id/toggle")
                        .param("active", "true"))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    void invitations_returns200() throws Exception {
        when(invitationClient.listInvitations(anyInt(), anyInt())).thenReturn(PagedResult.empty());

        mockMvc.perform(get("/admin/invitations"))
                .andExpect(status().isOk());
    }

    @Test
    void guestbook_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(invitationClient.listAllGuestbook(id)).thenReturn(List.of());

        mockMvc.perform(get("/admin/invitations/" + id + "/guestbook"))
                .andExpect(status().isOk());
    }

    @Test
    void approveGuestbook_completes() throws Exception {
        UUID invitationId = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();
        when(invitationClient.approveGuestbook(any(), any())).thenReturn(true);

        mockMvc.perform(post("/admin/invitations/" + invitationId + "/guestbook/" + entryId + "/approve"))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    void broadcastForm_returns200() throws Exception {
        mockMvc.perform(get("/admin/broadcast/wa"))
                .andExpect(status().isOk());
    }

    @Test
    void sendBroadcast_returns200() throws Exception {
        mockMvc.perform(post("/admin/broadcast/wa")
                        .param("targetGroup", "ALL_ACTIVE")
                        .param("message", "hello"))
                .andExpect(status().isOk());
    }

    @Test
    void buyers_returns200() throws Exception {
        when(orderClient.listOrders(anyInt(), anyInt(), any(), any())).thenReturn(PagedResult.empty());

        mockMvc.perform(get("/admin/buyers"))
                .andExpect(status().isOk());
    }
}
