package id.baundang.payment.controller;

import com.fasterxml.jackson.databind.JsonNode;
import id.baundang.common.ApiResponse;
import id.baundang.payment.dto.ChargeRequest;
import id.baundang.payment.dto.ChargeResponse;
import id.baundang.payment.dto.SnapTokenResponse;
import id.baundang.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/charge")
    public ResponseEntity<ApiResponse<ChargeResponse>> charge(
            @Valid @RequestBody ChargeRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.charge(req)));
    }

    @GetMapping("/snap-token/{orderId}")
    public ApiResponse<SnapTokenResponse> getSnapToken(@PathVariable UUID orderId) {
        return ApiResponse.ok(paymentService.getSnapToken(orderId));
    }

    @PostMapping("/webhook/midtrans")
    public ResponseEntity<Void> webhook(@RequestBody JsonNode notification) {
        paymentService.handleWebhook(notification);
        return ResponseEntity.ok().build();
    }
}
