package org.innowise.internship.paymentservice.service.messageservice;

import org.springframework.dao.DuplicateKeyException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.innowise.internship.paymentservice.model.dto.messagerequest.CreatePaymentInboxRequestDto;
import org.innowise.internship.paymentservice.model.entity.inbox.PaymentInboxRequest;
import org.innowise.internship.paymentservice.model.entity.inbox.PaymentInboxStatus;
import org.innowise.internship.paymentservice.model.mapper.PaymentInboxRequestMapper;
import org.innowise.internship.paymentservice.repository.PaymentInboxRepository;
import org.innowise.internship.paymentservice.service.exception.businessexception.NotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PaymentInboxService {
    private final PaymentInboxRepository paymentInboxRepository;
    private final PaymentInboxRequestMapper paymentInboxRequestMapper;

    public boolean reserve(@NonNull CreatePaymentInboxRequestDto dto) {
        try {
            PaymentInboxRequest request = paymentInboxRequestMapper.toPaymentInboxRequest(dto);
            request.setTimestamp(Instant.now());
            request.setStatus(PaymentInboxStatus.PROCESSING);
            paymentInboxRepository.insert(request);
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    public void markMessageCompleted(@NonNull String msgId) {
        PaymentInboxRequest request = paymentInboxRepository.findByMsgId(msgId)
                .orElseThrow(() -> new NotFoundException("Message is not found"));
        request.setStatus(PaymentInboxStatus.COMPLETED);
        paymentInboxRepository.save(request);
    }
}
