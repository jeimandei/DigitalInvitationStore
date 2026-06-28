package id.baundang.invitation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.baundang.invitation.config.GatewayHeaderFilter;
import id.baundang.invitation.domain.Invitation;
import id.baundang.invitation.service.InvitationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = InvitationPageController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class}
)
@AutoConfigureMockMvc(addFilters = false)
class InvitationPageControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    InvitationService invitationService;

    @MockBean
    GatewayHeaderFilter gatewayHeaderFilter;

    @Test
    void viewInvitation_validSlug_returns2xx() throws Exception {
        Invitation inv = new Invitation();
        inv.setContent(new ObjectMapper().createObjectNode());
        inv.setStatus(Invitation.InvitationStatus.ACTIVE);
        inv.setViewCount(0L);

        when(invitationService.getBySlugAndIncrementView("test-slug")).thenReturn(inv);

        // The controller processes slug and returns view "invitation/view".
        // In WebMvcTest without templates, may return 200 (no template resolver) or 500 on template error.
        mockMvc.perform(get("/i/test-slug"))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    void viewInvitation_nullContent_handledGracefully() throws Exception {
        Invitation inv = new Invitation();
        inv.setContent(null);
        inv.setStatus(Invitation.InvitationStatus.DRAFT);
        inv.setViewCount(0L);

        when(invitationService.getBySlugAndIncrementView("draft-slug")).thenReturn(inv);

        mockMvc.perform(get("/i/draft-slug"))
                .andExpect(status().is2xxSuccessful());
    }
}
