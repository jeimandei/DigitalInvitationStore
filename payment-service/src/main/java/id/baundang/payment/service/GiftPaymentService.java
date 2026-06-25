package id.baundang.payment.service;

import com.fasterxml.jackson.databind.JsonNode;
import id.baundang.common.exception.NotFoundException;
import id.baundang.payment.client.MidtransClient;
import id.baundang.payment.domain.GiftPayment;
import id.baundang.payment.dto.GiftChargeRequest;
import id.baundang.payment.dto.GiftChargeResponse;
import id.baundang.payment.messaging.PaymentEventPublisher;
import id.baundang.payment.repository.GiftPaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GiftPaymentService {

    private static final String PREFIX = "GIFT-";

    private final GiftPaymentRepository giftPaymentRepository;
    private final MidtransClient midtransClient;
    private final PaymentEventPublisher eventPublisher;

    @Transactional
    public GiftChargeResponse charge(GiftChargeRequest req) {
        String midtransOrderId = PREFIX + UUID.randomUUID().toString().replace("-", "");

        JsonNode snap = midtransClient.createSnapTransaction(
                midtransOrderId, req.amount(),
                req.senderName(), "guest@baundang.id", "-", "Hadiah Digital"
        );

        GiftPayment gift = new GiftPayment();
        gift.setInvitationId(req.invitationId());
        gift.setSenderName(req.senderName());
        gift.setMessage(req.message());
        gift.setMidtransOrderId(midtransOrderId);
        gift.setAmount(req.amount());
        gift.setSnapToken(snap.path("token").asText(null));
        gift.setPaymentUrl(snap.path("redirect_url").asText(null));

        gift = giftPaymentRepository.save(gift);
        return new GiftChargeResponse(gift.getId(), gift.getSnapToken(), gift.getPaymentUrl());
    }

    @Transactional
    public void handleWebhook(JsonNode notification) {
        String orderId           = notification.path("order_id").asText();
        String transactionStatus = notification.path("transaction_status").asText();
        String paymentType       = notification.path("payment_type").asText();
        String fraudStatus       = notification.path("fraud_status").asText("accept");

        GiftPayment gift = giftPaymentRepository.findByMidtransOrderId(orderId)
                .orElseThrow(() -> new NotFoundException("Gift payment not found: " + orderId));

        gift.setPaymentMethod(paymentType);

        switch (transactionStatus) {
            case "capture" -> {
                if ("accept".equals(fraudStatus)) {
                    markSuccess(gift);
                } else {
                    gift.setStatus("FAILED");
                    giftPaymentRepository.save(gift);
                }
            }
            case "settlement" -> markSuccess(gift);
            case "cancel", "deny" -> {
                gift.setStatus("FAILED");
                giftPaymentRepository.save(gift);
            }
            case "expire" -> {
                gift.setStatus("EXPIRED");
                giftPaymentRepository.save(gift);
            }
            default -> log.info("Unhandled status for gift payment {}: {}", orderId, transactionStatus);
        }
    }

    private void markSuccess(GiftPayment gift) {
        gift.setStatus("SUCCESS");
        gift.setPaidAt(Instant.now());
        giftPaymentRepository.save(gift);
        eventPublisher.publishGiftPaid(gift);
    }
}
