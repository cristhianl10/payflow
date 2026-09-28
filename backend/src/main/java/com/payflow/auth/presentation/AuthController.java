package com.payflow.auth.presentation;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.payflow.auth.application.*;
import com.payflow.shared.domain.BusinessException;
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

    public AuthController(AuthService auth, TokenService tokens, @Value("$" + "{payflow.auth.secure-cookie:false}") boolean secure) {
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

    @PatchMapping("/users/me")
    AuthService.UserView updateProfile(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ProfileRequest request) {
        return auth.updateProfile(UUID.fromString(jwt.getSubject()), request.firstName, request.lastName);
    }

    @PostMapping("/users/me/password")
    ResponseEntity<Void> changePassword(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ChangePasswordRequest request) {
        auth.changePassword(UUID.fromString(jwt.getSubject()), request.currentPassword, request.newPassword);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/me/sessions")
    List<TokenService.SessionView> sessions(@AuthenticationPrincipal Jwt jwt) {
        UUID user = UUID.fromString(jwt.getSubject());
        return tokens.activeSessions(user, currentSession(jwt));
    }

    @DeleteMapping("/users/me/sessions/{sessionId}")
    ResponseEntity<Void> revokeSession(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID sessionId) {
        UUID user = UUID.fromString(jwt.getSubject());
        tokens.revokeOtherSession(user, currentSession(jwt), sessionId);
        return ResponseEntity.noContent().build();
    }

    private UUID currentSession(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getClaimAsString("sid"));
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new BusinessException(401, "UNAUTHENTICATED", "Please sign in again.");
        }
    }

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
    public record ProfileRequest(@NotBlank @Size(max = 100) String firstName,
            @NotBlank @Size(max = 100) String lastName) {}
    public record ChangePasswordRequest(@NotBlank @Size(max = 72) String currentPassword,
            @NotBlank @Size(min = 10, max = 72) String newPassword) {}
    public record AuthView(String accessToken, Instant expiresAt, AuthService.UserView user) {}
    public record CsrfView(String headerName, String token) {}
}
