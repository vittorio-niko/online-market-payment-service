package org.innowise.internship.paymentservice.scheduler;

import lombok.RequiredArgsConstructor;
import org.innowise.internship.paymentservice.model.entity.outbox.PaymentOutboxRequest;
import org.innowise.internship.paymentservice.model.mapper.PaymentOutboxRequestMapper;
import org.innowise.internship.paymentservice.service.messageservice.PaymentOutboxProcessor;
import org.innowise.internship.paymentservice.service.messageservice.PaymentOutboxService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PaymentOutboxScheduler {
    private final PaymentOutboxService paymentOutboxService;
    private final PaymentOutboxProcessor paymentOutboxProcessor;

    @Scheduled(fixedDelayString = "${app.kafka.outbox.publish-delay}")
    public void publishMessages() {
        List<PaymentOutboxRequest> pendingMessages = paymentOutboxService.getBatchOfPendingPaymentRequests();

        for (PaymentOutboxRequest message : pendingMessages) {
            paymentOutboxProcessor.processMessage(message);
        }
    }

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    void cleanupSentRecords() {
        paymentOutboxService.cleanupSentRecords();
    }

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    void recoverStuckRecords() {
        paymentOutboxService.recoverStuckRecords();
    }
}
