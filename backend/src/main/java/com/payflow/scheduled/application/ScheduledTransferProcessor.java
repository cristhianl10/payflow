package com.payflow.scheduled.application;

import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.notification.application.NotificationPublisher;
import com.payflow.notification.domain.NotificationEvent;
import com.payflow.shared.domain.BusinessException;
import com.payflow.transfer.application.TransferService;
import com.payflow.user.infrastructure.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ScheduledTransferProcessor {
    private static final Logger log = LoggerFactory.getLogger(ScheduledTransferProcessor.class);

    private final ScheduledTransferStore store;
    private final TransferService transfers;
    private final ObjectMapper json;
    private final UserRepository users;
    private final NotificationPublisher notifications;

    public ScheduledTransferProcessor(ScheduledTransferStore store, TransferService transfers, ObjectMapper json,
            UserRepository users, NotificationPublisher notifications) {
        this.store = store;
        this.transfers = transfers;
        this.json = json;
        this.users = users;
        this.notifications = notifications;
    }

    @Scheduled(fixedDelayString = "${payflow.scheduled-transfers.poll-ms:30000}")
    public void processDue() {
        int recovered = store.recoverStale();
        if (recovered > 0) log.warn("Recovered {} stale scheduled transfer jobs", recovered);

        for (UUID id : store.claimDue(20)) {
            process(id);
        }
    }

    public void process(UUID id) {
        var job = store.loadProcessing(id);
        if (job == null) return;

        try {
            String receipt = transfers.send(job.userId(), job.id(), job.recipientEmail(), job.amount(),
                    job.currency(), job.description(), job.reference());
            String operationPublicId = json.readTree(receipt).get("publicId").asText();
            store.markCompleted(id, operationPublicId);
        } catch (BusinessException exception) {
            store.markFailed(id, exception.code(), exception.getMessage());
            notifyFailure(job, exception.code(), exception.getMessage());
        } catch (Exception exception) {
            // The transfer may already have committed while the final job-status update failed.
            // Keep PROCESSING so stale recovery retries with the same idempotency key.
            log.error("Scheduled transfer {} encountered an uncertain technical failure", job.publicId(), exception);
        }
    }

    private void notifyFailure(ScheduledTransferStore.Claimed job, String code, String message) {
        users.findById(job.userId()).ifPresent(user -> notifications.publish(new NotificationEvent(
                user.id(), user.email(), "SCHEDULED_TRANSFER_FAILED", "Scheduled transfer failed",
                message, "/app/scheduled-transfers", "A scheduled PayFlow transfer failed",
                "Your scheduled transfer to " + job.recipientEmail() + " could not be completed. Reason: " + code + ".")));
    }
}
