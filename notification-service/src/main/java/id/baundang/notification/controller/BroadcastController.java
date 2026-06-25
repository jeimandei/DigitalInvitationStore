package id.baundang.notification.controller;

import id.baundang.notification.client.FonnteClient;
import id.baundang.notification.dto.BroadcastRequest;
import id.baundang.notification.service.BroadcastService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class BroadcastController {

    private final BroadcastService broadcastService;
    private final FonnteClient fonnteClient;

    @PostMapping("/broadcast")
    public ResponseEntity<Void> broadcast(@RequestBody BroadcastRequest req) {
        broadcastService.broadcast(req);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/test-wa")
    public ResponseEntity<Map<String, String>> testWa(@RequestBody Map<String, String> req) {
        String phone = req.get("phone");
        String message = req.get("message");
        if (phone == null || phone.isBlank() || message == null || message.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "phone dan message wajib diisi"));
        }
        fonnteClient.send(phone.trim(), message.trim());
        return ResponseEntity.ok(Map.of("status", "sent", "phone", phone.trim()));
    }
}

