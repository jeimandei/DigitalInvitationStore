package id.baundang.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.baundang.payment.config.GatewayHeaderFilter;
import id.baundang.payment.dto.ChargeResponse;
import id.baundang.payment.dto.SnapTokenResponse;
import id.baundang.payment.service.GiftPaymentService;
import id.baundang.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = PaymentController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class}
)
class PaymentControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    PaymentService paymentService;

    @MockBean
    GiftPaymentService giftPaymentService;

    @MockBean
    GatewayHeaderFilter gatewayHeaderFilter;

    @Test
    void charge_returns200() throws Exception {
        UUID orderId = UUID.randomUUID();
        ChargeResponse response = new ChargeResponse(UUID.randomUUID(), orderId, "snap-token", "https://pay.url");
        when(paymentService.charge(any())).thenReturn(response);

        String body = objectMapper.writeValueAsString(
                new id.baundang.payment.dto.ChargeRequest(
                        orderId, 100000L, "Budi & Sari", "budi@email.com", "+628123456789")
        );

        mockMvc.perform(post("/api/v1/payments/charge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void webhook_emptyBody_returns200() throws Exception {
        // Edge case: empty JSON object is valid as JsonNode
        doNothing().when(paymentService).handleWebhook(any());

        mockMvc.perform(post("/api/v1/payments/webhook/midtrans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void getSnapToken_returns200() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(paymentService.getSnapToken(any())).thenReturn(new SnapTokenResponse("snap-tok", "https://pay.url"));

        mockMvc.perform(get("/api/v1/payments/snap-token/" + orderId))
                .andExpect(status().isOk());
    }

    @Test
    void webhook_returns200() throws Exception {
        doNothing().when(paymentService).handleWebhook(any());

        mockMvc.perform(post("/api/v1/payments/webhook/midtrans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"transaction_status\":\"settlement\"}"))
                .andExpect(status().isOk());
    }
}
