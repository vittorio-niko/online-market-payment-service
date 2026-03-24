package org.innowise.internship.paymentservice.repository;

import org.innowise.internship.paymentservice.model.entity.outbox.PaymentOutboxRequest;
import org.innowise.internship.paymentservice.model.entity.outbox.PaymentOutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentOutboxRepository
        extends MongoRepository<PaymentOutboxRequest, String> {

    List<PaymentOutboxRequest> findAllByStatus(PaymentOutboxStatus status, Pageable pageable);
    Optional<PaymentOutboxRequest> findByPaymentId(String paymentId);

    int deleteByStatusAndTimestampBefore(PaymentOutboxStatus status, Instant threshold);
}
