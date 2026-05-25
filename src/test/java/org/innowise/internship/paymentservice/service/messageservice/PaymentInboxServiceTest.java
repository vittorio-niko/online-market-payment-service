package org.innowise.internship.paymentservice.service.messageservice;

import org.innowise.internship.paymentservice.model.dto.messagerequest.CreatePaymentInboxRequestDto;
import org.innowise.internship.paymentservice.model.entity.inbox.PaymentInboxRequest;
import org.innowise.internship.paymentservice.model.entity.inbox.PaymentInboxStatus;
import org.innowise.internship.paymentservice.model.mapper.PaymentInboxRequestMapper;
import org.innowise.internship.paymentservice.repository.PaymentInboxRepository;
import org.innowise.internship.paymentservice.service.exception.businessexception.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.Instant;
import java.util.Optional;

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

    @Test
    @DisplayName("reserve should successfully insert new request and return true")
    void reserve_shouldInsertNewRequestAndReturnTrue() {
        CreatePaymentInboxRequestDto dto = CreatePaymentInboxRequestDto.builder()
                .msgId(msgId)
                .userId(userId)
                .orderId(orderId)
                .build();

        PaymentInboxRequest mappedRequest = PaymentInboxRequest.builder()
                .msgId(msgId)
                .userId(userId)
                .orderId(orderId)
                .build();

        when(paymentInboxRequestMapper.toPaymentInboxRequest(dto)).thenReturn(mappedRequest);
        when(paymentInboxRepository.insert(any(PaymentInboxRequest.class))).thenReturn(mappedRequest);

        boolean result = paymentInboxService.reserve(dto);

        assertTrue(result);
        verify(paymentInboxRequestMapper).toPaymentInboxRequest(dto);
        verify(paymentInboxRepository).insert(requestCaptor.capture());

        PaymentInboxRequest capturedRequest = requestCaptor.getValue();
        assertEquals(msgId, capturedRequest.getMsgId());
        assertEquals(userId, capturedRequest.getUserId());
        assertEquals(orderId, capturedRequest.getOrderId());
        assertEquals(PaymentInboxStatus.PROCESSING, capturedRequest.getStatus());
        assertNotNull(capturedRequest.getTimestamp());
    }

    @Test
    @DisplayName("reserve should set current timestamp on new request")
    void reserve_shouldSetCurrentTimestamp() {
        CreatePaymentInboxRequestDto dto = CreatePaymentInboxRequestDto.builder()
                .msgId(msgId)
                .userId(userId)
                .orderId(orderId)
                .build();

        PaymentInboxRequest mappedRequest = PaymentInboxRequest.builder()
                .msgId(msgId)
                .userId(userId)
                .orderId(orderId)
                .build();

        when(paymentInboxRequestMapper.toPaymentInboxRequest(dto)).thenReturn(mappedRequest);
        when(paymentInboxRepository.insert(any(PaymentInboxRequest.class))).thenReturn(mappedRequest);

        Instant beforeTest = Instant.now();
        paymentInboxService.reserve(dto);
        Instant afterTest = Instant.now();

        verify(paymentInboxRepository).insert(requestCaptor.capture());
        PaymentInboxRequest capturedRequest = requestCaptor.getValue();

        assertNotNull(capturedRequest.getTimestamp());
        assertTrue(capturedRequest.getTimestamp().compareTo(beforeTest) >= 0);
        assertTrue(capturedRequest.getTimestamp().compareTo(afterTest) <= 0);
    }

    @Test
    @DisplayName("reserve should return false when duplicate key exception occurs and existing request is not found")
    void reserve_shouldReturnFalse_whenDuplicateKeyAndExistingIsNotProcessing() {
        String msgId = "message-123";
        CreatePaymentInboxRequestDto dto = new CreatePaymentInboxRequestDto();
        dto.setMsgId(msgId);

        PaymentInboxRequest mappedRequest = new PaymentInboxRequest();
        mappedRequest.setMsgId(msgId);

        when(paymentInboxRequestMapper.toPaymentInboxRequest(dto))
                .thenReturn(mappedRequest);

        doThrow(new DuplicateKeyException("Duplicate key error"))
                .when(paymentInboxRepository).insert(any(PaymentInboxRequest.class));

        PaymentInboxRequest existingInDb = new PaymentInboxRequest();
        existingInDb.setStatus(PaymentInboxStatus.COMPLETED);

        boolean result = paymentInboxService.reserve(dto);
        assertFalse(result, "Should return false because existing status is COMPLETED");
    }

    @Test
    @DisplayName("reserve should propagate non-duplicate exceptions")
    void reserve_shouldPropagateNonDuplicateExceptions() {
        CreatePaymentInboxRequestDto dto = CreatePaymentInboxRequestDto.builder()
                .msgId(msgId)
                .userId(userId)
                .orderId(orderId)
                .build();

        PaymentInboxRequest mappedRequest = PaymentInboxRequest.builder()
                .msgId(msgId)
                .userId(userId)
                .orderId(orderId)
                .build();

        when(paymentInboxRequestMapper.toPaymentInboxRequest(dto)).thenReturn(mappedRequest);
        when(paymentInboxRepository.insert(any(PaymentInboxRequest.class)))
                .thenThrow(new RuntimeException("Database error"));

        assertThrows(RuntimeException.class, () -> paymentInboxService.reserve(dto));

        verify(paymentInboxRepository).insert(any(PaymentInboxRequest.class));
        verify(paymentInboxRepository, never()).findByMsgId(anyString());
    }

    @Test
    @DisplayName("markMessageCompleted should update existing message status to COMPLETED")
    void markMessageCompleted_shouldUpdateExistingMessageToCompleted() {
        PaymentInboxRequest existingRequest = PaymentInboxRequest.builder()
                .msgId(msgId)
                .userId(userId)
                .orderId(orderId)
                .status(PaymentInboxStatus.PROCESSING)
                .build();

        when(paymentInboxRepository.findByMsgId(msgId)).thenReturn(Optional.of(existingRequest));
        when(paymentInboxRepository.save(any(PaymentInboxRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        paymentInboxService.markMessageCompleted(msgId);

        verify(paymentInboxRepository).findByMsgId(msgId);
        verify(paymentInboxRepository).save(requestCaptor.capture());

        PaymentInboxRequest savedRequest = requestCaptor.getValue();
        assertEquals(PaymentInboxStatus.COMPLETED, savedRequest.getStatus());
        assertEquals(msgId, savedRequest.getMsgId());
    }

    @Test
    @DisplayName("markMessageCompleted should throw NotFoundException when message not found")
    void markMessageCompleted_shouldThrowNotFoundException_whenMessageNotFound() {
        when(paymentInboxRepository.findByMsgId(msgId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> paymentInboxService.markMessageCompleted(msgId)
        );

        assertEquals("Message is not found", exception.getMessage());
        verify(paymentInboxRepository).findByMsgId(msgId);
        verify(paymentInboxRepository, never()).save(any());
    }

    @Test
    @DisplayName("markMessageCompleted should preserve all other fields when updating status")
    void markMessageCompleted_shouldPreserveAllOtherFields() {
        Instant originalTimestamp = Instant.now().minusSeconds(3600);

        PaymentInboxRequest existingRequest = PaymentInboxRequest.builder()
                .msgId(msgId)
                .userId(userId)
                .orderId(orderId)
                .status(PaymentInboxStatus.PROCESSING)
                .timestamp(originalTimestamp)
                .build();

        when(paymentInboxRepository.findByMsgId(msgId)).thenReturn(Optional.of(existingRequest));
        when(paymentInboxRepository.save(any(PaymentInboxRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        paymentInboxService.markMessageCompleted(msgId);

        verify(paymentInboxRepository).save(requestCaptor.capture());

        PaymentInboxRequest savedRequest = requestCaptor.getValue();
        assertEquals(msgId, savedRequest.getMsgId());
        assertEquals(userId, savedRequest.getUserId());
        assertEquals(orderId, savedRequest.getOrderId());
        assertEquals(originalTimestamp, savedRequest.getTimestamp());
        assertEquals(PaymentInboxStatus.COMPLETED, savedRequest.getStatus());
    }

    @Test
    @DisplayName("markMessageCompleted should work when message has COMPLETED status already")
    void markMessageCompleted_shouldWork_whenMessageAlreadyCompleted() {
        PaymentInboxRequest existingRequest = PaymentInboxRequest.builder()
                .msgId(msgId)
                .userId(userId)
                .orderId(orderId)
                .status(PaymentInboxStatus.COMPLETED)
                .build();

        when(paymentInboxRepository.findByMsgId(msgId)).thenReturn(Optional.of(existingRequest));
        when(paymentInboxRepository.save(any(PaymentInboxRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        paymentInboxService.markMessageCompleted(msgId);

        verify(paymentInboxRepository).save(requestCaptor.capture());

        PaymentInboxRequest savedRequest = requestCaptor.getValue();
        assertEquals(PaymentInboxStatus.COMPLETED, savedRequest.getStatus());
    }
}
