package com.payflow.auth.presentation;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import com.payflow.auth.application.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class AuthController {
    private final AuthService auth;
    private final TokenService tokens;
    private final boolean secure;

    public AuthController(AuthService auth, TokenService tokens, @Value("${payflow.auth.secure-cookie:false}") boolean secure) {
        this.auth = auth; this.tokens = tokens; this.secure = secure;
    }

    @GetMapping("/auth/csrf")
    CsrfView csrf(CsrfToken csrf) { return new CsrfView(csrf.getHeaderName(), csrf.getToken()); }

    @PostMapping("/auth/register")
    ResponseEntity<AuthView> register(@Valid @RequestBody RegisterRequest request) {
        return response(auth.register(request.firstName, request.lastName, request.email, request.password), 201);
    }

    @PostMapping("/auth/login")
    ResponseEntity<AuthView> login(@Valid @RequestBody LoginRequest request) {
        return response(auth.login(request.email, request.password), 200);
    }

    @PostMapping("/auth/refresh")
    ResponseEntity<AuthView> refresh(@CookieValue(name = "payflow_refresh", required = false) String refresh) {
        return response(tokens.refresh(refresh), 200);
    }

    @PostMapping("/auth/logout")
    ResponseEntity<Void> logout(@CookieValue(name = "payflow_refresh", required = false) String refresh) {
        tokens.logout(refresh);
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString()).build();
    }

    @GetMapping("/users/me")
    AuthService.UserView me(@AuthenticationPrincipal Jwt jwt) { return auth.me(UUID.fromString(jwt.getSubject())); }

    private ResponseEntity<AuthView> response(TokenService.Tokens issued, int status) {
        return ResponseEntity.status(status).header(HttpHeaders.SET_COOKIE,
                cookie(issued.refreshToken(), Duration.between(Instant.now(), issued.sessionExpiresAt())).toString())
                .body(new AuthView(issued.accessToken(), issued.expiresAt(), auth.me(issued.userId())));
    }

    private ResponseCookie cookie(String value, Duration duration) {
        return ResponseCookie.from("payflow_refresh", value).httpOnly(true).secure(secure)
                .sameSite("Lax").path("/api/v1/auth").maxAge(duration).build();
    }

    public record RegisterRequest(@NotBlank @Size(max = 100) String firstName,
            @NotBlank @Size(max = 100) String lastName, @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 10, max = 72) String password) {}
    public record LoginRequest(@NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(max = 72) String password) {}
    public record AuthView(String accessToken, Instant expiresAt, AuthService.UserView user) {}
    public record CsrfView(String headerName, String token) {}
}
