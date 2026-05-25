package org.innowise.internship.paymentservice.repository;

import lombok.NonNull;
import org.innowise.internship.paymentservice.model.entity.inbox.PaymentInboxRequest;
import org.innowise.internship.paymentservice.model.entity.inbox.PaymentInboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentInboxRepository
        extends MongoRepository<PaymentInboxRequest, String> {

    Optional<PaymentInboxRequest> findByPaymentId(@NonNull String msgId);
    List<PaymentInboxRequest> findAllByPaymentId(@NonNull String msgId);

    int deleteByStatusAndTimestampBefore(PaymentInboxStatus status, Instant threshold);
}
