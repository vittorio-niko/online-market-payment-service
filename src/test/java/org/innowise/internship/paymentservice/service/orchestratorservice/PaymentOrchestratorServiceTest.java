package org.innowise.internship.paymentservice.service.orchestratorservice;

import org.innowise.internship.paymentservice.model.dto.log.request.CreatePaymentLogRequestDto;
import org.innowise.internship.paymentservice.model.dto.messagerequest.CreatePaymentInboxRequestDto;
import org.innowise.internship.paymentservice.model.dto.messagerequest.CreatePaymentOutboxRequestDto;
import org.innowise.internship.paymentservice.model.entity.log.PaymentLog;
import org.innowise.internship.paymentservice.model.entity.log.PaymentStatus;
import org.innowise.internship.paymentservice.model.mapper.PaymentInboxRequestMapper;
import org.innowise.internship.paymentservice.model.mapper.PaymentOutboxRequestMapper;
import org.innowise.internship.paymentservice.service.exception.businessexception.NotFoundException;
import org.innowise.internship.paymentservice.service.logservice.PaymentLogsService;
import org.innowise.internship.paymentservice.service.messageservice.PaymentInboxService;
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
    private PaymentInboxService inboxService;

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

    @Captor
    private ArgumentCaptor<CreatePaymentOutboxRequestDto> outboxDtoCaptor;

    private final String msgId = "inbox-msg-123";
    private final String userId = "user-456";
    private final Long orderId = 789L;
    private final BigDecimal amount = BigDecimal.valueOf(299.99);
    private final String paymentId = "payment-123";

    @Test
    @DisplayName("Should successfully complete payment flow with SUCCESS status")
    void shouldCompletePaymentFlowWithSuccessStatus() {
        CreatePaymentInboxRequestDto inboxDto = createBaseInboxDto();
        CreatePaymentLogRequestDto logDto = createBaseLogDto();
        PaymentLog createdPaymentLog = createPaymentLog(PaymentStatus.SUCCESS);
        CreatePaymentOutboxRequestDto outboxDto = createBaseOutboxDto();

        when(paymentInboxRequestMapper.toCreatePaymentLogRequestDto(inboxDto)).thenReturn(logDto);
        when(paymentLogsService.createPaymentLog(any())).thenReturn(createdPaymentLog);
        when(paymentOutboxRequestMapper.toCreatePaymentOutboxRequestDto(createdPaymentLog)).thenReturn(outboxDto);
        when(outboxService.reserve(any())).thenReturn(true);

        paymentOrchestratorService.finalizePayment(inboxDto, PaymentStatus.SUCCESS);

        verify(inboxService).markMessageCompleted(msgId);
        verify(paymentLogsService).createPaymentLog(logDtoCaptor.capture());
        verify(outboxService).reserve(outboxDtoCaptor.capture());

        assertEquals("SUCCESS", logDtoCaptor.getValue().getStatus());
        assertEquals(paymentId, outboxDtoCaptor.getValue().getPaymentId());
    }

    @Test
    @DisplayName("Should complete payment flow with FAILURE status")
    void shouldCompletePaymentFlowWithFailureStatus() {
        CreatePaymentInboxRequestDto inboxDto = createBaseInboxDto();
        CreatePaymentLogRequestDto logDto = createBaseLogDto();
        PaymentLog createdPaymentLog = createPaymentLog(PaymentStatus.FAILURE);
        CreatePaymentOutboxRequestDto outboxDto = createBaseOutboxDto();

        when(paymentInboxRequestMapper.toCreatePaymentLogRequestDto(inboxDto)).thenReturn(logDto);
        when(paymentLogsService.createPaymentLog(any())).thenReturn(createdPaymentLog);
        when(paymentOutboxRequestMapper.toCreatePaymentOutboxRequestDto(createdPaymentLog)).thenReturn(outboxDto);
        when(outboxService.reserve(any())).thenReturn(true);

        paymentOrchestratorService.finalizePayment(inboxDto, PaymentStatus.FAILURE);

        verify(paymentLogsService).createPaymentLog(logDtoCaptor.capture());
        assertEquals("FAILURE", logDtoCaptor.getValue().getStatus());
    }

    @Test
    @DisplayName("Should handle outbox reservation failure gracefully")
    void shouldHandleOutboxReservationFailure() {
        CreatePaymentInboxRequestDto inboxDto = createBaseInboxDto();
        CreatePaymentLogRequestDto logDto = createBaseLogDto();
        PaymentLog createdPaymentLog = createPaymentLog(PaymentStatus.SUCCESS);
        CreatePaymentOutboxRequestDto outboxDto = createBaseOutboxDto();

        when(paymentInboxRequestMapper.toCreatePaymentLogRequestDto(inboxDto)).thenReturn(logDto);
        when(paymentLogsService.createPaymentLog(any())).thenReturn(createdPaymentLog);
        when(paymentOutboxRequestMapper.toCreatePaymentOutboxRequestDto(createdPaymentLog)).thenReturn(outboxDto);
        when(outboxService.reserve(any())).thenReturn(false);

        assertDoesNotThrow(() ->
                paymentOrchestratorService.finalizePayment(inboxDto, PaymentStatus.SUCCESS)
        );

        verify(outboxService).reserve(any());
    }

    @Test
    @DisplayName("Should preserve all data through the orchestration flow")
    void shouldPreserveDataThroughFlow() {
        CreatePaymentInboxRequestDto inboxDto = createBaseInboxDto();
        CreatePaymentLogRequestDto logDto = createBaseLogDto();
        PaymentLog createdPaymentLog = createPaymentLog(PaymentStatus.SUCCESS);
        CreatePaymentOutboxRequestDto outboxDto = createBaseOutboxDto();

        when(paymentInboxRequestMapper.toCreatePaymentLogRequestDto(inboxDto)).thenReturn(logDto);
        when(paymentLogsService.createPaymentLog(any())).thenReturn(createdPaymentLog);
        when(paymentOutboxRequestMapper.toCreatePaymentOutboxRequestDto(createdPaymentLog)).thenReturn(outboxDto);
        when(outboxService.reserve(any())).thenReturn(true);

        paymentOrchestratorService.finalizePayment(inboxDto, PaymentStatus.SUCCESS);

        verify(paymentLogsService).createPaymentLog(logDtoCaptor.capture());
        verify(outboxService).reserve(outboxDtoCaptor.capture());

        CreatePaymentLogRequestDto capturedLogDto = logDtoCaptor.getValue();
        assertEquals(userId, capturedLogDto.getUserId());
        assertEquals(orderId, capturedLogDto.getOrderId());
        assertEquals(amount, capturedLogDto.getPaymentAmount());

        CreatePaymentOutboxRequestDto capturedOutboxDto = outboxDtoCaptor.getValue();
        assertEquals(paymentId, capturedOutboxDto.getPaymentId());
        assertEquals(userId, capturedOutboxDto.getUserId());
        assertEquals(orderId, capturedOutboxDto.getOrderId());
    }

    @Test
    @DisplayName("Should throw exception when inbox marking fails")
    void shouldThrowWhenInboxMarkingFails() {
        CreatePaymentInboxRequestDto inboxDto = createBaseInboxDto();

        doThrow(new NotFoundException("Message not found"))
                .when(inboxService).markMessageCompleted(msgId);

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> paymentOrchestratorService.finalizePayment(inboxDto, PaymentStatus.SUCCESS)
        );

        assertEquals("Message not found", exception.getMessage());
        verify(paymentInboxRequestMapper, never()).toCreatePaymentLogRequestDto(any());
        verify(paymentLogsService, never()).createPaymentLog(any());
        verify(outboxService, never()).reserve(any());
    }

    @Test
    @DisplayName("Should throw exception when payment log creation fails")
    void shouldThrowWhenPaymentLogCreationFails() {
        CreatePaymentInboxRequestDto inboxDto = createBaseInboxDto();
        CreatePaymentLogRequestDto logDto = createBaseLogDto();

        when(paymentInboxRequestMapper.toCreatePaymentLogRequestDto(inboxDto)).thenReturn(logDto);
        when(paymentLogsService.createPaymentLog(any()))
                .thenThrow(new RuntimeException("Database connection failed"));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> paymentOrchestratorService.finalizePayment(inboxDto, PaymentStatus.SUCCESS)
        );

        assertEquals("Database connection failed", exception.getMessage());
        verify(inboxService).markMessageCompleted(msgId);
        verify(outboxService, never()).reserve(any());
    }

    @Test
    @DisplayName("Should throw exception when outbox reservation throws exception")
    void shouldThrowWhenOutboxReservationThrows() {
        CreatePaymentInboxRequestDto inboxDto = createBaseInboxDto();
        CreatePaymentLogRequestDto logDto = createBaseLogDto();
        PaymentLog createdPaymentLog = createPaymentLog(PaymentStatus.SUCCESS);
        CreatePaymentOutboxRequestDto outboxDto = createBaseOutboxDto();

        when(paymentInboxRequestMapper.toCreatePaymentLogRequestDto(inboxDto)).thenReturn(logDto);
        when(paymentLogsService.createPaymentLog(any())).thenReturn(createdPaymentLog);
        when(paymentOutboxRequestMapper.toCreatePaymentOutboxRequestDto(createdPaymentLog)).thenReturn(outboxDto);
        when(outboxService.reserve(any())).thenThrow(new RuntimeException("Duplicate key"));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> paymentOrchestratorService.finalizePayment(inboxDto, PaymentStatus.SUCCESS)
        );

        assertEquals("Duplicate key", exception.getMessage());
        verify(inboxService).markMessageCompleted(msgId);
        verify(paymentLogsService).createPaymentLog(any());
    }

    @Test
    @DisplayName("Should handle zero payment amount correctly")
    void shouldHandleZeroAmount() {
        BigDecimal zeroAmount = BigDecimal.ZERO;

        CreatePaymentInboxRequestDto inboxDto = CreatePaymentInboxRequestDto.builder()
                .msgId(msgId)
                .userId(userId)
                .orderId(orderId)
                .paymentAmount(zeroAmount)
                .build();

        CreatePaymentLogRequestDto logDto = CreatePaymentLogRequestDto.builder()
                .userId(userId)
                .orderId(orderId)
                .paymentAmount(zeroAmount)
                .build();

        PaymentLog createdPaymentLog = PaymentLog.builder()
                .paymentId(paymentId)
                .userId(userId)
                .orderId(orderId)
                .paymentAmount(zeroAmount)
                .status(PaymentStatus.SUCCESS)
                .build();

        CreatePaymentOutboxRequestDto outboxDto = CreatePaymentOutboxRequestDto.builder()
                .paymentId(paymentId)
                .userId(userId)
                .orderId(orderId)
                .build();

        when(paymentInboxRequestMapper.toCreatePaymentLogRequestDto(inboxDto)).thenReturn(logDto);
        when(paymentLogsService.createPaymentLog(any())).thenReturn(createdPaymentLog);
        when(paymentOutboxRequestMapper.toCreatePaymentOutboxRequestDto(createdPaymentLog)).thenReturn(outboxDto);
        when(outboxService.reserve(any())).thenReturn(true);

        paymentOrchestratorService.finalizePayment(inboxDto, PaymentStatus.SUCCESS);

        verify(paymentLogsService).createPaymentLog(logDtoCaptor.capture());
        verify(outboxService).reserve(outboxDtoCaptor.capture());

        assertEquals(zeroAmount, logDtoCaptor.getValue().getPaymentAmount());
    }

    @Test
    @DisplayName("Should execute all steps in correct order")
    void shouldExecuteStepsInCorrectOrder() {
        CreatePaymentInboxRequestDto inboxDto = createBaseInboxDto();
        CreatePaymentLogRequestDto logDto = createBaseLogDto();
        PaymentLog createdPaymentLog = createPaymentLog(PaymentStatus.SUCCESS);
        CreatePaymentOutboxRequestDto outboxDto = createBaseOutboxDto();

        when(paymentInboxRequestMapper.toCreatePaymentLogRequestDto(inboxDto)).thenReturn(logDto);
        when(paymentLogsService.createPaymentLog(any())).thenReturn(createdPaymentLog);
        when(paymentOutboxRequestMapper.toCreatePaymentOutboxRequestDto(createdPaymentLog)).thenReturn(outboxDto);
        when(outboxService.reserve(any())).thenReturn(true);

        paymentOrchestratorService.finalizePayment(inboxDto, PaymentStatus.SUCCESS);

        InOrder inOrder = inOrder(
                inboxService,
                paymentInboxRequestMapper,
                paymentLogsService,
                paymentOutboxRequestMapper,
                outboxService
        );

        inOrder.verify(inboxService).markMessageCompleted(msgId);
        inOrder.verify(paymentInboxRequestMapper).toCreatePaymentLogRequestDto(inboxDto);
        inOrder.verify(paymentLogsService).createPaymentLog(any());
        inOrder.verify(paymentOutboxRequestMapper).toCreatePaymentOutboxRequestDto(createdPaymentLog);
        inOrder.verify(outboxService).reserve(any());
    }

    @Test
    @DisplayName("Should work with minimum required fields in DTO")
    void shouldWorkWithMinimumRequiredFields() {
        CreatePaymentInboxRequestDto inboxDto = CreatePaymentInboxRequestDto.builder()
                .msgId(msgId)
                .userId(userId)
                .orderId(orderId)
                .paymentAmount(amount)
                .build();

        CreatePaymentLogRequestDto logDto = CreatePaymentLogRequestDto.builder()
                .userId(userId)
                .orderId(orderId)
                .paymentAmount(amount)
                .build();

        PaymentLog createdPaymentLog = PaymentLog.builder()
                .paymentId(paymentId)
                .userId(userId)
                .orderId(orderId)
                .paymentAmount(amount)
                .status(PaymentStatus.SUCCESS)
                .build();

        CreatePaymentOutboxRequestDto outboxDto = CreatePaymentOutboxRequestDto.builder()
                .paymentId(paymentId)
                .userId(userId)
                .orderId(orderId)
                .build();

        when(paymentInboxRequestMapper.toCreatePaymentLogRequestDto(inboxDto)).thenReturn(logDto);
        when(paymentLogsService.createPaymentLog(any())).thenReturn(createdPaymentLog);
        when(paymentOutboxRequestMapper.toCreatePaymentOutboxRequestDto(createdPaymentLog)).thenReturn(outboxDto);
        when(outboxService.reserve(any())).thenReturn(true);

        assertDoesNotThrow(() ->
                paymentOrchestratorService.finalizePayment(inboxDto, PaymentStatus.SUCCESS)
        );
    }

    private CreatePaymentInboxRequestDto createBaseInboxDto() {
        return CreatePaymentInboxRequestDto.builder()
                .msgId(msgId)
                .userId(userId)
                .orderId(orderId)
                .paymentAmount(amount)
                .build();
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
                .build();
    }
}