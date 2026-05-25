package org.innowise.internship.paymentservice.scheduler;

import lombok.RequiredArgsConstructor;
import org.innowise.internship.paymentservice.model.dto.messagerequest.PaymentResultEventDto;
import org.innowise.internship.paymentservice.model.entity.outbox.PaymentOutboxRequest;
import org.innowise.internship.paymentservice.model.entity.outbox.PaymentOutboxStatus;
import org.innowise.internship.paymentservice.model.mapper.PaymentOutboxRequestMapper;
import org.innowise.internship.paymentservice.service.messageservice.PaymentOutboxService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PaymentOutboxScheduler {
    private final PaymentOutboxService paymentOutboxService;
    private final PaymentOutboxRequestMapper paymentOutboxRequestMapper;

    @Value("${app.kafka.topics.payment-results}")
    private String paymentResultsTopic;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.outbox.retries}")
    private Integer maxRetriesCount;

    @Scheduled(fixedDelayString = "${app.kafka.outbox.publish-delay}")
    public void publishMessages() {
        List<PaymentOutboxRequest> pendingMessages = paymentOutboxService.getBatchOfPendingPaymentRequests();

        for (PaymentOutboxRequest message : pendingMessages) {
            sendToKafka(message);
        }
    }

    private void sendToKafka(PaymentOutboxRequest message) {
        try {
            PaymentResultEventDto kafkaDto = paymentOutboxRequestMapper.toPaymentResultEventDto(message);
            kafkaTemplate.send(paymentResultsTopic, message.getPaymentId(), kafkaDto).get();

            message.setStatus(PaymentOutboxStatus.SENT);
            paymentOutboxService.saveMessage(message);
        } catch (Exception e) {
            message.setAttempts(message.getAttempts() + 1);

            if (message.getAttempts() > maxRetriesCount) {
                message.setStatus(PaymentOutboxStatus.FAILED);
            }

            paymentOutboxService.saveMessage(message);
        }
    }
}
