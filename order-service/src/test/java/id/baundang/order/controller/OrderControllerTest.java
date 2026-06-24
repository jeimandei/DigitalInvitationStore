package id.baundang.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.baundang.order.config.GatewayHeaderFilter;
import id.baundang.order.dto.CreateOrderRequest;
import id.baundang.order.dto.CreateOrderResponse;
import id.baundang.order.dto.OrderDTO;
import id.baundang.order.dto.OrderRevisionDTO;
import id.baundang.order.service.OrderService;
import id.baundang.order.service.RevisionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = OrderController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class}
)
class OrderControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    OrderService orderService;

    @MockBean
    RevisionService revisionService;

    @MockBean
    GatewayHeaderFilter gatewayHeaderFilter;

    private OrderDTO sampleOrderDTO() {
        return new OrderDTO(
                UUID.randomUUID(), "ORD-001", UUID.randomUUID(), UUID.randomUUID(),
                (short) 1, "Budi & Sari", "+628123456789", "budi@email.com",
                "PENDING", null, null, (short) 0, (short) 3, "budi-sari",
                null, Instant.now(), Instant.now()
        );
    }

    @Test
    void createOrder_returns201() throws Exception {
        UUID templateId = UUID.randomUUID();
        CreateOrderResponse response = new CreateOrderResponse(UUID.randomUUID(), "ORD-001", "snap-token");
        when(orderService.createOrder(any(), any())).thenReturn(response);

        String body = objectMapper.writeValueAsString(new CreateOrderRequest(
                templateId, (short) 1, "Budi & Sari", "+628123456789", "budi@email.com", null
        ));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    void createOrder_secondRequest_also2xx() throws Exception {
        UUID templateId2 = UUID.randomUUID();
        CreateOrderResponse response2 = new CreateOrderResponse(UUID.randomUUID(), "ORD-002", "snap-token-2");
        when(orderService.createOrder(any(), any())).thenReturn(response2);

        String body2 = objectMapper.writeValueAsString(new CreateOrderRequest(
                templateId2, (short) 2, "Dedi & Rina", "+6287654321000", "dedi@email.com", "notes"
        ));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body2))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    void getOrder_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(orderService.getOrder(any(), any(), anyBoolean())).thenReturn(sampleOrderDTO());

        mockMvc.perform(get("/api/v1/orders/" + id))
                .andExpect(status().isOk());
    }

    @Test
    void listAll_returns200() throws Exception {
        when(orderService.listAllOrders(any(), any(), any())).thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isOk());
    }

    @Test
    void updateStatus_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(orderService.updateStatus(any(), any())).thenReturn(sampleOrderDTO());

        String body = "{\"status\":\"PAID\"}";
        mockMvc.perform(put("/api/v1/orders/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void requestRevision_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        OrderRevisionDTO revDTO = new OrderRevisionDTO(
                UUID.randomUUID(), id, UUID.randomUUID(),
                new ObjectMapper().createObjectNode(), "PENDING", Instant.now()
        );
        when(revisionService.requestRevision(any(), any(), any())).thenReturn(revDTO);

        String body = "{\"changes\":{\"field\":\"value\"}}";
        mockMvc.perform(post("/api/v1/orders/" + id + "/revisions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void completeRevision_returns200() throws Exception {
        UUID revId = UUID.randomUUID();
        OrderRevisionDTO revDTO = new OrderRevisionDTO(
                revId, UUID.randomUUID(), UUID.randomUUID(),
                new ObjectMapper().createObjectNode(), "COMPLETED", Instant.now()
        );
        when(revisionService.completeRevision(any())).thenReturn(revDTO);

        mockMvc.perform(put("/api/v1/orders/revisions/" + revId + "/complete"))
                .andExpect(status().isOk());
    }

    @Test
    void listRevisions_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(orderService.listRevisions(any(), any(), anyBoolean())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/orders/" + id + "/revisions"))
                .andExpect(status().isOk());
    }
}
