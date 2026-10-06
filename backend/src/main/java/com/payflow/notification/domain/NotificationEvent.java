package com.payflow.notification.domain;

import java.util.UUID;

public record NotificationEvent(
        UUID userId,
        String email,
        String type,
        String title,
        String message,
        String actionUrl,
        String emailSubject,
        String emailBody) {}
