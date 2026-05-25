package org.innowise.internship.paymentservice.service.messageservice;

import com.mongodb.DuplicateKeyException;
import org.innowise.internship.paymentservice.model.dto.messagerequest.CreatePaymentOutboxRequestDto;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    private PaymentOutboxRepository paymentOutboxRepository;

    @Mock
    private PaymentOutboxRequestMapper paymentOutboxRequestMapper;

    @InjectMocks
    private PaymentOutboxService paymentOutboxService;

    @Captor
    private ArgumentCaptor<PaymentOutboxRequest> requestCaptor;

    @Captor
    private ArgumentCaptor<Pageable> pageableCaptor;

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
                .build();

        PaymentOutboxRequest mappedRequest = PaymentOutboxRequest.builder()
                .paymentId(paymentId)
                .userId(userId)
                .orderId(orderId)
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
                .build();

        PaymentOutboxRequest mappedRequest = PaymentOutboxRequest.builder()
                .paymentId(paymentId)
                .userId(userId)
                .orderId(orderId)
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
                .build();

        PaymentOutboxRequest mappedRequest = PaymentOutboxRequest.builder()
                .paymentId(paymentId)
                .userId(userId)
                .orderId(orderId)
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
                .build();

        PaymentOutboxRequest mappedRequest = PaymentOutboxRequest.builder()
                .paymentId(paymentId)
                .userId(userId)
                .orderId(orderId)
                .build();

        when(paymentOutboxRequestMapper.toPaymentOutboxRequest(dto)).thenReturn(mappedRequest);
        when(paymentOutboxRepository.insert(any(PaymentOutboxRequest.class)))
                .thenThrow(new RuntimeException("Database error"));

        assertThrows(RuntimeException.class, () -> paymentOutboxService.reserve(dto));

        verify(paymentOutboxRepository).insert(any(PaymentOutboxRequest.class));
    }

    @Test
    @DisplayName("getBatchOfPendingPaymentRequests should return list of PENDING requests with correct pagination")
    void getBatchOfPendingPaymentRequests_shouldReturnPendingRequestsWithCorrectPagination() {
        List<PaymentOutboxRequest> expectedRequests = List.of(
                PaymentOutboxRequest.builder().paymentId("1").status(PaymentOutboxStatus.PENDING).build(),
                PaymentOutboxRequest.builder().paymentId("2").status(PaymentOutboxStatus.PENDING).build()
        );

        when(paymentOutboxRepository.findAllByStatus(eq(PaymentOutboxStatus.PENDING), any(Pageable.class)))
                .thenReturn(expectedRequests);

        List<PaymentOutboxRequest> result = paymentOutboxService.getBatchOfPendingPaymentRequests();

        assertEquals(expectedRequests, result);
        verify(paymentOutboxRepository).findAllByStatus(
                eq(PaymentOutboxStatus.PENDING),
                pageableCaptor.capture()
        );

        Pageable capturedPageable = pageableCaptor.getValue();
        assertEquals(0, capturedPageable.getPageNumber());
        assertEquals(batchSize, capturedPageable.getPageSize());

        Sort.Order order = capturedPageable.getSort().getOrderFor("timestamp");
        assertNotNull(order);
        assertEquals(Sort.Direction.ASC, order.getDirection());
    }

    @Test
    @DisplayName("getBatchOfPendingPaymentRequests should return empty list when no PENDING requests")
    void getBatchOfPendingPaymentRequests_shouldReturnEmptyList_whenNoPendingRequests() {
        when(paymentOutboxRepository.findAllByStatus(eq(PaymentOutboxStatus.PENDING), any(Pageable.class)))
                .thenReturn(List.of());

        List<PaymentOutboxRequest> result = paymentOutboxService.getBatchOfPendingPaymentRequests();

        assertTrue(result.isEmpty());
        verify(paymentOutboxRepository).findAllByStatus(eq(PaymentOutboxStatus.PENDING), any(Pageable.class));
    }

    @Test
    @DisplayName("getBatchOfPendingPaymentRequests should respect configured batch size")
    void getBatchOfPendingPaymentRequests_shouldRespectConfiguredBatchSize() {
        ReflectionTestUtils.setField(paymentOutboxService, "batchSize", 5);

        when(paymentOutboxRepository.findAllByStatus(eq(PaymentOutboxStatus.PENDING), any(Pageable.class)))
                .thenReturn(List.of());

        paymentOutboxService.getBatchOfPendingPaymentRequests();

        verify(paymentOutboxRepository).findAllByStatus(
                eq(PaymentOutboxStatus.PENDING),
                pageableCaptor.capture()
        );

        Pageable capturedPageable = pageableCaptor.getValue();
        assertEquals(5, capturedPageable.getPageSize());
    }

    @Test
    @DisplayName("getBatchOfPendingPaymentRequests should use ascending timestamp sorting")
    void getBatchOfPendingPaymentRequests_shouldUseAscendingTimestampSorting() {
        when(paymentOutboxRepository.findAllByStatus(eq(PaymentOutboxStatus.PENDING), any(Pageable.class)))
                .thenReturn(List.of());

        paymentOutboxService.getBatchOfPendingPaymentRequests();

        verify(paymentOutboxRepository).findAllByStatus(
                eq(PaymentOutboxStatus.PENDING),
                pageableCaptor.capture()
        );

        Sort sort = pageableCaptor.getValue().getSort();
        assertTrue(sort.isSorted());
        assertEquals("timestamp", sort.iterator().next().getProperty());
        assertEquals(Sort.Direction.ASC, sort.iterator().next().getDirection());
    }

    @Test
    @DisplayName("saveMessage should save the message to repository")
    void saveMessage_shouldSaveMessageToRepository() {
        PaymentOutboxRequest message = PaymentOutboxRequest.builder()
                .paymentId(paymentId)
                .userId(userId)
                .orderId(orderId)
                .status(PaymentOutboxStatus.PENDING)
                .timestamp(Instant.now())
                .build();

        when(paymentOutboxRepository.save(message)).thenReturn(message);

        paymentOutboxService.saveMessage(message);

        verify(paymentOutboxRepository).save(message);
    }
}