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

    @KafkaListener(
            topics = "${app.kafka.topics.payment-requests}",
            groupId = "payment-group"
    )
    public void listen(@Valid CreatePaymentInboxRequestDto dto) {
        inboxService.reserve(dto);
    }
}
