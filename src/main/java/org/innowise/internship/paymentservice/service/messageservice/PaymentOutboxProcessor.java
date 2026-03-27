package org.innowise.internship.paymentservice.service.messageservice;

import lombok.RequiredArgsConstructor;
import org.innowise.internship.paymentservice.model.dto.messagerequest.PaymentResultEventDto;
import org.innowise.internship.paymentservice.model.entity.outbox.PaymentOutboxRequest;
import org.innowise.internship.paymentservice.model.entity.outbox.PaymentOutboxStatus;
import org.innowise.internship.paymentservice.model.mapper.PaymentOutboxRequestMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentOutboxProcessor {
    private final PaymentOutboxService outboxService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final PaymentOutboxRequestMapper mapper;

    @Value("${app.kafka.topics.payment-results.name}")
    private String topic;

    @Value("${app.kafka.outbox.retries}")
    private Integer maxRetries;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processMessage(PaymentOutboxRequest message) {
        try {
            PaymentResultEventDto event = mapper.toPaymentResultEventDto(message);

            kafkaTemplate.send(topic, message.getPaymentId(), event).get();

            message.setStatus(PaymentOutboxStatus.SENT);
            outboxService.saveMessage(message);
        } catch (Exception e) {
            handleFailure(message);
        }
    }

    private void handleFailure(PaymentOutboxRequest message) {
        message.setAttempts(message.getAttempts() + 1);
        if (message.getAttempts() >= maxRetries) {
            message.setStatus(PaymentOutboxStatus.FAILED);
        } else {
            message.setStatus(PaymentOutboxStatus.PENDING);
        }
        outboxService.saveMessage(message);
    }
}
