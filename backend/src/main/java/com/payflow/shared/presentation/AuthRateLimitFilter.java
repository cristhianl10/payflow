package com.payflow.shared.presentation;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** A bounded, single-instance limit; reverse-proxy/distributed limits are a deployment concern. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class AuthRateLimitFilter extends OncePerRequestFilter {
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final ObjectMapper json;
    private final int limit;
    public AuthRateLimitFilter(ObjectMapper json, @Value("${payflow.auth.requests-per-minute:30}") int limit) {
        this.json = json; this.limit = limit;
    }

    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getMethod().equals("POST") || !(request.getRequestURI().equals("/api/v1/auth/login")
                || request.getRequestURI().equals("/api/v1/auth/register")
                || request.getRequestURI().equals("/api/v1/auth/forgot-password")
                || request.getRequestURI().equals("/api/v1/auth/reset-password"));
    }

    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        long minute = System.currentTimeMillis() / 60000;
        if (buckets.size() >= 10000) buckets.entrySet().removeIf(entry -> entry.getValue().minute != minute);
        if (buckets.size() >= 10000 && !buckets.containsKey(request.getRemoteAddr())) {
            reject(request, response); return;
        }
        Bucket bucket = buckets.compute(request.getRemoteAddr(), (key, old) ->
                old == null || old.minute != minute ? new Bucket(minute, 1) : new Bucket(minute, old.count + 1));
        if (bucket.count > limit) { reject(request, response); return; }
        chain.doFilter(request, response);
    }

    private void reject(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json");
        response.setHeader("Retry-After", "60");
        json.writeValue(response.getOutputStream(), ApiError.create(429, "RATE_LIMITED", "Too many attempts. Try again in one minute.", request));
    }
    private record Bucket(long minute, int count) {}
}
