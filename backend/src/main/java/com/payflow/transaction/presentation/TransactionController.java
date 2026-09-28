package com.payflow.transaction.presentation;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import com.payflow.transaction.application.TransactionQueries;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {
    private final TransactionQueries queries;
    public TransactionController(TransactionQueries queries) { this.queries = queries; }

    @GetMapping
    TransactionQueries.Page history(@AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "all") String direction) {
        return queries.history(UUID.fromString(jwt.getSubject()), page, size, direction);
    }

    @GetMapping(value = "/export", produces = "text/csv")
    ResponseEntity<byte[]> export(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "all") String direction) {
        byte[] body = queries.exportCsv(UUID.fromString(jwt.getSubject()), direction)
                .getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=payflow-transactions.csv")
                .body(body);
    }

    @GetMapping("/{publicId}")
    TransactionQueries.TransactionView detail(@AuthenticationPrincipal Jwt jwt, @PathVariable String publicId) {
        return queries.detail(UUID.fromString(jwt.getSubject()), publicId);
    }
}
