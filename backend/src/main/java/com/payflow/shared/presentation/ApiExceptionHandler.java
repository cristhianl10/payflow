package com.payflow.shared.presentation;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class,
            HttpMessageNotReadableException.class})
    ResponseEntity<ApiError> invalidRequest(Exception exception, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(
                ApiError.create(400, "INVALID_REQUEST", "The request contains invalid data.", request));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpectedError(Exception exception, HttpServletRequest request) {
        if (exception instanceof ErrorResponse error) {
            int status = error.getStatusCode().value();
            HttpStatus httpStatus = HttpStatus.resolve(status);
            String message = httpStatus == null ? "The request could not be processed." : httpStatus.getReasonPhrase();
            return ResponseEntity.status(status).body(ApiError.create(status, "HTTP_" + status, message, request));
        }
        LOGGER.error("Unhandled request failure; traceId={}", request.getAttribute(CorrelationIdFilter.ATTRIBUTE), exception);
        return ResponseEntity.internalServerError().body(
                ApiError.create(500, "INTERNAL_ERROR", "An unexpected error occurred.", request));
    }
}
