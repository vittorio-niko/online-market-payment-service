package org.innowise.internship.paymentservice.scheduler;

import lombok.RequiredArgsConstructor;
import org.innowise.internship.paymentservice.bankclient.BankClient;
import org.innowise.internship.paymentservice.bankclient.BankPaymentStatus;
import org.innowise.internship.paymentservice.model.entity.inbox.PaymentInboxRequest;
import org.innowise.internship.paymentservice.model.entity.inbox.PaymentInboxStatus;
import org.innowise.internship.paymentservice.model.entity.log.PaymentStatus;
import org.innowise.internship.paymentservice.service.messageservice.PaymentInboxService;
import org.innowise.internship.paymentservice.service.orchestratorservice.PaymentOrchestratorService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PaymentInboxScheduler {
    private final PaymentOrchestratorService paymentOrchestratorService;

    private final BankClient client;
    private final PaymentInboxService paymentInboxService;

    @Scheduled(fixedDelayString = "${app.kafka.inbox.processing-delay}")
    void processInboxRecords() {
        List<PaymentInboxRequest> inboxPaymentRecords = paymentInboxService.getInboxRecordsBatchForProcessing();
        for (var record : inboxPaymentRecords) {
            try {
                // mock bank client call
                BankPaymentStatus status = client.processPayment(
                        record.getUserId(), record.getPaymentAmount()
                );

                if (status == BankPaymentStatus.SUCCESS) {
                    paymentOrchestratorService.finalizePayment(record, PaymentStatus.SUCCESS);
                } else {
                    paymentOrchestratorService.finalizePayment(record, PaymentStatus.FAILURE);
                }
            } catch (Exception e) {
                paymentOrchestratorService.markAsFailed(record);
            }
        }
    }

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    void cleanupProcessedRecords() {
        paymentInboxService.cleanupProcessedRecords();
    }
}
