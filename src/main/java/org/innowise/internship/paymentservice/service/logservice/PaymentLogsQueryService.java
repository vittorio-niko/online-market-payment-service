package org.innowise.internship.paymentservice.service.logservice;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.innowise.internship.paymentservice.model.entity.log.PaymentLog;
import org.innowise.internship.paymentservice.model.entity.log.PaymentStatus;
import org.innowise.internship.paymentservice.repository.PaymentLogsRepository;
import org.innowise.internship.paymentservice.service.exception.businessexception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentLogsQueryService {
    private final PaymentLogsRepository paymentLogsRepository;
    private final MongoTemplate mongoTemplate;

    @Transactional(readOnly = true)
    public PaymentLog findByPaymentId(@NonNull String paymentId) {
        return paymentLogsRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new NotFoundException("Payment log not found"));
    }

    @Transactional(readOnly = true)
    public List<PaymentLog> getPaymentLogsByOrderId(@NonNull Long orderId) {
        return paymentLogsRepository.findByOrderId(orderId);
    }

    @Transactional(readOnly = true)
    public Boolean isOrderSuccessfullyPaid(@NonNull Long orderId) {
        return paymentLogsRepository.existsByOrderIdAndStatus(orderId, PaymentStatus.SUCCESS);
    }

    @Transactional(readOnly = true)
    public Page<PaymentLog> findByUserId(@NonNull String userId,
                                         @NonNull Pageable pageable) {
        return paymentLogsRepository.findByUserId(userId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<PaymentLog> findByUserIdAndStatus(@NonNull String userId,
                                                  @NonNull PaymentStatus status,
                                                  @NonNull Pageable pageable) {
        return paymentLogsRepository.findByUserIdAndStatus(userId, status, pageable);
    }

    @Transactional(readOnly = true)
    public Page<PaymentLog> findByUserIdAndTimestampBetween(@NonNull String userId,
                                                            @NonNull LocalDate startDate,
                                                            @NonNull LocalDate endDate,
                                                            @NonNull Pageable pageable) {
        Instant start = startDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        return paymentLogsRepository.findByUserIdAndTimestampBetween(userId, start, end, pageable);
    }

    @Transactional(readOnly = true)
    public BigDecimal findPaymentSumByUserIdAndDateRange(@NonNull String userId,
                                                         @NonNull LocalDate startDate,
                                                         @NonNull LocalDate endDate) {
        Instant start = startDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        var criteria = new Criteria().andOperator(
                Criteria.where("user_id").is(userId),
                Criteria.where("timestamp").gte(start).lt(end)
        );

        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(criteria),
                Aggregation.group().sum("payment_amount").as("total")
        );

        AggregationResults<Document> results = mongoTemplate.aggregate(
                agg, "payments", Document.class
        );

        if (results.getUniqueMappedResult() == null) {
            return BigDecimal.ZERO;
        } else {
            return results.getUniqueMappedResult().get("total", BigDecimal.class);
        }
    }

    @Transactional(readOnly = true)
    public BigDecimal findPaymentSumByUserIdAndDate(@NonNull String userId,
                                                    @NonNull LocalDate date) {
        Instant day = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant nextDay = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        var criteria = new Criteria().andOperator(
                Criteria.where("user_id").is(userId),
                Criteria.where("timestamp").gte(day).lt(nextDay)
        );

        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(criteria),
                Aggregation.group().sum("payment_amount").as("total")
        );

        AggregationResults<Document> results = mongoTemplate.aggregate(
                agg, "payments", Document.class
        );

        if (results.getUniqueMappedResult() == null) {
            return BigDecimal.ZERO;
        } else {
            return results.getUniqueMappedResult().get("total", BigDecimal.class);
        }
    }

    @Transactional(readOnly = true)
    public BigDecimal findPaymentSumByDateForAllUsers(@NonNull LocalDate date) {
        Instant day = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant nextDay = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("timestamp").gte(day).lt(nextDay)),
                Aggregation.group().sum("payment_amount").as("total")
        );

        AggregationResults<Document> results = mongoTemplate.aggregate(
                agg, "payments", Document.class
        );

        if (results.getUniqueMappedResult() == null) {
            return BigDecimal.ZERO;
        } else {
            return results.getUniqueMappedResult().get("total", BigDecimal.class);
        }
    }
}
