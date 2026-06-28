package id.baundang.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.baundang.notification.client.FonnteClient;
import id.baundang.notification.dto.BroadcastRequest;
import id.baundang.notification.service.BroadcastService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = BroadcastController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class}
)
class BroadcastControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    BroadcastService broadcastService;

    @MockBean
    FonnteClient fonnteClient;

    @Test
    void broadcast_returns200() throws Exception {
        doNothing().when(broadcastService).broadcast(any());

        String body = objectMapper.writeValueAsString(new BroadcastRequest("ALL_ACTIVE", "hello"));

        mockMvc.perform(post("/api/v1/notifications/broadcast")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(broadcastService).broadcast(any());
    }

    @Test
    void broadcast_emptyBody_returns200() throws Exception {
        doNothing().when(broadcastService).broadcast(any());

        mockMvc.perform(post("/api/v1/notifications/broadcast")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }
}
