package id.baundang.order.dto;

import com.fasterxml.jackson.databind.JsonNode;
import id.baundang.order.domain.OrderIntake;

import java.util.UUID;

public record OrderIntakeDTO(
        UUID orderId,
        JsonNode answers,
        boolean submitted
) {
    public static OrderIntakeDTO from(OrderIntake i) {
        return new OrderIntakeDTO(i.getOrderId(), i.getAnswers(), i.getSubmitted());
    }
}
