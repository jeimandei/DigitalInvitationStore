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
                                         String customerPhone, String packageName) {
        var body = new java.util.HashMap<String, Object>();
        body.put("transaction_details", Map.of(
                "order_id", midtransOrderId,
                "gross_amount", grossAmount
        ));
        body.put("customer_details", Map.of(
                "first_name", customerName,
                "email", customerEmail,
                "phone", customerPhone
        ));
        body.put("credit_card", Map.of("secure", true));

        if (packageName != null && !packageName.isBlank()) {
            body.put("item_details", java.util.List.of(Map.of(
                    "id", "PKG-" + midtransOrderId,
                    "price", grossAmount,
                    "quantity", 1,
                    "name", "Undangan Digital - Paket " + packageName
            )));
        }

        log.debug("Creating Snap transaction: orderId={} amount={}", midtransOrderId, grossAmount);

        return restClient.post()
                .uri("/snap/v1/transactions")
                .body(body)
                .retrieve()
                .body(JsonNode.class);
    }
}
