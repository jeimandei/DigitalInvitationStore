package id.baundang.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import id.baundang.notification.client.FonnteClient;
import id.baundang.notification.domain.Notification;
import id.baundang.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final FonnteClient fonnteClient;
    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;

    public void sendWhatsApp(String templateKey, String recipient, String message, Object payloadSource) {
        Notification notification = new Notification();
        notification.setType(templateKey);
        notification.setRecipient(recipient);
        notification.setChannel("WHATSAPP");
        notification.setTemplateKey(templateKey);
        notification.setPayload(objectMapper.valueToTree(payloadSource));

        try {
            fonnteClient.send(recipient, message);
            notification.setStatus("SENT");
            notification.setSentAt(Instant.now());
        } catch (Exception e) {
            notification.setStatus("FAILED");
            log.error("WA notification failed [{}] to {}: {}", templateKey, recipient, e.getMessage());
        } finally {
            notificationRepository.save(notification);
        }
    }

    public void sendWhatsAppRaw(String templateKey, String recipient, String message) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("recipient", recipient);
        payload.put("templateKey", templateKey);
        sendWhatsAppNode(templateKey, recipient, message, payload);
    }

    private void sendWhatsAppNode(String templateKey, String recipient, String message,
                                   com.fasterxml.jackson.databind.JsonNode payload) {
        Notification notification = new Notification();
        notification.setType(templateKey);
        notification.setRecipient(recipient);
        notification.setChannel("WHATSAPP");
        notification.setTemplateKey(templateKey);
        notification.setPayload(payload);

        try {
            fonnteClient.send(recipient, message);
            notification.setStatus("SENT");
            notification.setSentAt(Instant.now());
        } catch (Exception e) {
            notification.setStatus("FAILED");
            log.error("WA notification failed [{}] to {}: {}", templateKey, recipient, e.getMessage());
        } finally {
            notificationRepository.save(notification);
        }
    }
}
