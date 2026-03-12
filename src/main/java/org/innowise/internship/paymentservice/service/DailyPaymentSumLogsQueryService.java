package org.innowise.internship.paymentservice.service;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.innowise.internship.paymentservice.model.entity.DailyPaymentSumLog;
import org.innowise.internship.paymentservice.repository.DailyPaymentSumLogsRepository;
import org.innowise.internship.paymentservice.service.exception.businessexception.InvalidArgumentException;
import org.springframework.data.domain.Page;
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
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DailyPaymentSumLogsQueryService {
    private final DailyPaymentSumLogsRepository dailyPaymentSumLogsRepository;
    private final MongoTemplate mongoTemplate;

    @Transactional(readOnly = true)
    public Optional<DailyPaymentSumLog> findByDate(@NonNull LocalDate date) {
        if (date.isAfter(LocalDate.now())) {
            throw new InvalidArgumentException("Payments for future dates do not exist");
        }

        Instant start = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        return dailyPaymentSumLogsRepository.findByDate(start);
    }

    @Transactional(readOnly = true)
    public Page<DailyPaymentSumLog> findAllByDateBetween(@NonNull LocalDate startDate,
                                                         @NonNull LocalDate endDate) {
        var now = LocalDate.now();
        if (startDate.isAfter(now) || endDate.isAfter(now)) {
            throw new InvalidArgumentException("Payments for future dates do not exist");
        }

        if (startDate.isAfter(endDate)) {
            throw new InvalidArgumentException("Invalid date range");
        }

        Instant start = startDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        return dailyPaymentSumLogsRepository.findAllByDateBetween(start, end);
    }

    @Transactional(readOnly = true)
    public BigDecimal findPaymentSumByDateRangeForAllUsers(@NonNull LocalDate startDate,
                                                           @NonNull LocalDate endDate) {
        Instant start = startDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("date").gte(start).lt(end)),
                Aggregation.group().sum("paymentSum").as("total")
        );

        AggregationResults<Document> results = mongoTemplate.aggregate(
                agg, "daily_payment_sums", Document.class
        );

        if (results.getUniqueMappedResult() == null) {
            return BigDecimal.ZERO;
        } else {
            return results.getUniqueMappedResult().get("total", BigDecimal.class);
        }
    }
}
