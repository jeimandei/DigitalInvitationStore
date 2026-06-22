package id.baundang.payment.service;

import com.fasterxml.jackson.databind.JsonNode;
import id.baundang.common.exception.NotFoundException;
import id.baundang.common.exception.ValidationException;
import id.baundang.payment.client.MidtransClient;
import id.baundang.payment.domain.Payment;
import id.baundang.payment.dto.ChargeRequest;
import id.baundang.payment.dto.ChargeResponse;
import id.baundang.payment.dto.SnapTokenResponse;
import id.baundang.payment.messaging.PaymentEventPublisher;
import id.baundang.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final MidtransClient midtransClient;
    private final PaymentEventPublisher eventPublisher;
    private final SignatureValidator signatureValidator;

    @Transactional
    public ChargeResponse charge(ChargeRequest req) {
        if (paymentRepository.findByOrderId(req.orderId()).isPresent()) {
            throw new ValidationException("Payment already exists for order: " + req.orderId());
        }

        String midtransOrderId = "BND-" + req.orderId();

        JsonNode snapResponse = midtransClient.createSnapTransaction(
                midtransOrderId, req.amount(),
                req.coupleName(), req.contactEmail(), req.contactWhatsapp()
        );

        Payment payment = new Payment();
        payment.setOrderId(req.orderId());
        payment.setMidtransOrderId(midtransOrderId);
        payment.setAmount(req.amount());
        payment.setSnapToken(snapResponse.path("token").asText(null));
        payment.setPaymentUrl(snapResponse.path("redirect_url").asText(null));
        payment.setStatus("PENDING");

        payment = paymentRepository.save(payment);

        return new ChargeResponse(payment.getId(), payment.getOrderId(),
                payment.getSnapToken(), payment.getPaymentUrl());
    }

    @Transactional(readOnly = true)
    public SnapTokenResponse getSnapToken(UUID orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NotFoundException("Payment not found for order: " + orderId));
        return new SnapTokenResponse(payment.getSnapToken(), payment.getPaymentUrl());
    }

    @Transactional
    public void handleWebhook(JsonNode notification) {
        String orderId        = notification.path("order_id").asText();
        String statusCode     = notification.path("status_code").asText();
        String grossAmount    = notification.path("gross_amount").asText();
        String signatureKey   = notification.path("signature_key").asText();
        String transactionStatus = notification.path("transaction_status").asText();
        String paymentType    = notification.path("payment_type").asText();
        String fraudStatus    = notification.path("fraud_status").asText("accept");

        if (!signatureValidator.isValid(orderId, statusCode, grossAmount, signatureKey)) {
            throw new ValidationException("Invalid Midtrans signature");
        }

        Payment payment = paymentRepository.findByMidtransOrderId(orderId)
                .orElseThrow(() -> new NotFoundException("Payment not found for midtrans order: " + orderId));

        payment.setRawNotification(notification);
        payment.setPaymentMethod(paymentType);

        switch (transactionStatus) {
            case "capture" -> {
                if ("accept".equals(fraudStatus)) {
                    markSuccess(payment);
                } else {
                    markFailed(payment, "Fraud detected: " + fraudStatus);
                }
            }
            case "settlement" -> markSuccess(payment);
            case "cancel", "deny" -> markFailed(payment, "Transaction " + transactionStatus);
            case "expire" -> {
                payment.setStatus("EXPIRED");
                paymentRepository.save(payment);
                eventPublisher.publishPaymentFailed(payment, "expired");
            }
            default -> log.info("Unhandled Midtrans transaction_status: {}", transactionStatus);
        }
    }

    private void markSuccess(Payment payment) {
        payment.setStatus("SUCCESS");
        payment.setPaidAt(Instant.now());
        paymentRepository.save(payment);
        eventPublisher.publishOrderPaid(payment);
    }

    private void markFailed(Payment payment, String reason) {
        payment.setStatus("FAILED");
        paymentRepository.save(payment);
        eventPublisher.publishPaymentFailed(payment, reason);
    }
}
