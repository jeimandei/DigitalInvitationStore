package id.baundang.invitation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.baundang.invitation.config.GatewayHeaderFilter;
import id.baundang.invitation.service.InvitationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = InvitationApiController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class}
)
@AutoConfigureMockMvc(addFilters = false)
class InvitationApiControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    InvitationService invitationService;

    @MockBean
    GatewayHeaderFilter gatewayHeaderFilter;

    @Test
    void submitRsvp_returns200() throws Exception {
        doNothing().when(invitationService).submitRsvp(any(), any());

        String body = "{\"guestName\":\"Budi\",\"attendance\":\"hadir\",\"guestCount\":2}";
        mockMvc.perform(post("/api/v1/invitations/test-slug/rsvp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void listGuestbook_returns200() throws Exception {
        when(invitationService.listApprovedGuestbook("test-slug")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/invitations/test-slug/guestbook"))
                .andExpect(status().isOk());
    }

    @Test
    void submitGuestbook_returns200() throws Exception {
        doNothing().when(invitationService).submitGuestbook(any(), any());

        String body = "{\"guestName\":\"Budi\",\"message\":\"Selamat!\"}";
        mockMvc.perform(post("/api/v1/invitations/test-slug/guestbook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void listEvents_returns200() throws Exception {
        when(invitationService.getEvents("test-slug")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/invitations/test-slug/events"))
                .andExpect(status().isOk());
    }

    @Test
    void listExpiring_returns200() throws Exception {
        when(invitationService.findExpiring(7)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/invitations/expiring?days=7"))
                .andExpect(status().isOk());
    }

    @Test
    void listActivePhones_returns200() throws Exception {
        when(invitationService.listActivePhones()).thenReturn(List.of("+628123456789"));

        mockMvc.perform(get("/api/v1/admin/invitations/active-phones"))
                .andExpect(status().isOk());
    }

    @Test
    void approveGuestbook_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        UUID entryId = UUID.randomUUID();
        doNothing().when(invitationService).approveGuestbook(eq(id), eq(entryId));

        mockMvc.perform(put("/api/v1/admin/invitations/" + id + "/approve-guestbook/" + entryId))
                .andExpect(status().isOk());
    }

    @Test
    void updateContent_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(invitationService.updateContent(eq(id), any())).thenReturn(null);

        mockMvc.perform(put("/api/v1/admin/invitations/" + id + "/content")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"coupleName\":\"Budi & Sari\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void updateStatus_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(invitationService.updateStatus(any(), any())).thenReturn(null);

        mockMvc.perform(put("/api/v1/admin/invitations/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void getGiftAccount_returns200() throws Exception {
        when(invitationService.getGiftAccount("test-slug")).thenReturn(null);

        mockMvc.perform(get("/api/v1/invitations/test-slug/gift-accounts"))
                .andExpect(status().isOk());
    }

    @Test
    void submitGiftConfirmation_returns200() throws Exception {
        doNothing().when(invitationService).submitGiftConfirmation(any(), any());

        String body = "{\"senderName\":\"Budi\",\"amount\":100000}";
        mockMvc.perform(post("/api/v1/invitations/test-slug/gift-confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void setGiftAccount_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(invitationService).setGiftAccount(any(), any());

        mockMvc.perform(put("/api/v1/admin/invitations/" + id + "/gift-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void listInvitations_returns200() throws Exception {
        when(invitationService.listInvitations(any())).thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/admin/invitations"))
                .andExpect(status().isOk());
    }

    @Test
    void listAllGuestbook_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(invitationService.listAllGuestbook(id)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/admin/invitations/" + id + "/guestbook"))
                .andExpect(status().isOk());
    }

    @Test
    void listRsvp_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(invitationService.listRsvp(id)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/admin/invitations/" + id + "/rsvp"))
                .andExpect(status().isOk());
    }
}
