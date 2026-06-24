package id.baundang.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.baundang.auth.dto.TokenResponse;
import id.baundang.auth.service.AuthService;
import id.baundang.auth.service.JwtService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = AuthController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class}
)
class AuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    AuthService authService;

    @MockBean
    JwtService jwtService;

    @Test
    void login_returnsToken() throws Exception {
        when(authService.login(any())).thenReturn(new TokenResponse("tok", "ref", 3600L));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@b.com\",\"password\":\"pass1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("tok"));
    }

    @Test
    void login_invalidBody_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_returns201() throws Exception {
        when(authService.register(any())).thenReturn(new TokenResponse("tok", "ref", 3600L));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@b.com\",\"password\":\"password1\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void publicKey_returnsTextPlain() throws Exception {
        when(jwtService.publicKeyPem()).thenReturn("-----BEGIN PUBLIC KEY-----");

        mockMvc.perform(get("/api/v1/auth/public-key"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN));
    }

    @Test
    void refresh_returns200() throws Exception {
        when(authService.refresh(any())).thenReturn(new TokenResponse("tok2", "ref2", 3600L));

        mockMvc.perform(post("/api/v1/auth/token/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refresh_token\":\"ref\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void orderToken_returns200() throws Exception {
        when(authService.issueOrderToken(any())).thenReturn("order-tok");

        mockMvc.perform(post("/api/v1/auth/order-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"access_token\":\"tok\",\"order_id\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isOk());
    }
}
