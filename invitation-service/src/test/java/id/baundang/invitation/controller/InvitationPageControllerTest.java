package id.baundang.invitation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import id.baundang.invitation.config.GatewayHeaderFilter;
import id.baundang.invitation.domain.Invitation;
import id.baundang.invitation.service.InvitationService;
import id.baundang.invitation.service.PinGateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = InvitationPageController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class}
)
@AutoConfigureMockMvc(addFilters = false)
class InvitationPageControllerTest {

    private static final String PIN = "246813";
    private static final String SECRET_STORY = "Kami bertemu di Bandung";

    @Autowired
    MockMvc mockMvc;

    @MockBean
    InvitationService invitationService;

    @MockBean
    PinGateService pinGateService;

    @MockBean
    GatewayHeaderFilter gatewayHeaderFilter;

    private Invitation invitation(ObjectNode content) {
        Invitation inv = new Invitation();
        inv.setContent(content);
        inv.setStatus(Invitation.InvitationStatus.ACTIVE);
        inv.setViewCount(0L);
        return inv;
    }

    private ObjectNode protectedContent() {
        ObjectNode content = new ObjectMapper().createObjectNode();
        content.put("accessPin", PIN);
        content.put("coupleName", "Budi & Sari");
        content.put("loveStory", SECRET_STORY);
        return content;
    }

    @Test
    void viewInvitation_validSlug_returns2xx() throws Exception {
        Invitation inv = invitation(new ObjectMapper().createObjectNode());
        when(invitationService.getBySlug("test-slug")).thenReturn(inv);
        when(invitationService.getBySlugAndIncrementView("test-slug")).thenReturn(inv);

        mockMvc.perform(get("/i/test-slug"))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    void viewInvitation_nullContent_handledGracefully() throws Exception {
        Invitation inv = invitation(null);
        inv.setStatus(Invitation.InvitationStatus.DRAFT);
        when(invitationService.getBySlug("draft-slug")).thenReturn(inv);
        when(invitationService.getBySlugAndIncrementView("draft-slug")).thenReturn(inv);

        mockMvc.perform(get("/i/draft-slug"))
                .andExpect(status().is2xxSuccessful());
    }

    // ── PIN gate ──────────────────────────────────────────────────────────────

    @Test
    void viewInvitation_protectedWithoutPass_leaksNeitherPinNorContent() throws Exception {
        when(invitationService.getBySlug("secret-slug")).thenReturn(invitation(protectedContent()));
        when(pinGateService.isProtected(PIN)).thenReturn(true);
        when(pinGateService.hasValidPass(any(), anyString())).thenReturn(false);

        mockMvc.perform(get("/i/secret-slug"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString(PIN))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString(SECRET_STORY))));
    }

    @Test
    void viewInvitation_protectedWithoutPass_doesNotCountAsAView() throws Exception {
        when(invitationService.getBySlug("secret-slug")).thenReturn(invitation(protectedContent()));
        when(pinGateService.isProtected(PIN)).thenReturn(true);
        when(pinGateService.hasValidPass(any(), anyString())).thenReturn(false);

        mockMvc.perform(get("/i/secret-slug")).andExpect(status().isOk());

        org.mockito.Mockito.verify(invitationService, org.mockito.Mockito.never())
                .getBySlugAndIncrementView("secret-slug");
    }

    @Test
    void viewInvitation_protectedWithValidPass_rendersInvitation() throws Exception {
        Invitation inv = invitation(protectedContent());
        when(invitationService.getBySlug("secret-slug")).thenReturn(inv);
        when(invitationService.getBySlugAndIncrementView("secret-slug")).thenReturn(inv);
        when(pinGateService.isProtected(PIN)).thenReturn(true);
        when(pinGateService.hasValidPass(any(), anyString())).thenReturn(true);

        mockMvc.perform(get("/i/secret-slug"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(SECRET_STORY)))
                // Even on the real page the PIN itself is never rendered.
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString(PIN))));
    }

    @Test
    void giftPage_protectedWithoutPass_isGated() throws Exception {
        when(invitationService.getBySlug("secret-slug")).thenReturn(invitation(protectedContent()));
        when(pinGateService.isProtected(PIN)).thenReturn(true);
        when(pinGateService.hasValidPass(any(), anyString())).thenReturn(false);

        mockMvc.perform(get("/i/secret-slug/gift"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString(PIN))));
    }

    @Test
    void submitPin_correct_issuesPassAndRedirects() throws Exception {
        when(invitationService.getBySlug("secret-slug")).thenReturn(invitation(protectedContent()));
        when(pinGateService.matches(PIN, PIN)).thenReturn(true);
        when(pinGateService.cookieName("secret-slug")).thenReturn("inv_pin_abc");
        when(pinGateService.issue("secret-slug")).thenReturn("pass-value");
        when(pinGateService.cookieMaxAgeSeconds()).thenReturn(3600);

        mockMvc.perform(post("/i/secret-slug/pin").param("pin", PIN))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/i/secret-slug"))
                .andExpect(cookie().value("inv_pin_abc", "pass-value"))
                .andExpect(cookie().httpOnly("inv_pin_abc", true));
    }

    @Test
    void submitPin_correct_preservesGuestGreeting() throws Exception {
        when(invitationService.getBySlug("secret-slug")).thenReturn(invitation(protectedContent()));
        when(pinGateService.matches(PIN, PIN)).thenReturn(true);
        when(pinGateService.cookieName("secret-slug")).thenReturn("inv_pin_abc");
        when(pinGateService.issue("secret-slug")).thenReturn("pass-value");

        mockMvc.perform(post("/i/secret-slug/pin").param("pin", PIN).param("to", "Budi Santoso"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/i/secret-slug?to=Budi+Santoso"));
    }

    @Test
    void submitPin_wrong_reRendersGateWithoutPassOrContent() throws Exception {
        when(invitationService.getBySlug("secret-slug")).thenReturn(invitation(protectedContent()));
        when(pinGateService.matches(PIN, "000000")).thenReturn(false);

        mockMvc.perform(post("/i/secret-slug/pin").param("pin", "000000"))
                .andExpect(status().isOk())
                .andExpect(cookie().doesNotExist("inv_pin_abc"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString(PIN))))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString(SECRET_STORY))));
    }
}
