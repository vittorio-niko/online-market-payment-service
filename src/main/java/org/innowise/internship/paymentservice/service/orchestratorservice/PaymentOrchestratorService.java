package org.innowise.internship.paymentservice.service.orchestratorservice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.innowise.internship.paymentservice.model.dto.log.request.CreatePaymentLogRequestDto;
import org.innowise.internship.paymentservice.model.dto.messagerequest.CreatePaymentInboxRequestDto;
import org.innowise.internship.paymentservice.model.dto.messagerequest.PaymentResultEventDto;
import org.innowise.internship.paymentservice.model.entity.inbox.PaymentInboxRequest;
import org.innowise.internship.paymentservice.model.entity.inbox.PaymentInboxStatus;
import org.innowise.internship.paymentservice.model.entity.log.PaymentStatus;
import org.innowise.internship.paymentservice.model.entity.outbox.PaymentOutboxRequest;
import org.innowise.internship.paymentservice.model.entity.outbox.PaymentOutboxStatus;
import org.innowise.internship.paymentservice.model.mapper.PaymentInboxRequestMapper;
import org.innowise.internship.paymentservice.model.mapper.PaymentOutboxRequestMapper;
import org.innowise.internship.paymentservice.repository.PaymentInboxRepository;
import org.innowise.internship.paymentservice.service.logservice.PaymentLogsService;
import org.innowise.internship.paymentservice.service.messageservice.PaymentInboxService;
import org.innowise.internship.paymentservice.service.messageservice.PaymentOutboxService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentOrchestratorService {
    private final PaymentOutboxService outboxService;

    private final PaymentLogsService paymentLogsService;

    private final PaymentInboxRepository paymentInboxRepository;

    private final PaymentInboxRequestMapper paymentInboxRequestMapper;
    private final PaymentOutboxRequestMapper paymentOutboxRequestMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finalizePayment(PaymentInboxRequest record, PaymentStatus status) {
        record.setStatus(PaymentInboxStatus.PROCESSED);
        paymentInboxRepository.save(record);

        CreatePaymentLogRequestDto logDto = paymentInboxRequestMapper.toCreatePaymentLogRequestDto(record);
        logDto.setStatus(status.name());
        var paymentLog = paymentLogsService.createPaymentLog(logDto);
        log.info("Payment with id {} for order {} is finalized with status {}",
                paymentLog.getPaymentId(), paymentLog.getOrderId(), paymentLog.getStatus());

        outboxService.reserve(
                paymentOutboxRequestMapper.toCreatePaymentOutboxRequestDto(paymentLog)
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAsFailed(PaymentInboxRequest record) {
        record.setStatus(PaymentInboxStatus.FAILED);
        paymentInboxRepository.save(record);
    }
}
