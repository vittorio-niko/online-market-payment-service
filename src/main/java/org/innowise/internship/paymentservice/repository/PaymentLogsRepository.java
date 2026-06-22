package org.innowise.internship.paymentservice.repository;

import org.innowise.internship.paymentservice.model.entity.log.PaymentLog;
import org.innowise.internship.paymentservice.model.entity.log.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentLogsRepository
        extends MongoRepository<PaymentLog, String> {

    Optional<PaymentLog> findByPaymentId(String paymentId);

    List<PaymentLog> findByOrderId(Long orderId);
    boolean existsByOrderIdAndStatus(Long orderId, PaymentStatus status);

    Page<PaymentLog> findByUserId(String userId, Pageable pageable);
    Page<PaymentLog> findByUserIdAndStatus(String userId,
                                           PaymentStatus status,
                                           Pageable pageable);
    Page<PaymentLog> findByUserIdAndStatusAndTimestampBetween(String userId,
                                                              PaymentStatus status,
                                                              Instant start,
                                                              Instant end,
                                                              Pageable pageable
    );
    Page<PaymentLog> findByUserIdAndTimestampBetween(String userId,
                                                     Instant start,
                                                     Instant end,
                                                     Pageable pageable);
}
