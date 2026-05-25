package org.innowise.internship.paymentservice.service.messageservice;

import com.mongodb.DuplicateKeyException;
import org.innowise.internship.paymentservice.model.dto.messagerequest.CreatePaymentOutboxRequestDto;
import org.innowise.internship.paymentservice.model.entity.log.PaymentStatus;
import org.innowise.internship.paymentservice.model.entity.outbox.PaymentOutboxRequest;
import org.innowise.internship.paymentservice.model.entity.outbox.PaymentOutboxStatus;
import org.innowise.internship.paymentservice.model.mapper.PaymentOutboxRequestMapper;
import org.innowise.internship.paymentservice.repository.PaymentOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentOutboxServiceTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private PaymentOutboxRepository paymentOutboxRepository;

    @Mock
    private PaymentOutboxRequestMapper paymentOutboxRequestMapper;

    @InjectMocks
    private PaymentOutboxService paymentOutboxService;

    @Captor
    private ArgumentCaptor<PaymentOutboxRequest> requestCaptor;

    private final String paymentId = "outbox-payment-123";
    private final String userId = "user-456";
    private final Long orderId = 789L;
    private final Integer batchSize = 10;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentOutboxService, "batchSize", batchSize);
    }

    @Test
    @DisplayName("reserve should successfully insert new PENDING request and return true")
    void reserve_shouldInsertNewRequestAndReturnTrue() {
        CreatePaymentOutboxRequestDto dto = CreatePaymentOutboxRequestDto.builder()
                .paymentId(paymentId)
                .userId(userId)
                .orderId(orderId)
                .paymentStatus(PaymentStatus.SUCCESS)
                .build();

        PaymentOutboxRequest mappedRequest = PaymentOutboxRequest.builder()
                .paymentId(paymentId)
                .userId(userId)
                .orderId(orderId)
                .paymentStatus(PaymentStatus.SUCCESS.name())
                .build();

        when(paymentOutboxRequestMapper.toPaymentOutboxRequest(dto)).thenReturn(mappedRequest);
        when(paymentOutboxRepository.insert(any(PaymentOutboxRequest.class))).thenReturn(mappedRequest);

        boolean result = paymentOutboxService.reserve(dto);

        assertTrue(result);
        verify(paymentOutboxRequestMapper).toPaymentOutboxRequest(dto);
        verify(paymentOutboxRepository).insert(requestCaptor.capture());

        PaymentOutboxRequest capturedRequest = requestCaptor.getValue();
        assertEquals(paymentId, capturedRequest.getPaymentId());
        assertEquals(userId, capturedRequest.getUserId());
        assertEquals(orderId, capturedRequest.getOrderId());
        assertEquals(PaymentOutboxStatus.PENDING, capturedRequest.getStatus());
        assertNotNull(capturedRequest.getTimestamp());
    }

    @Test
    @DisplayName("reserve should set current timestamp on new request")
    void reserve_shouldSetCurrentTimestamp() {
        CreatePaymentOutboxRequestDto dto = CreatePaymentOutboxRequestDto.builder()
                .paymentId(paymentId)
                .userId(userId)
                .orderId(orderId)
                .paymentStatus(PaymentStatus.SUCCESS)
                .build();

        PaymentOutboxRequest mappedRequest = PaymentOutboxRequest.builder()
                .paymentId(paymentId)
                .userId(userId)
                .orderId(orderId)
                .paymentStatus(PaymentStatus.SUCCESS.name())
                .build();

        when(paymentOutboxRequestMapper.toPaymentOutboxRequest(dto)).thenReturn(mappedRequest);
        when(paymentOutboxRepository.insert(any(PaymentOutboxRequest.class))).thenReturn(mappedRequest);

        Instant beforeTest = Instant.now();
        paymentOutboxService.reserve(dto);
        Instant afterTest = Instant.now();

        verify(paymentOutboxRepository).insert(requestCaptor.capture());
        PaymentOutboxRequest capturedRequest = requestCaptor.getValue();

        assertNotNull(capturedRequest.getTimestamp());
        assertTrue(capturedRequest.getTimestamp().compareTo(beforeTest) >= 0);
        assertTrue(capturedRequest.getTimestamp().compareTo(afterTest) <= 0);
    }

    @Test
    @DisplayName("reserve should return false when duplicate key exception occurs")
    void reserve_shouldReturnFalse_whenDuplicateKeyException() {
        CreatePaymentOutboxRequestDto dto = CreatePaymentOutboxRequestDto.builder()
                .paymentId(paymentId)
                .userId(userId)
                .orderId(orderId)
                .paymentStatus(PaymentStatus.SUCCESS)
                .build();

        PaymentOutboxRequest mappedRequest = PaymentOutboxRequest.builder()
                .paymentId(paymentId)
                .userId(userId)
                .orderId(orderId)
                .paymentStatus(PaymentStatus.SUCCESS.name())
                .build();

        when(paymentOutboxRequestMapper.toPaymentOutboxRequest(dto)).thenReturn(mappedRequest);
        when(paymentOutboxRepository.insert(any(PaymentOutboxRequest.class)))
                .thenThrow(DuplicateKeyException.class);

        boolean result = paymentOutboxService.reserve(dto);

        assertFalse(result);
        verify(paymentOutboxRepository).insert(any(PaymentOutboxRequest.class));
    }

    @Test
    @DisplayName("reserve should propagate non-duplicate exceptions")
    void reserve_shouldPropagateNonDuplicateExceptions() {
        CreatePaymentOutboxRequestDto dto = CreatePaymentOutboxRequestDto.builder()
                .paymentId(paymentId)
                .userId(userId)
                .orderId(orderId)
                .paymentStatus(PaymentStatus.SUCCESS)
                .build();

        PaymentOutboxRequest mappedRequest = PaymentOutboxRequest.builder()
                .paymentId(paymentId)
                .userId(userId)
                .orderId(orderId)
                .paymentStatus(PaymentStatus.SUCCESS.name())
                .build();

        when(paymentOutboxRequestMapper.toPaymentOutboxRequest(dto)).thenReturn(mappedRequest);
        when(paymentOutboxRepository.insert(any(PaymentOutboxRequest.class)))
                .thenThrow(new RuntimeException("Database error"));

        assertThrows(RuntimeException.class, () -> paymentOutboxService.reserve(dto));

        verify(paymentOutboxRepository).insert(any(PaymentOutboxRequest.class));
    }

    @Test
    @DisplayName("getBatchOfPendingPaymentRequests should return list of PENDING requests and use correct query")
    void getBatchOfPendingPaymentRequests_shouldReturnPendingRequestsWithCorrectQuery() {
        var expected1 = PaymentOutboxRequest.builder().paymentId("1").status(PaymentOutboxStatus.PROCESSING).build();
        var expected2 = PaymentOutboxRequest.builder().paymentId("2").status(PaymentOutboxStatus.PROCESSING).build();

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);

        when(mongoTemplate.findAndModify(queryCaptor.capture(), any(Update.class),
                any(FindAndModifyOptions.class), eq(PaymentOutboxRequest.class)))
                .thenReturn(expected1)
                .thenReturn(expected2)
                .thenReturn(null);

        List<PaymentOutboxRequest> result = paymentOutboxService.getBatchOfPendingPaymentRequests();

        assertEquals(2, result.size());
        assertEquals("1", result.get(0).getPaymentId());
        assertEquals("2", result.get(1).getPaymentId());

        List<Query> capturedQueries = queryCaptor.getAllValues();
        Query firstQuery = capturedQueries.getFirst();

        assertEquals(PaymentOutboxStatus.PENDING, firstQuery.getQueryObject().get("status"));

        var sortObject = firstQuery.getSortObject();
        assertEquals(1, sortObject.get("timestamp"));
        verify(mongoTemplate, times(3)).findAndModify(any(), any(), any(), eq(PaymentOutboxRequest.class));
    }

    @Test
    @DisplayName("getBatchOfPendingPaymentRequests should return empty list when no PENDING requests")
    void getBatchOfPendingPaymentRequests_shouldReturnEmptyList_whenNoPendingRequests() {
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class),
                any(FindAndModifyOptions.class), eq(PaymentOutboxRequest.class)))
                .thenReturn(null);

        List<PaymentOutboxRequest> result = paymentOutboxService.getBatchOfPendingPaymentRequests();

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("getBatchOfPendingPaymentRequests should respect configured batch size")
    void getBatchOfPendingPaymentRequests_shouldRespectConfiguredBatchSize() {
        int customBatchSize = 5;
        ReflectionTestUtils.setField(paymentOutboxService, "batchSize", customBatchSize);

        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(PaymentOutboxRequest.class)))
                .thenReturn(new PaymentOutboxRequest());

        List<PaymentOutboxRequest> result = paymentOutboxService.getBatchOfPendingPaymentRequests();

        assertEquals(customBatchSize, result.size());

        verify(mongoTemplate, times(customBatchSize)).findAndModify(any(), any(), any(), eq(PaymentOutboxRequest.class));
    }

    @Test
    @DisplayName("getBatchOfPendingPaymentRequests should use ascending timestamp sorting")
    void getBatchOfPendingPaymentRequests_shouldUseAscendingTimestampSorting() {
        PaymentOutboxRequest record = new PaymentOutboxRequest();
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(PaymentOutboxRequest.class)))
                .thenReturn(record)
                .thenReturn(null);

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);

        paymentOutboxService.getBatchOfPendingPaymentRequests();

        verify(mongoTemplate, atLeastOnce()).findAndModify(
                queryCaptor.capture(),
                any(Update.class),
                any(FindAndModifyOptions.class),
                eq(PaymentOutboxRequest.class)
        );

        Query capturedQuery = queryCaptor.getValue();

        var sortObject = capturedQuery.getSortObject();

        assertNotNull(sortObject);
        assertTrue(sortObject.containsKey("timestamp"));
        assertEquals(1, sortObject.get("timestamp")); // asc order

        assertEquals(PaymentOutboxStatus.PENDING, capturedQuery.getQueryObject().get("status"));
    }

    @Test
    @DisplayName("saveMessage should save the message to repository")
    void saveMessage_shouldSaveMessageToRepository() {
        PaymentOutboxRequest message = PaymentOutboxRequest.builder()
                .paymentId(paymentId)
                .userId(userId)
                .orderId(orderId)
                .paymentStatus(PaymentStatus.FAILURE.name())
                .status(PaymentOutboxStatus.PENDING)
                .timestamp(Instant.now())
                .build();

        when(paymentOutboxRepository.save(message)).thenReturn(message);

        paymentOutboxService.saveMessage(message);

        verify(paymentOutboxRepository).save(message);
    }
}