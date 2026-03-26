package org.innowise.internship.paymentservice.service.logservice;

import org.innowise.internship.paymentservice.model.dto.log.request.CreatePaymentLogRequestDto;
import org.innowise.internship.paymentservice.model.entity.log.PaymentLog;
import org.innowise.internship.paymentservice.model.entity.log.PaymentStatus;
import org.innowise.internship.paymentservice.model.mapper.PaymentLogMapper;
import org.innowise.internship.paymentservice.repository.PaymentLogsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.DisplayName;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentLogsServiceTest {

    @Mock
    private PaymentLogsRepository paymentLogsRepository;

    @Mock
    private PaymentLogMapper paymentLogMapper;

    @InjectMocks
    private PaymentLogsService paymentLogsService;

    @Captor
    private ArgumentCaptor<PaymentLog> paymentLogCaptor;

    private final String userId = "user-123";
    private final Long orderId = 456L;
    private final BigDecimal amount = BigDecimal.valueOf(99.99);
    private final PaymentStatus status = PaymentStatus.SUCCESS;

    @Test
    @DisplayName("createPaymentLog should create and save log")
    void createPaymentLog_shouldCreateAndSaveLog() {
        CreatePaymentLogRequestDto dto = CreatePaymentLogRequestDto.builder()
                .paymentId("pay-ment-id")
                .userId(userId)
                .orderId(orderId)
                .paymentAmount(amount)
                .status(status.name())
                .build();

        PaymentLog mappedLog = PaymentLog.builder()
                .paymentId("pay-ment-id")
                .userId(userId)
                .orderId(orderId)
                .paymentAmount(amount)
                .status(status)
                .build();

        PaymentLog savedLog = PaymentLog.builder()
                .paymentId("pay-ment-id")
                .userId(userId)
                .orderId(orderId)
                .paymentAmount(amount)
                .status(status)
                .timestamp(Instant.now())
                .build();

        when(paymentLogMapper.toPaymentLog(dto)).thenReturn(mappedLog);
        when(paymentLogsRepository.save(any(PaymentLog.class))).thenReturn(savedLog);

        PaymentLog result = paymentLogsService.createPaymentLog(dto);

        assertNotNull(result);
        assertEquals(savedLog, result);
        assertNotNull(result.getPaymentId());
        assertNotNull(result.getTimestamp());

        verify(paymentLogMapper).toPaymentLog(dto);
        verify(paymentLogsRepository).save(paymentLogCaptor.capture());

        PaymentLog capturedLog = paymentLogCaptor.getValue();
        assertEquals(userId, capturedLog.getUserId());
        assertEquals(orderId, capturedLog.getOrderId());
        assertEquals(amount, capturedLog.getPaymentAmount());
        assertEquals(status, capturedLog.getStatus());
        assertNotNull(capturedLog.getPaymentId());
        assertNotNull(capturedLog.getTimestamp());
    }

    @Test
    @DisplayName("createPaymentLog should preserve all data fields")
    void createPaymentLog_shouldPreserveAllDtoFields() {
        CreatePaymentLogRequestDto dto = CreatePaymentLogRequestDto.builder()
                .userId(userId)
                .orderId(orderId)
                .paymentAmount(amount)
                .status(status.name())
                .build();

        PaymentLog mappedLog = PaymentLog.builder()
                .userId(userId)
                .orderId(orderId)
                .paymentAmount(amount)
                .status(status)
                .build();

        when(paymentLogMapper.toPaymentLog(dto)).thenReturn(mappedLog);
        when(paymentLogsRepository.save(any(PaymentLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentLog result = paymentLogsService.createPaymentLog(dto);

        assertEquals(userId, result.getUserId());
        assertEquals(orderId, result.getOrderId());
        assertEquals(amount, result.getPaymentAmount());
        assertEquals(status, result.getStatus());
    }

    @Test
    @DisplayName("createPaymentLog should handle zero amount")
    void createPaymentLog_shouldHandleZeroAmount() {
        CreatePaymentLogRequestDto dto = CreatePaymentLogRequestDto.builder()
                .userId(userId)
                .orderId(orderId)
                .paymentAmount(BigDecimal.ZERO)
                .status(status.name())
                .build();

        PaymentLog mappedLog = PaymentLog.builder()
                .userId(userId)
                .orderId(orderId)
                .paymentAmount(BigDecimal.ZERO)
                .status(status)
                .build();

        when(paymentLogMapper.toPaymentLog(dto)).thenReturn(mappedLog);
        when(paymentLogsRepository.save(any(PaymentLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentLog result = paymentLogsService.createPaymentLog(dto);

        assertEquals(BigDecimal.ZERO, result.getPaymentAmount());
    }

    @Test
    @DisplayName("createPaymentLog should call repository exactly once")
    void createPaymentLog_shouldVerifyRepositorySaveCalledExactlyOnce() {
        CreatePaymentLogRequestDto dto = CreatePaymentLogRequestDto.builder()
                .userId(userId)
                .orderId(orderId)
                .paymentAmount(amount)
                .status(status.name())
                .build();

        PaymentLog mappedLog = PaymentLog.builder()
                .userId(userId)
                .orderId(orderId)
                .paymentAmount(amount)
                .status(status)
                .build();

        when(paymentLogMapper.toPaymentLog(dto)).thenReturn(mappedLog);
        when(paymentLogsRepository.save(any(PaymentLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        paymentLogsService.createPaymentLog(dto);

        verify(paymentLogsRepository, times(1)).save(any(PaymentLog.class));
    }
}
