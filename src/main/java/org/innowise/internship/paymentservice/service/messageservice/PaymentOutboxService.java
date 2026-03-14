package org.innowise.internship.paymentservice.service.messageservice;

import com.mongodb.DuplicateKeyException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.innowise.internship.paymentservice.model.dto.messagerequest.CreatePaymentOutboxRequestDto;
import org.innowise.internship.paymentservice.model.entity.outbox.PaymentOutboxRequest;
import org.innowise.internship.paymentservice.model.entity.outbox.PaymentOutboxStatus;
import org.innowise.internship.paymentservice.model.mapper.PaymentOutboxRequestMapper;
import org.innowise.internship.paymentservice.repository.PaymentOutboxRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentOutboxService {
    private final PaymentOutboxRepository paymentOutboxRepository;
    private final PaymentOutboxRequestMapper paymentOutboxRequestMapper;

    @Value("${app.kafka.outbox.batch-size}")
    private Integer batchSize;

    public boolean reserve(@NonNull CreatePaymentOutboxRequestDto dto) {
        try {
            PaymentOutboxRequest request = paymentOutboxRequestMapper.toPaymentOutboxRequest(dto);
            request.setTimestamp(Instant.now());
            request.setStatus(PaymentOutboxStatus.PENDING);
            paymentOutboxRepository.insert(request);
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    public List<PaymentOutboxRequest> getBatchOfPendingPaymentRequests() {
        Pageable limit = PageRequest.of(0, batchSize,
                Sort.by("timestamp").ascending());

        return paymentOutboxRepository.findAllByStatus(PaymentOutboxStatus.PENDING, limit);
    }

    public void saveMessage(PaymentOutboxRequest message) {
        paymentOutboxRepository.save(message);
    }
}
