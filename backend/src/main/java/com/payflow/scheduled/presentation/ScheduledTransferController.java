package com.payflow.scheduled.presentation;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.payflow.scheduled.application.ScheduledTransferService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/scheduled-transfers")
public class ScheduledTransferController {
    private final ScheduledTransferService scheduled;

    public ScheduledTransferController(ScheduledTransferService scheduled) {
        this.scheduled = scheduled;
    }

    @GetMapping
    List<ScheduledTransferService.ScheduledTransferView> list(@AuthenticationPrincipal Jwt jwt) {
        return scheduled.list(user(jwt));
    }

    @PostMapping
    ResponseEntity<ScheduledTransferService.ScheduledTransferView> create(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateScheduledTransferRequest request) {
        return ResponseEntity.status(201).body(scheduled.create(
                user(jwt), request.recipient, request.amount, request.currency,
                request.description, request.reference, request.executeAt));
    }

    @DeleteMapping("/{publicId}")
    ResponseEntity<Void> cancel(@AuthenticationPrincipal Jwt jwt,
            @PathVariable @Size(max = 64) String publicId) {
        scheduled.cancel(user(jwt), publicId);
        return ResponseEntity.noContent().build();
    }

    private UUID user(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    public record CreateScheduledTransferRequest(
            @NotBlank @Email @Size(max = 254) String recipient,
            @NotBlank @Pattern(regexp = "(?:0|[1-9][0-9]{0,14})(?:\\.[0-9]{1,2})?")
            @DecimalMin("0.01") String amount,
            @NotBlank @Pattern(regexp = "USD") String currency,
            @Size(max = 240) String description,
            @Size(max = 80) String reference,
            @NotNull Instant executeAt) {}
}
