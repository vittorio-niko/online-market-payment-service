package org.innowise.internship.paymentservice.repository;

import org.innowise.internship.paymentservice.model.entity.log.DailyPaymentSumLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface DailyPaymentSumLogsRepository
        extends MongoRepository<DailyPaymentSumLog, String> {

    Optional<DailyPaymentSumLog> findByDate(Instant start);
    Page<DailyPaymentSumLog> findAllByDateBetween(Instant start, Instant end, Pageable pageable);
}
