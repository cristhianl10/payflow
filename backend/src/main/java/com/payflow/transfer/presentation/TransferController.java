package com.payflow.transfer.presentation;

import java.util.UUID;
import com.payflow.transfer.application.TransferService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {
    private final TransferService transfers;
    public TransferController(TransferService transfers) { this.transfers = transfers; }

    @GetMapping("/recipient")
    TransferService.Recipient recipient(@AuthenticationPrincipal Jwt jwt, @RequestParam @Size(max = 254) @Email String email) {
        return transfers.recipient(UUID.fromString(jwt.getSubject()), email);
    }

    @PostMapping
    ResponseEntity<String> send(@AuthenticationPrincipal Jwt jwt, @RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody TransferRequest request) {
        return ResponseEntity.status(201).contentType(MediaType.APPLICATION_JSON).body(transfers.send(
                UUID.fromString(jwt.getSubject()), key, request.recipient, request.amount, request.currency, request.description));
    }

    public record TransferRequest(@NotBlank @Email @Size(max = 254) String recipient,
            @NotBlank @Pattern(regexp = "(?:0|[1-9][0-9]{0,14})(?:\\.[0-9]{1,2})?")
            @DecimalMin("0.01") @DecimalMax("999999999999999.99") String amount,
            @NotBlank @Pattern(regexp = "USD") String currency, @Size(max = 240) String description) {}
}
