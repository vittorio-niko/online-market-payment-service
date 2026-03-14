package org.innowise.internship.paymentservice.service.orchestratorservice;

import lombok.RequiredArgsConstructor;
import org.innowise.internship.paymentservice.model.dto.log.request.CreatePaymentLogRequestDto;
import org.innowise.internship.paymentservice.model.dto.messagerequest.CreatePaymentInboxRequestDto;
import org.innowise.internship.paymentservice.model.entity.log.PaymentStatus;
import org.innowise.internship.paymentservice.model.mapper.PaymentInboxRequestMapper;
import org.innowise.internship.paymentservice.model.mapper.PaymentOutboxRequestMapper;
import org.innowise.internship.paymentservice.service.logservice.PaymentLogsService;
import org.innowise.internship.paymentservice.service.messageservice.PaymentInboxService;
import org.innowise.internship.paymentservice.service.messageservice.PaymentOutboxService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentOrchestratorService {
    private final PaymentInboxService inboxService;
    private final PaymentOutboxService outboxService;

    private final PaymentLogsService paymentLogsService;

    private final PaymentInboxRequestMapper paymentInboxRequestMapper;
    private final PaymentOutboxRequestMapper paymentOutboxRequestMapper;

    @Transactional
    public void finalizePayment(CreatePaymentInboxRequestDto dto, PaymentStatus status) {
        inboxService.markMessageCompleted(dto.getMsgId());

        CreatePaymentLogRequestDto logDto = paymentInboxRequestMapper.toCreatePaymentLogRequestDto(dto);
        logDto.setStatus(status.name());
        var paymentLog = paymentLogsService.createPaymentLog(logDto);

        outboxService.reserve(
                paymentOutboxRequestMapper.toCreatePaymentOutboxRequestDto(paymentLog)
        );
    }
}
