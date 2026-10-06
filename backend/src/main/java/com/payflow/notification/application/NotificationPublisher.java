package com.payflow.notification.application;

import com.payflow.notification.domain.NotificationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class NotificationPublisher {
    private final ApplicationEventPublisher events;

    public NotificationPublisher(ApplicationEventPublisher events) {
        this.events = events;
    }

    public void publish(NotificationEvent event) {
        events.publishEvent(event);
    }
}
