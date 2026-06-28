package id.baundang.invitation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import id.baundang.common.GlobalExceptionHandler;
import id.baundang.invitation.config.GatewayHeaderFilter;
import id.baundang.invitation.domain.Invitation;
import id.baundang.invitation.dto.AttendanceDTO;
import id.baundang.invitation.dto.GiftSummaryDTO;
import id.baundang.invitation.dto.GuestDTO;
import id.baundang.invitation.repository.InvitationRepository;
import id.baundang.invitation.service.InvitationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = MyInvitationApiController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class}
)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class MyInvitationApiControllerTest {

    private static final String BUYER = "11111111-1111-1111-1111-111111111111";
    private static final String OTHER = "22222222-2222-2222-2222-222222222222";
    private static final UUID ORDER = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    InvitationService invitationService;

    @MockBean
    InvitationRepository invitationRepository;

    @MockBean
    GatewayHeaderFilter gatewayHeaderFilter;

    private final Principal buyerPrincipal = () -> BUYER;
    private final Principal otherPrincipal = () -> OTHER;

    private Invitation ownedByBuyer() {
        Invitation inv = new Invitation();
        inv.setId(UUID.randomUUID());
        inv.setOrderId(ORDER);
        inv.setCoupleSlug("budi-sari");
        ObjectNode content = objectMapper.createObjectNode();
        content.put("buyerId", BUYER);
        content.put("coupleName", "Budi & Sari");
        inv.setContent(content);
        return inv;
    }

    @BeforeEach
    void setUp() {
        when(invitationRepository.findByOrderId(ORDER)).thenReturn(Optional.of(ownedByBuyer()));
    }

    private GuestDTO sampleGuest() {
        return new GuestDTO(UUID.randomUUID(), "Budi", "abc123", "Keluarga",
                "A1", (short) 2, false, null, (short) 0);
    }

    // ── Ownership / token enforcement ──────────────────────────────────────────

    @Test
    void getMyInvitation_owner_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/invitations/my/" + ORDER).principal(buyerPrincipal))
                .andExpect(status().isOk());
    }

    @Test
    void getMyInvitation_notOwner_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/invitations/my/" + ORDER).principal(otherPrincipal))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMyInvitation_noPrincipal_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/invitations/my/" + ORDER))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMyInvitation_notFound_returns404() throws Exception {
        UUID unknown = UUID.randomUUID();
        when(invitationRepository.findByOrderId(unknown)).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/v1/invitations/my/" + unknown).principal(buyerPrincipal))
                .andExpect(status().isNotFound());
    }

    // ── Guests ──────────────────────────────────────────────────────────────--

    @Test
    void listGuests_owner_returns200() throws Exception {
        when(invitationService.listGuests(any())).thenReturn(List.of(sampleGuest()));
        mockMvc.perform(get("/api/v1/invitations/my/" + ORDER + "/guests").principal(buyerPrincipal))
                .andExpect(status().isOk());
    }

    @Test
    void listGuests_notOwner_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/invitations/my/" + ORDER + "/guests").principal(otherPrincipal))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addGuest_validBody_returns200() throws Exception {
        when(invitationService.addGuest(any(), any())).thenReturn(sampleGuest());
        String body = "{\"name\":\"Budi\",\"groupLabel\":\"Keluarga\",\"tableNo\":\"A1\",\"allottedCount\":2}";
        mockMvc.perform(post("/api/v1/invitations/my/" + ORDER + "/guests")
                        .principal(buyerPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void addGuest_blankName_returns400() throws Exception {
        String body = "{\"name\":\"\",\"allottedCount\":2}";
        mockMvc.perform(post("/api/v1/invitations/my/" + ORDER + "/guests")
                        .principal(buyerPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addGuest_nonPositiveAllotted_returns400() throws Exception {
        String body = "{\"name\":\"Budi\",\"allottedCount\":0}";
        mockMvc.perform(post("/api/v1/invitations/my/" + ORDER + "/guests")
                        .principal(buyerPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void removeGuest_owner_returns200() throws Exception {
        UUID guestId = UUID.randomUUID();
        doNothing().when(invitationService).removeGuest(any(), eq(guestId));
        mockMvc.perform(delete("/api/v1/invitations/my/" + ORDER + "/guests/" + guestId)
                        .principal(buyerPrincipal))
                .andExpect(status().isOk());
    }

    // ── RSVP / Attendance / Gifts / Guestbook ──────────────────────────────────

    @Test
    void listRsvp_owner_returns200() throws Exception {
        when(invitationService.listRsvp(any())).thenReturn(List.of());
        mockMvc.perform(get("/api/v1/invitations/my/" + ORDER + "/rsvp").principal(buyerPrincipal))
                .andExpect(status().isOk());
    }

    @Test
    void getAttendance_owner_returns200() throws Exception {
        when(invitationService.getAttendance(any())).thenReturn(AttendanceDTO.of(0, 0, 0, 0));
        mockMvc.perform(get("/api/v1/invitations/my/" + ORDER + "/attendance").principal(buyerPrincipal))
                .andExpect(status().isOk());
    }

    @Test
    void getGifts_owner_returns200() throws Exception {
        when(invitationService.getGiftSummary(any())).thenReturn(new GiftSummaryDTO(0, 0, List.of()));
        mockMvc.perform(get("/api/v1/invitations/my/" + ORDER + "/gifts").principal(buyerPrincipal))
                .andExpect(status().isOk());
    }

    @Test
    void listGuestbook_owner_returns200() throws Exception {
        when(invitationService.listAllGuestbook(any())).thenReturn(List.of());
        mockMvc.perform(get("/api/v1/invitations/my/" + ORDER + "/guestbook").principal(buyerPrincipal))
                .andExpect(status().isOk());
    }

    @Test
    void approveGuestbook_owner_returns200() throws Exception {
        UUID entryId = UUID.randomUUID();
        doNothing().when(invitationService).approveGuestbook(any(), eq(entryId));
        mockMvc.perform(put("/api/v1/invitations/my/" + ORDER + "/guestbook/" + entryId + "/approve")
                        .principal(buyerPrincipal))
                .andExpect(status().isOk());
    }

    @Test
    void guestbook_notOwner_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/invitations/my/" + ORDER + "/guestbook").principal(otherPrincipal))
                .andExpect(status().isUnauthorized());
    }
}
