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
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentInboxService {
    private final PaymentInboxRepository paymentInboxRepository;
    private final PaymentInboxRequestMapper paymentInboxRequestMapper;

    private final MongoTemplate mongoTemplate;

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

    public List<PaymentInboxRequest> getInboxRecordsBatchForProcessing() {
        List<PaymentInboxRequest> grabbedRecords = new ArrayList<>();

        for (int i = 0; i < batchSize; i++) {
            Query query = new Query(Criteria.where("status").is(PaymentInboxStatus.NEW))
                    .with(Sort.by(Sort.Direction.ASC, "timestamp"));

            Update update = new Update().set("status", PaymentInboxStatus.PROCESSING);

            PaymentInboxRequest record = mongoTemplate.findAndModify(
                    query,
                    update,
                    new FindAndModifyOptions().returnNew(true),
                    PaymentInboxRequest.class
            );

            if (record != null) {
                grabbedRecords.add(record);
            } else {
                break;
            }
        }
        return grabbedRecords;
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