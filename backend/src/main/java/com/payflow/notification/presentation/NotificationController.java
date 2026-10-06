package com.payflow.notification.presentation;

import java.util.Map;
import java.util.UUID;

import com.payflow.notification.application.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final NotificationService notifications;

    public NotificationController(NotificationService notifications) {
        this.notifications = notifications;
    }

    @GetMapping
    NotificationService.Page list(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return notifications.list(user(jwt), page, size);
    }

    @GetMapping("/unread-count")
    Map<String, Long> unreadCount(@AuthenticationPrincipal Jwt jwt) {
        return Map.of("count", notifications.unreadCount(user(jwt)));
    }

    @PostMapping("/{publicId}/read")
    ResponseEntity<Void> markRead(@AuthenticationPrincipal Jwt jwt, @PathVariable String publicId) {
        notifications.markRead(user(jwt), publicId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/read-all")
    ResponseEntity<Void> markAllRead(@AuthenticationPrincipal Jwt jwt) {
        notifications.markAllRead(user(jwt));
        return ResponseEntity.noContent().build();
    }

    private UUID user(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
