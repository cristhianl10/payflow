package com.payflow.auth.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailDeliveryService {
    private static final Logger log = LoggerFactory.getLogger(EmailDeliveryService.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String deliveryMode;
    private final String from;

    public EmailDeliveryService(ObjectProvider<JavaMailSender> mailSender,
            @Value("${payflow.email.delivery-mode:log}") String deliveryMode,
            @Value("${payflow.email.from:no-reply@payflow.local}") String from) {
        this.mailSender = mailSender;
        this.deliveryMode = deliveryMode;
        this.from = from;
    }

    public void send(String recipient, String subject, String body, String logLabel) {
        if ("log".equalsIgnoreCase(deliveryMode)) {
            log.info("PayFlow {} for {}: {}", logLabel, recipient, body);
            return;
        }
        if (!"smtp".equalsIgnoreCase(deliveryMode)) {
            throw new IllegalStateException("Unsupported payflow.email.delivery-mode");
        }

        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            throw new IllegalStateException("SMTP delivery is enabled but no JavaMailSender is configured");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText(body);
        sender.send(message);
    }
}
