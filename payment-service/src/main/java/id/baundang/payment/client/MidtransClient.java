package id.baundang.payment.client;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.Map;

@Slf4j
@Component
public class MidtransClient {

    private final RestClient restClient;
    private final String authHeader;

    public MidtransClient(
            @Value("${app.midtrans.server-key}") String serverKey,
            @Value("${app.midtrans.is-production}") boolean isProduction) {

        String baseUrl = isProduction
                ? "https://app.midtrans.com"
                : "https://app.sandbox.midtrans.com";

        this.authHeader = "Basic " + Base64.getEncoder()
                .encodeToString((serverKey + ":").getBytes());

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", this.authHeader)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public JsonNode createSnapTransaction(String midtransOrderId, long grossAmount,
                                         String customerName, String customerEmail,
                                         String customerPhone) {
        Map<String, Object> body = Map.of(
                "transaction_details", Map.of(
                        "order_id", midtransOrderId,
                        "gross_amount", grossAmount
                ),
                "customer_details", Map.of(
                        "first_name", customerName,
                        "email", customerEmail,
                        "phone", customerPhone
                ),
                "credit_card", Map.of("secure", true)
        );

        return restClient.post()
                .uri("/snap/v1/transactions")
                .body(body)
                .retrieve()
                .body(JsonNode.class);
    }
}
