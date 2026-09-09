package com.payflow.shared.presentation;

import java.time.Instant;
import jakarta.servlet.http.HttpServletRequest;

public record ApiError(
        Instant timestamp, int status, String code, String message, String path, String traceId) {
    public static ApiError create(int status, String code, String message, HttpServletRequest request) {
        return new ApiError(Instant.now(), status, code, message, request.getRequestURI(),
                (String) request.getAttribute(CorrelationIdFilter.ATTRIBUTE));
    }
}
