package id.baundang.order.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record OrderIntakeRequest(
        JsonNode answers,
        Boolean submitted
) {}
