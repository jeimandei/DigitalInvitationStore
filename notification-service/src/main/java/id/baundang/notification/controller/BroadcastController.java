package id.baundang.notification.controller;

import id.baundang.notification.dto.BroadcastRequest;
import id.baundang.notification.service.BroadcastService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class BroadcastController {

    private final BroadcastService broadcastService;

    @PostMapping("/broadcast")
    public ResponseEntity<Void> broadcast(@RequestBody BroadcastRequest req) {
        broadcastService.broadcast(req);
        return ResponseEntity.ok().build();
    }
}
