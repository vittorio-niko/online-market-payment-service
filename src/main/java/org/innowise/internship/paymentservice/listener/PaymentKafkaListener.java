package org.innowise.internship.paymentservice.listener;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.innowise.internship.paymentservice.bankclient.BankClient;
import org.innowise.internship.paymentservice.bankclient.BankPaymentStatus;
import org.innowise.internship.paymentservice.model.dto.messagerequest.CreatePaymentInboxRequestDto;
import org.innowise.internship.paymentservice.model.entity.log.PaymentStatus;
import org.innowise.internship.paymentservice.service.messageservice.PaymentInboxService;
import org.innowise.internship.paymentservice.service.orchestratorservice.PaymentOrchestratorService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentKafkaListener {
    private final PaymentInboxService inboxService;
    private final PaymentOrchestratorService paymentOrchestratorService;

    private final BankClient client;

    @KafkaListener(
            topics = "${app.kafka.topics.payment-requests}",
            groupId = "payment-group"
    )
    public void listen(@Valid CreatePaymentInboxRequestDto dto) {
        if (!inboxService.reserve(dto)) {
            return;
        }

        // mock for external api
        BankPaymentStatus status = client.processPayment(dto.getUserId(), dto.getPaymentAmount());

        if (status == BankPaymentStatus.SUCCESS) {
            paymentOrchestratorService.finalizePayment(dto, PaymentStatus.SUCCESS);
        } else if (status == BankPaymentStatus.FAILED) {
            paymentOrchestratorService.finalizePayment(dto, PaymentStatus.FAILURE);
        }
    }
}
