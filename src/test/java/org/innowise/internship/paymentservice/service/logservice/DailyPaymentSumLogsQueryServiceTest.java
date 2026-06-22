package org.innowise.internship.paymentservice.service.logservice;

import org.bson.Document;
import org.innowise.internship.paymentservice.model.entity.log.DailyPaymentSumLog;
import org.innowise.internship.paymentservice.repository.DailyPaymentSumLogsRepository;
import org.innowise.internship.paymentservice.service.exception.businessexception.InvalidArgumentException;
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
class DailyPaymentSumLogsQueryServiceTest {

    @Mock
    private DailyPaymentSumLogsRepository dailyPaymentSumLogsRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private DailyPaymentSumLogsQueryService dailyPaymentSumLogsQueryService;

    private final LocalDate now = LocalDate.now();
    private final LocalDate pastDate = now.minusDays(5);
    private final Instant pastDateInstant = pastDate.atStartOfDay(ZoneOffset.UTC).toInstant();

    private final Pageable pageable = PageRequest.of(0, 10);

    @Test
    void findByDate_shouldReturnLog_whenDateExists() {
        DailyPaymentSumLog expectedLog = DailyPaymentSumLog.builder()
                .date(pastDateInstant)
                .paymentSum(BigDecimal.valueOf(1000))
                .build();

        when(dailyPaymentSumLogsRepository.findByDate(pastDateInstant))
                .thenReturn(Optional.of(expectedLog));

        DailyPaymentSumLog result = dailyPaymentSumLogsQueryService.findByDate(pastDate);

        assertEquals(expectedLog, result);
        assertEquals(pastDateInstant, result.getDate());
        assertEquals(BigDecimal.valueOf(1000), result.getPaymentSum());
        verify(dailyPaymentSumLogsRepository).findByDate(pastDateInstant);
    }

    @Test
    void findByDate_shouldReturnEmptyOptional_whenDateDoesNotExist() {
        when(dailyPaymentSumLogsRepository.findByDate(pastDateInstant))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> dailyPaymentSumLogsQueryService.findByDate(pastDate));

        verify(dailyPaymentSumLogsRepository).findByDate(pastDateInstant);
    }

    @Test
    void findByDate_shouldThrowException_whenDateIsInFuture() {
        LocalDate futureDate = now.plusDays(1);

        InvalidArgumentException exception = assertThrows(
                InvalidArgumentException.class,
                () -> dailyPaymentSumLogsQueryService.findByDate(futureDate)
        );

        assertEquals("Payments for future dates do not exist", exception.getMessage());
        verify(dailyPaymentSumLogsRepository, never()).findByDate(any());
    }

    private final LocalDate startDate = now.minusDays(10);
    private final LocalDate endDate = now.minusDays(5);
    private final Instant startInstant = startDate.atStartOfDay(ZoneOffset.UTC).toInstant();
    private final Instant endInstant = endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

    @Test
    void findAllByDateBetween_shouldReturnPageOfLogs() {
        List<DailyPaymentSumLog> logs = List.of(
                DailyPaymentSumLog.builder().date(startInstant).paymentSum(BigDecimal.valueOf(500)).build(),
                DailyPaymentSumLog.builder().date(endInstant.minusSeconds(10000)).paymentSum(BigDecimal.valueOf(700)).build()
        );
        Page<DailyPaymentSumLog> expectedPage = new PageImpl<>(logs);

        when(dailyPaymentSumLogsRepository.findAllByDateBetween(startInstant, endInstant, pageable))
                .thenReturn(expectedPage);

        Page<DailyPaymentSumLog> result =
                dailyPaymentSumLogsQueryService.findAllByDateBetween(startDate, endDate, pageable);

        assertEquals(2, result.getTotalElements());
        assertEquals(logs, result.getContent());
        verify(dailyPaymentSumLogsRepository).findAllByDateBetween(startInstant, endInstant, pageable);
    }

    @Test
    void findAllByDateBetween_shouldThrowException_whenStartDateIsInFuture() {
        LocalDate futureDate = now.plusDays(1);

        InvalidArgumentException exception = assertThrows(
                InvalidArgumentException.class,
                () -> dailyPaymentSumLogsQueryService.findAllByDateBetween(futureDate, endDate, pageable)
        );

        assertEquals("Payments for future dates do not exist", exception.getMessage());
        verify(dailyPaymentSumLogsRepository, never()).findAllByDateBetween(any(), any(), any());
    }

    @Test
    void findAllByDateBetween_shouldThrowException_whenEndDateIsInFuture() {
        LocalDate futureDate = now.plusDays(1);

        InvalidArgumentException exception = assertThrows(
                InvalidArgumentException.class,
                () -> dailyPaymentSumLogsQueryService.findAllByDateBetween(startDate, futureDate, pageable)
        );

        assertEquals("Payments for future dates do not exist", exception.getMessage());
        verify(dailyPaymentSumLogsRepository, never()).findAllByDateBetween(any(), any(), any());
    }

    @Test
    void findAllByDateBetween_shouldThrowException_whenStartDateIsAfterEndDate() {
        InvalidArgumentException exception = assertThrows(
                InvalidArgumentException.class,
                () -> dailyPaymentSumLogsQueryService.findAllByDateBetween(endDate, startDate, pageable)
        );

        assertEquals("Invalid date range", exception.getMessage());
        verify(dailyPaymentSumLogsRepository, never()).findAllByDateBetween(any(), any(), any());
    }

    @Test
    void findPaymentSumByDateRangeForAllUsers_shouldReturnSum_whenResultsExist() {
        Document resultDoc = new Document("total", BigDecimal.valueOf(2730.50));
        AggregationResults<Document> aggregationResults = mock(AggregationResults.class);

        when(aggregationResults.getUniqueMappedResult()).thenReturn(resultDoc);
        when(mongoTemplate.aggregate(any(Aggregation.class), eq("daily_payment_sums"), eq(Document.class)))
                .thenReturn(aggregationResults);

        BigDecimal result = dailyPaymentSumLogsQueryService.findPaymentSumByDateRangeForAllUsers(startDate, endDate);

        assertEquals(BigDecimal.valueOf(2730.50), result);
        verify(mongoTemplate).aggregate(any(Aggregation.class), eq("daily_payment_sums"), eq(Document.class));
    }

    @Test
    void findPaymentSumByDateRangeForAllUsers_shouldReturnZero_whenNoResults() {
        AggregationResults<Document> aggregationResults = mock(AggregationResults.class);

        when(aggregationResults.getUniqueMappedResult()).thenReturn(null);
        when(mongoTemplate.aggregate(any(Aggregation.class), eq("daily_payment_sums"), eq(Document.class)))
                .thenReturn(aggregationResults);

        BigDecimal result = dailyPaymentSumLogsQueryService.findPaymentSumByDateRangeForAllUsers(startDate, endDate);

        assertEquals(BigDecimal.ZERO, result);
        verify(mongoTemplate).aggregate(any(Aggregation.class), eq("daily_payment_sums"), eq(Document.class));
    }
}