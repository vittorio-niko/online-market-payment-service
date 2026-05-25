package org.innowise.internship.paymentservice.repository;

import lombok.NonNull;
import org.innowise.internship.paymentservice.model.entity.inbox.PaymentInboxRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentInboxRepository
        extends MongoRepository<PaymentInboxRequest, String> {

    Optional<PaymentInboxRequest> findByMsgId(@NonNull String msgId);
}
