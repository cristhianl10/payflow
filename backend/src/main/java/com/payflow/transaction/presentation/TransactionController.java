package com.payflow.transaction.presentation;

import java.util.UUID;
import com.payflow.transaction.application.TransactionQueries;
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

    @GetMapping("/{publicId}")
    TransactionQueries.TransactionView detail(@AuthenticationPrincipal Jwt jwt, @PathVariable String publicId) {
        return queries.detail(UUID.fromString(jwt.getSubject()), publicId);
    }
}
