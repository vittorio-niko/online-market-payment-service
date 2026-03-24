package org.innowise.internship.paymentservice.service.messageservice;

import org.innowise.internship.paymentservice.model.dto.messagerequest.CreatePaymentInboxRequestDto;
import org.innowise.internship.paymentservice.model.entity.inbox.PaymentInboxRequest;
import org.innowise.internship.paymentservice.model.entity.inbox.PaymentInboxStatus;
import org.innowise.internship.paymentservice.model.mapper.PaymentInboxRequestMapper;
import org.innowise.internship.paymentservice.repository.PaymentInboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentInboxServiceTest {

    @Mock
    private PaymentInboxRepository paymentInboxRepository;

    @Mock
    private PaymentInboxRequestMapper paymentInboxRequestMapper;

    @InjectMocks
    private PaymentInboxService paymentInboxService;

    @Captor
    private ArgumentCaptor<PaymentInboxRequest> requestCaptor;

    private final String msgId = "message-123";
    private final String userId = "user-456";
    private final Long orderId = 789L;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentInboxService, "batchSize", 10);
    }

    @Test
    @DisplayName("reserve should successfully insert new request with NEW status")
    void reserve_shouldInsertNewRequestAndReturnTrue() {
        CreatePaymentInboxRequestDto dto = CreatePaymentInboxRequestDto.builder()
                .msgId(msgId)
                .userId(userId)
                .orderId(orderId)
                .build();

        PaymentInboxRequest mappedRequest = new PaymentInboxRequest();
        mappedRequest.setMsgId(msgId);

        when(paymentInboxRequestMapper.toPaymentInboxRequest(dto)).thenReturn(mappedRequest);

        boolean result = paymentInboxService.reserve(dto);

        assertTrue(result);
        verify(paymentInboxRepository).insert(requestCaptor.capture());

        PaymentInboxRequest saved = requestCaptor.getValue();
        assertEquals(PaymentInboxStatus.NEW, saved.getStatus());
        assertNotNull(saved.getTimestamp());
        assertEquals(msgId, saved.getMsgId());
    }

    @Test
    @DisplayName("reserve should return false when duplicate key occurs")
    void reserve_shouldReturnFalseOnDuplicate() {
        CreatePaymentInboxRequestDto dto = new CreatePaymentInboxRequestDto();
        when(paymentInboxRequestMapper.toPaymentInboxRequest(any())).thenReturn(new PaymentInboxRequest());

        doThrow(new DuplicateKeyException("Duplicate")).when(paymentInboxRepository).insert(any(PaymentInboxRequest.class));

        boolean result = paymentInboxService.reserve(dto);

        assertFalse(result);
        verify(paymentInboxRepository).insert(any(PaymentInboxRequest.class));
    }

    @Test
    @DisplayName("getInboxRecordsBatchForProcessing should return sorted list of NEW records")
    void getBatch_shouldReturnSortedRecords() {
        PaymentInboxRequest record = new PaymentInboxRequest();
        record.setStatus(PaymentInboxStatus.NEW);

        Pageable expectedPageable = PageRequest.of(0, 10, Sort.by("timestamp").ascending());

        when(paymentInboxRepository.findAllByStatus(eq(PaymentInboxStatus.NEW), any(Pageable.class)))
                .thenReturn(List.of(record));

        List<PaymentInboxRequest> result = paymentInboxService.getInboxRecordsBatchForProcessing();

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        verify(paymentInboxRepository).findAllByStatus(eq(PaymentInboxStatus.NEW), eq(expectedPageable));
    }

    @Test
    @DisplayName("reserve should propagate unexpected exceptions")
    void reserve_shouldThrowOnRandomException() {
        when(paymentInboxRequestMapper.toPaymentInboxRequest(any())).thenReturn(new PaymentInboxRequest());
        doThrow(new RuntimeException("DB Down")).when(paymentInboxRepository).insert(any(PaymentInboxRequest.class));

        assertThrows(RuntimeException.class, () -> paymentInboxService.reserve(new CreatePaymentInboxRequestDto()));
    }
}