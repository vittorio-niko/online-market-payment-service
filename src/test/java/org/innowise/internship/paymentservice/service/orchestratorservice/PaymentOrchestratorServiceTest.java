package org.innowise.internship.paymentservice.service.orchestratorservice;

import org.innowise.internship.paymentservice.model.dto.log.request.CreatePaymentLogRequestDto;
import org.innowise.internship.paymentservice.model.dto.messagerequest.CreatePaymentOutboxRequestDto;
import org.innowise.internship.paymentservice.model.entity.inbox.PaymentInboxRequest;
import org.innowise.internship.paymentservice.model.entity.inbox.PaymentInboxStatus;
import org.innowise.internship.paymentservice.model.entity.log.PaymentLog;
import org.innowise.internship.paymentservice.model.entity.log.PaymentStatus;
import org.innowise.internship.paymentservice.model.mapper.PaymentInboxRequestMapper;
import org.innowise.internship.paymentservice.model.mapper.PaymentOutboxRequestMapper;
import org.innowise.internship.paymentservice.repository.PaymentInboxRepository;
import org.innowise.internship.paymentservice.service.logservice.PaymentLogsService;
import org.innowise.internship.paymentservice.service.messageservice.PaymentOutboxService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentOrchestratorServiceTest {

    @Mock
    private PaymentInboxRepository paymentInboxRepository;

    @Mock
    private PaymentOutboxService outboxService;

    @Mock
    private PaymentLogsService paymentLogsService;

    @Mock
    private PaymentInboxRequestMapper paymentInboxRequestMapper;

    @Mock
    private PaymentOutboxRequestMapper paymentOutboxRequestMapper;

    @InjectMocks
    private PaymentOrchestratorService paymentOrchestratorService;

    @Captor
    private ArgumentCaptor<CreatePaymentLogRequestDto> logDtoCaptor;

    private final String userId = "user-456";
    private final Long orderId = 789L;
    private final BigDecimal amount = BigDecimal.valueOf(299.99);
    private final String paymentId = "payment-123";

    @Test
    @DisplayName("Should successfully complete payment flow and update inbox status")
    void shouldCompletePaymentFlowSuccessfully() {
        PaymentInboxRequest record = createBaseRecord();
        CreatePaymentLogRequestDto logDto = createBaseLogDto();
        PaymentLog createdPaymentLog = createPaymentLog(PaymentStatus.SUCCESS);
        CreatePaymentOutboxRequestDto outboxDto = createBaseOutboxDto();

        when(paymentInboxRequestMapper.toCreatePaymentLogRequestDto(record)).thenReturn(logDto);
        when(paymentLogsService.createPaymentLog(any())).thenReturn(createdPaymentLog);
        when(paymentOutboxRequestMapper.toCreatePaymentOutboxRequestDto(createdPaymentLog)).thenReturn(outboxDto);

        paymentOrchestratorService.finalizePayment(record, PaymentStatus.SUCCESS);

        assertEquals(PaymentInboxStatus.PROCESSED, record.getStatus());
        verify(paymentInboxRepository).save(record);

        verify(paymentLogsService).createPaymentLog(logDtoCaptor.capture());
        verify(outboxService).reserve(any());

        assertEquals("SUCCESS", logDtoCaptor.getValue().getStatus());
    }

    @Test
    @DisplayName("Should execute all steps in correct order within the new transaction")
    void shouldExecuteStepsInCorrectOrder() {
        PaymentInboxRequest record = createBaseRecord();
        PaymentLog createdPaymentLog = createPaymentLog(PaymentStatus.SUCCESS);

        when(paymentInboxRequestMapper.toCreatePaymentLogRequestDto(record)).thenReturn(createBaseLogDto());
        when(paymentLogsService.createPaymentLog(any())).thenReturn(createdPaymentLog);
        when(paymentOutboxRequestMapper.toCreatePaymentOutboxRequestDto(any())).thenReturn(createBaseOutboxDto());

        paymentOrchestratorService.finalizePayment(record, PaymentStatus.SUCCESS);

        InOrder inOrder = inOrder(paymentInboxRepository, paymentLogsService, outboxService);

        inOrder.verify(paymentInboxRepository).save(record);
        inOrder.verify(paymentLogsService).createPaymentLog(any());
        inOrder.verify(outboxService).reserve(any());
    }

    @Test
    @DisplayName("Should mark record as FAILED in markAsFailed method")
    void shouldMarkAsFailedCorrectly() {
        PaymentInboxRequest record = createBaseRecord();

        paymentOrchestratorService.markAsFailed(record);

        assertEquals(PaymentInboxStatus.FAILED, record.getStatus());
        verify(paymentInboxRepository).save(record);
    }

    @Test
    @DisplayName("Should throw exception and rollback if Log Service fails")
    void shouldThrowWhenLogFails() {
        PaymentInboxRequest record = createBaseRecord();

        when(paymentInboxRequestMapper.toCreatePaymentLogRequestDto(record)).thenReturn(createBaseLogDto());
        when(paymentLogsService.createPaymentLog(any())).thenThrow(new RuntimeException("DB Error"));

        assertThrows(RuntimeException.class,
                () -> paymentOrchestratorService.finalizePayment(record, PaymentStatus.SUCCESS));

        assertEquals(PaymentInboxStatus.PROCESSED, record.getStatus());
    }

    private PaymentInboxRequest createBaseRecord() {
        PaymentInboxRequest record = new PaymentInboxRequest();
        record.setUserId(userId);
        record.setOrderId(orderId);
        record.setPaymentAmount(amount);
        record.setStatus(PaymentInboxStatus.NEW);
        return record;
    }

    private CreatePaymentLogRequestDto createBaseLogDto() {
        return CreatePaymentLogRequestDto.builder()
                .userId(userId)
                .orderId(orderId)
                .paymentAmount(amount)
                .build();
    }

    private PaymentLog createPaymentLog(PaymentStatus status) {
        return PaymentLog.builder()
                .paymentId(paymentId)
                .userId(userId)
                .orderId(orderId)
                .paymentAmount(amount)
                .status(status)
                .build();
    }

    private CreatePaymentOutboxRequestDto createBaseOutboxDto() {
        return CreatePaymentOutboxRequestDto.builder()
                .paymentId(paymentId)
                .userId(userId)
                .orderId(orderId)
                .paymentStatus(PaymentStatus.SUCCESS)
                .build();
    }
}