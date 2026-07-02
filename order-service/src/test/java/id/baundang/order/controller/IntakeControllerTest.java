package id.baundang.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.baundang.order.config.GatewayHeaderFilter;
import id.baundang.order.dto.IntakeQuestionDTO;
import id.baundang.order.dto.OrderIntakeDTO;
import id.baundang.order.service.IntakeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = IntakeController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class}
)
@AutoConfigureMockMvc(addFilters = false)
class IntakeControllerTest {

    private static final UUID ORDER = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final String BUYER = "55555555-5555-5555-5555-555555555555";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    IntakeService intakeService;

    @MockBean
    GatewayHeaderFilter gatewayHeaderFilter;

    private final UsernamePasswordAuthenticationToken buyerAuth =
            new UsernamePasswordAuthenticationToken(BUYER, null,
                    List.of(new SimpleGrantedAuthority("ROLE_BUYER")));

    private IntakeQuestionDTO sampleQuestion() {
        return new IntakeQuestionDTO(UUID.randomUUID(), "Pasangan", "Nama Pasangan",
                "coupleName", "TEXT", null, (short) 1, true, 0, true);
    }

    private OrderIntakeDTO sampleIntake() {
        return new OrderIntakeDTO(ORDER, objectMapper.createObjectNode(), false);
    }

    // ── Admin: questionnaire definition ────────────────────────────────────────

    @Test
    void listQuestions_returns200() throws Exception {
        when(intakeService.listAllQuestions()).thenReturn(List.of(sampleQuestion()));
        mockMvc.perform(get("/api/v1/admin/intake/questions"))
                .andExpect(status().isOk());
    }

    @Test
    void createQuestion_validBody_returns200() throws Exception {
        when(intakeService.createQuestion(any())).thenReturn(sampleQuestion());
        String body = "{\"section\":\"Pasangan\",\"label\":\"Nama\","
                + "\"fieldKey\":\"coupleName\",\"inputType\":\"TEXT\"}";
        mockMvc.perform(post("/api/v1/admin/intake/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void createQuestion_blankLabel_returns400() throws Exception {
        String body = "{\"label\":\"\",\"fieldKey\":\"coupleName\"}";
        mockMvc.perform(post("/api/v1/admin/intake/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createQuestion_blankFieldKey_returns400() throws Exception {
        String body = "{\"label\":\"Nama\",\"fieldKey\":\"\"}";
        mockMvc.perform(post("/api/v1/admin/intake/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateQuestion_validBody_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(intakeService.updateQuestion(any(), any())).thenReturn(sampleQuestion());
        String body = "{\"label\":\"Nama\",\"fieldKey\":\"coupleName\"}";
        mockMvc.perform(put("/api/v1/admin/intake/questions/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void deleteQuestion_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(intakeService).deleteQuestion(id);
        mockMvc.perform(delete("/api/v1/admin/intake/questions/" + id))
                .andExpect(status().isOk());
    }

    // ── Per-order intake (buyer) ───────────────────────────────────────────────

    @Test
    void questionsForOrder_returns200() throws Exception {
        when(intakeService.questionsForOrder(any(), any(), anyBoolean())).thenReturn(List.of(sampleQuestion()));
        mockMvc.perform(get("/api/v1/orders/" + ORDER + "/intake/questions").principal(buyerAuth))
                .andExpect(status().isOk());
    }

    @Test
    void getIntake_returns200() throws Exception {
        when(intakeService.getIntake(any(), any(), anyBoolean())).thenReturn(sampleIntake());
        mockMvc.perform(get("/api/v1/orders/" + ORDER + "/intake").principal(buyerAuth))
                .andExpect(status().isOk());
    }

    @Test
    void saveIntake_returns200() throws Exception {
        when(intakeService.saveIntake(any(), any(), any(), anyBoolean())).thenReturn(sampleIntake());
        String body = "{\"answers\":{\"coupleName\":\"Budi & Sari\"},\"submitted\":true}";
        mockMvc.perform(put("/api/v1/orders/" + ORDER + "/intake")
                        .principal(buyerAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }
}
