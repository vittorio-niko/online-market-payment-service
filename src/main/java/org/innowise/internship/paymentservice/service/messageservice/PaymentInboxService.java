package org.innowise.internship.paymentservice.service.messageservice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.innowise.internship.paymentservice.model.dto.messagerequest.CreatePaymentInboxRequestDto;
import org.innowise.internship.paymentservice.model.entity.inbox.PaymentInboxRequest;
import org.innowise.internship.paymentservice.model.entity.inbox.PaymentInboxStatus;
import org.innowise.internship.paymentservice.model.mapper.PaymentInboxRequestMapper;
import org.innowise.internship.paymentservice.repository.PaymentInboxRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentInboxService {
    private final PaymentInboxRepository paymentInboxRepository;
    private final PaymentInboxRequestMapper paymentInboxRequestMapper;

    @Value("${app.kafka.inbox.batch-size}")
    private Integer batchSize;

    @Value("${app.kafka.inbox.days-to-store-records}")
    private Integer daysToKeep;

    @Transactional
    public boolean reserve(@NonNull CreatePaymentInboxRequestDto dto) {
        try {
            PaymentInboxRequest request = paymentInboxRequestMapper.toPaymentInboxRequest(dto);
            request.setTimestamp(Instant.now());
            request.setStatus(PaymentInboxStatus.NEW);
            paymentInboxRepository.insert(request);
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    @Transactional
    public List<PaymentInboxRequest> getInboxRecordsBatchForProcessing() {
        Pageable batch = PageRequest.of(0, batchSize, Sort.by("timestamp").ascending());
        return paymentInboxRepository.findAllByStatus(PaymentInboxStatus.NEW, batch);
    }

    @Transactional
    public int cleanupProcessedRecords() {
        Instant threshold = Instant.now().minus(daysToKeep, ChronoUnit.DAYS);

        return paymentInboxRepository.deleteByStatusAndTimestampBefore(
                PaymentInboxStatus.PROCESSED,
                threshold
        );
    }
}