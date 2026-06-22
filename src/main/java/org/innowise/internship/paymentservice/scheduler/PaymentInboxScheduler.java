package org.innowise.internship.paymentservice.scheduler;

import lombok.RequiredArgsConstructor;
import org.innowise.internship.paymentservice.model.entity.inbox.PaymentInboxRequest;
import org.innowise.internship.paymentservice.service.messageservice.PaymentInboxProcessor;
import org.innowise.internship.paymentservice.service.messageservice.PaymentInboxService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PaymentInboxScheduler {
    private final PaymentInboxProcessor paymentInboxProcessor;

    private final PaymentInboxService paymentInboxService;

    @Scheduled(fixedDelayString = "${app.kafka.inbox.processing-delay}")
    void processInboxRecords() {
        List<PaymentInboxRequest> inboxPaymentRecords = paymentInboxService.getInboxRecordsBatchForProcessing();
        for (var record : inboxPaymentRecords) {
            paymentInboxProcessor.processInboxRecord(record);
        }
    }

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    void cleanupProcessedRecords() {
        paymentInboxService.cleanupProcessedRecords();
    }
}
