package id.baundang.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record OrderTokenRequest(
        @NotBlank @JsonProperty("access_token") String accessToken,
        @NotNull  @JsonProperty("order_id")     UUID orderId
) {}
