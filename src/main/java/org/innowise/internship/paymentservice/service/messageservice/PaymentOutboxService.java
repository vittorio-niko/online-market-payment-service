package org.innowise.internship.paymentservice.service.messageservice;

import com.mongodb.DuplicateKeyException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.innowise.internship.paymentservice.model.dto.messagerequest.CreatePaymentOutboxRequestDto;
import org.innowise.internship.paymentservice.model.entity.inbox.PaymentInboxStatus;
import org.innowise.internship.paymentservice.model.entity.outbox.PaymentOutboxRequest;
import org.innowise.internship.paymentservice.model.entity.outbox.PaymentOutboxStatus;
import org.innowise.internship.paymentservice.model.mapper.PaymentOutboxRequestMapper;
import org.innowise.internship.paymentservice.repository.PaymentOutboxRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentOutboxService {
    private final PaymentOutboxRepository paymentOutboxRepository;
    private final PaymentOutboxRequestMapper paymentOutboxRequestMapper;

    @Value("${app.kafka.outbox.batch-size}")
    private Integer batchSize;

    @Value("${app.kafka.outbox.days-to-store-records}")
    private Integer daysToKeep;

    @Value("${app.kafka.outbox.recover-stuck-delay-min}")
    private Integer recoverDelayMin;

    private final MongoTemplate mongoTemplate;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
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
        List<PaymentOutboxRequest> grabbedBatch = new ArrayList<>();

        for (int i = 0; i < batchSize; i++) {
            Query query = new Query(Criteria.where("status").is(PaymentOutboxStatus.PENDING))
                    .with(Sort.by(Sort.Direction.ASC, "timestamp"));

            Update update = new Update().set("status", PaymentOutboxStatus.PROCESSING);

            PaymentOutboxRequest record = mongoTemplate.findAndModify(
                    query,
                    update,
                    new FindAndModifyOptions().returnNew(true),
                    PaymentOutboxRequest.class
            );

            if (record != null) {
                grabbedBatch.add(record);
            } else {
                break;
            }
        }
        return grabbedBatch;
    }

    public void saveMessage(PaymentOutboxRequest message) {
        paymentOutboxRepository.save(message);
    }

    @Transactional
    public int cleanupSentRecords() {
        Instant threshold = Instant.now().minus(daysToKeep, ChronoUnit.DAYS);

        return paymentOutboxRepository.deleteByStatusAndTimestampBefore(
                PaymentOutboxStatus.SENT,
                threshold
        );
    }

    public void recoverStuckRecords() {
        Instant threshold = Instant.now().minus(recoverDelayMin, ChronoUnit.MINUTES);

        Query query = new Query(Criteria.where("status").is(PaymentOutboxStatus.PROCESSING)
                .and("timestamp").lt(threshold));

        Update update = new Update().set("status", PaymentOutboxStatus.PENDING);
        mongoTemplate.updateMulti(query, update, PaymentOutboxRequest.class);
    }
}
