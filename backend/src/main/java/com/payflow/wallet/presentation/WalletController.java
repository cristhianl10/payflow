package com.payflow.wallet.presentation;

import java.util.List;
import java.util.UUID;
import com.payflow.wallet.application.WalletQueries;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/wallets")
public class WalletController {
    private final WalletQueries wallets;
    public WalletController(WalletQueries wallets) { this.wallets = wallets; }
    @GetMapping
    List<WalletQueries.WalletView> all(@AuthenticationPrincipal Jwt jwt) { return List.of(wallets.mine(user(jwt))); }
    @GetMapping("/me")
    WalletQueries.WalletView mine(@AuthenticationPrincipal Jwt jwt) { return wallets.mine(user(jwt)); }
    @GetMapping({"/{publicId}", "/{publicId}/balance"})
    WalletQueries.WalletView owned(@AuthenticationPrincipal Jwt jwt, @PathVariable String publicId) {
        return wallets.owned(user(jwt), publicId);
    }
    private UUID user(Jwt jwt) { return UUID.fromString(jwt.getSubject()); }
}
