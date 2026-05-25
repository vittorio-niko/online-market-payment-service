package org.innowise.internship.paymentservice.service.messageservice;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.innowise.internship.paymentservice.bankclient.BankClient;
import org.innowise.internship.paymentservice.bankclient.BankPaymentStatus;
import org.innowise.internship.paymentservice.model.entity.inbox.PaymentInboxRequest;
import org.innowise.internship.paymentservice.model.entity.log.PaymentStatus;
import org.innowise.internship.paymentservice.service.orchestratorservice.PaymentOrchestratorService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentInboxProcessor {
    private final PaymentOrchestratorService paymentOrchestratorService;

    private final BankClient client;

    public void processInboxRecord(@NonNull PaymentInboxRequest record) {
        BankPaymentStatus status;
        try {
            status = client.processPayment(
                    record.getUserId(), record.getPaymentAmount()
            );
        } catch (Exception e) {
            paymentOrchestratorService.markAsFailed(record);
            return;
        }

        if (status == BankPaymentStatus.SUCCESS) {
            paymentOrchestratorService.finalizePayment(record, PaymentStatus.SUCCESS);
        } else {
            paymentOrchestratorService.finalizePayment(record, PaymentStatus.FAILURE);
        }
    }
}
