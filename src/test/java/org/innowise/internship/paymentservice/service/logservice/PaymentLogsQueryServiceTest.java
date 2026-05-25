package org.innowise.internship.paymentservice.service.logservice;

import org.bson.Document;
import org.innowise.internship.paymentservice.model.entity.log.PaymentLog;
import org.innowise.internship.paymentservice.model.entity.log.PaymentStatus;
import org.innowise.internship.paymentservice.repository.PaymentLogsRepository;
import org.innowise.internship.paymentservice.service.exception.businessexception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentLogsQueryServiceTest {

    @Mock
    private PaymentLogsRepository paymentLogsRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private PaymentLogsQueryService paymentLogsQueryService;

    private final String paymentId = "payment-123";
    private final String userId = "user-456";
    private final Long orderId = 789L;
    private final LocalDate now = LocalDate.now();
    private final LocalDate startDate = now.minusDays(10);
    private final LocalDate endDate = now.minusDays(5);
    private final Instant startInstant = startDate.atStartOfDay(ZoneOffset.UTC).toInstant();
    private final Instant endInstant = endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    private final Pageable pageable = PageRequest.of(0, 10);

    @Test
    void findByPaymentId_shouldReturnLog_whenExists() {
        PaymentLog expectedLog = PaymentLog.builder()
                .paymentId(paymentId)
                .build();

        when(paymentLogsRepository.findByPaymentId(paymentId))
                .thenReturn(Optional.of(expectedLog));

        PaymentLog result = paymentLogsQueryService.findByPaymentId(paymentId);

        assertEquals(expectedLog, result);
        verify(paymentLogsRepository).findByPaymentId(paymentId);
    }

    @Test
    void findByPaymentId_shouldReturnEmpty_whenNotExists() {
        when(paymentLogsRepository.findByPaymentId(paymentId))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> paymentLogsQueryService.findByPaymentId(paymentId));

        verify(paymentLogsRepository).findByPaymentId(paymentId);
    }

    @Test
    void getPaymentLogsByOrderId_shouldReturnListOfLogs() {
        List<PaymentLog> expectedLogs = List.of(
                PaymentLog.builder().orderId(orderId).build(),
                PaymentLog.builder().orderId(orderId).build()
        );

        when(paymentLogsRepository.findByOrderId(orderId))
                .thenReturn(expectedLogs);

        List<PaymentLog> result = paymentLogsQueryService.getPaymentLogsByOrderId(orderId);

        assertEquals(2, result.size());
        assertEquals(expectedLogs, result);
        verify(paymentLogsRepository).findByOrderId(orderId);
    }

    @Test
    void getPaymentLogsByOrderId_shouldReturnEmptyList_whenNoLogs() {
        when(paymentLogsRepository.findByOrderId(orderId))
                .thenReturn(List.of());

        List<PaymentLog> result = paymentLogsQueryService.getPaymentLogsByOrderId(orderId);

        assertTrue(result.isEmpty());
        verify(paymentLogsRepository).findByOrderId(orderId);
    }


    @Test
    void isOrderSuccessfullyPaid_shouldReturnTrue_whenExists() {
        when(paymentLogsRepository.existsByOrderIdAndStatus(orderId, PaymentStatus.SUCCESS))
                .thenReturn(true);

        Boolean result = paymentLogsQueryService.isOrderSuccessfullyPaid(orderId);

        assertTrue(result);
        verify(paymentLogsRepository).existsByOrderIdAndStatus(orderId, PaymentStatus.SUCCESS);
    }

    @Test
    void isOrderSuccessfullyPaid_shouldReturnFalse_whenNotExists() {
        when(paymentLogsRepository.existsByOrderIdAndStatus(orderId, PaymentStatus.SUCCESS))
                .thenReturn(false);

        Boolean result = paymentLogsQueryService.isOrderSuccessfullyPaid(orderId);

        assertFalse(result);
        verify(paymentLogsRepository).existsByOrderIdAndStatus(orderId, PaymentStatus.SUCCESS);
    }

    @Test
    void findByUserId_shouldReturnPageOfLogs() {
        List<PaymentLog> logs = List.of(
                PaymentLog.builder().userId(userId).build(),
                PaymentLog.builder().userId(userId).build()
        );
        Page<PaymentLog> expectedPage = new PageImpl<>(logs);

        when(paymentLogsRepository.findByUserId(userId, pageable))
                .thenReturn(expectedPage);

        Page<PaymentLog> result = paymentLogsQueryService.findByUserId(userId, pageable);

        assertEquals(2, result.getTotalElements());
        assertEquals(logs, result.getContent());
        verify(paymentLogsRepository).findByUserId(userId, pageable);
    }

    @Test
    void findByUserIdAndStatus_shouldReturnPageOfLogs() {
        List<PaymentLog> logs = List.of(
                PaymentLog.builder().userId(userId).status(PaymentStatus.SUCCESS).build()
        );
        Page<PaymentLog> expectedPage = new PageImpl<>(logs);

        when(paymentLogsRepository.findByUserIdAndStatus(userId, PaymentStatus.SUCCESS, pageable))
                .thenReturn(expectedPage);

        Page<PaymentLog> result = paymentLogsQueryService.findByUserIdAndStatus(
                userId, PaymentStatus.SUCCESS, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(logs, result.getContent());
        verify(paymentLogsRepository).findByUserIdAndStatus(userId, PaymentStatus.SUCCESS, pageable);
    }

    @Test
    void findByUserIdAndTimestampBetween_shouldReturnPageOfLogs() {
        List<PaymentLog> logs = List.of(
                PaymentLog.builder().userId(userId).timestamp(startInstant.plusSeconds(3600)).build()
        );
        Page<PaymentLog> expectedPage = new PageImpl<>(logs);

        when(paymentLogsRepository.findByUserIdAndTimestampBetween(
                userId, startInstant, endInstant, pageable))
                .thenReturn(expectedPage);

        Page<PaymentLog> result = paymentLogsQueryService.findByUserIdAndTimestampBetween(
                userId, startDate, endDate, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(logs, result.getContent());
        verify(paymentLogsRepository).findByUserIdAndTimestampBetween(
                userId, startInstant, endInstant, pageable);
    }

    @Test
    void findPaymentSumByUserIdAndDateRange_shouldReturnSum_whenResultsExist() {
        Document resultDoc = new Document("total", BigDecimal.valueOf(3500.25));
        AggregationResults<Document> aggregationResults = mock(AggregationResults.class);

        when(aggregationResults.getUniqueMappedResult()).thenReturn(resultDoc);
        when(mongoTemplate.aggregate(any(Aggregation.class), eq("payments"), eq(Document.class)))
                .thenReturn(aggregationResults);

        BigDecimal result = paymentLogsQueryService.findPaymentSumByUserIdAndDateRangeAndStatusSuccess(
                userId, startDate, endDate);

        assertEquals(BigDecimal.valueOf(3500.25), result);
        verify(mongoTemplate).aggregate(any(Aggregation.class), eq("payments"), eq(Document.class));
    }

    @Test
    void findPaymentSumByUserIdAndDateRange_shouldReturnZero_whenNoResults() {
        AggregationResults<Document> aggregationResults = mock(AggregationResults.class);

        when(aggregationResults.getUniqueMappedResult()).thenReturn(null);
        when(mongoTemplate.aggregate(any(Aggregation.class), eq("payments"), eq(Document.class)))
                .thenReturn(aggregationResults);

        BigDecimal result = paymentLogsQueryService.findPaymentSumByUserIdAndDateRangeAndStatusSuccess(
                userId, startDate, endDate);

        assertEquals(BigDecimal.ZERO, result);
        verify(mongoTemplate).aggregate(any(Aggregation.class), eq("payments"), eq(Document.class));
    }

    private final LocalDate date = now.minusDays(3);

    @Test
    void findPaymentSumByUserIdAndDate_shouldReturnSum_whenResultsExist() {
        Document resultDoc = new Document("total", BigDecimal.valueOf(1200.50));
        AggregationResults<Document> aggregationResults = mock(AggregationResults.class);

        when(aggregationResults.getUniqueMappedResult()).thenReturn(resultDoc);
        when(mongoTemplate.aggregate(any(Aggregation.class), eq("payments"), eq(Document.class)))
                .thenReturn(aggregationResults);

        BigDecimal result = paymentLogsQueryService.findPaymentSumByUserIdAndDateAndStatusSuccess(userId, date);

        assertEquals(BigDecimal.valueOf(1200.50), result);
        verify(mongoTemplate).aggregate(any(Aggregation.class), eq("payments"), eq(Document.class));
    }

    @Test
    void findPaymentSumByUserIdAndDate_shouldReturnZero_whenNoResults() {
        AggregationResults<Document> aggregationResults = mock(AggregationResults.class);

        when(aggregationResults.getUniqueMappedResult()).thenReturn(null);
        when(mongoTemplate.aggregate(any(Aggregation.class), eq("payments"), eq(Document.class)))
                .thenReturn(aggregationResults);

        BigDecimal result = paymentLogsQueryService.findPaymentSumByUserIdAndDateAndStatusSuccess(userId, date);

        assertEquals(BigDecimal.ZERO, result);
        verify(mongoTemplate).aggregate(any(Aggregation.class), eq("payments"), eq(Document.class));
    }

    @Test
    void findPaymentSumByDateForAllUsers_shouldReturnSum_whenResultsExist() {
        Document resultDoc = new Document("total", BigDecimal.valueOf(5000.00));
        AggregationResults<Document> aggregationResults = mock(AggregationResults.class);

        when(aggregationResults.getUniqueMappedResult()).thenReturn(resultDoc);
        when(mongoTemplate.aggregate(any(Aggregation.class), eq("payments"), eq(Document.class)))
                .thenReturn(aggregationResults);

        BigDecimal result = paymentLogsQueryService.findPaymentSumByDateAndStatusSuccessForAllUsers(date);

        assertEquals(BigDecimal.valueOf(5000.00), result);
        verify(mongoTemplate).aggregate(any(Aggregation.class), eq("payments"), eq(Document.class));
    }

    @Test
    void findPaymentSumByDateForAllUsers_shouldReturnZero_whenNoResults() {
        AggregationResults<Document> aggregationResults = mock(AggregationResults.class);

        when(aggregationResults.getUniqueMappedResult()).thenReturn(null);
        when(mongoTemplate.aggregate(any(Aggregation.class), eq("payments"), eq(Document.class)))
                .thenReturn(aggregationResults);

        BigDecimal result = paymentLogsQueryService.findPaymentSumByDateAndStatusSuccessForAllUsers(date);

        assertEquals(BigDecimal.ZERO, result);
        verify(mongoTemplate).aggregate(any(Aggregation.class), eq("payments"), eq(Document.class));
    }
}