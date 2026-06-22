package org.innowise.internship.paymentservice.service.logservice;

import org.innowise.internship.paymentservice.model.dto.log.request.CreateDailyPaymentSumLogRequestDto;
import org.innowise.internship.paymentservice.model.entity.log.DailyPaymentSumLog;
import org.innowise.internship.paymentservice.model.mapper.DailyPaymentSumLogMapper;
import org.innowise.internship.paymentservice.repository.DailyPaymentSumLogsRepository;
import org.innowise.internship.paymentservice.service.exception.businessexception.InvalidArgumentException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyPaymentSumLogsServiceTest {

    @Mock
    private DailyPaymentSumLogsRepository dailyPaymentSumLogsRepository;

    @Mock
    private DailyPaymentSumLogMapper dailyPaymentSumLogMapper;

    @Mock
    private PaymentLogsQueryService paymentLogsQueryService;

    @InjectMocks
    private DailyPaymentSumLogsService dailyPaymentSumLogsService;

    private final LocalDate now = LocalDate.now();
    private final LocalDate pastDate = now.minusDays(5);
    private final Instant pastDateInstant = pastDate.atStartOfDay(ZoneOffset.UTC).toInstant();
    private final BigDecimal paymentSum = BigDecimal.valueOf(1500.75);

    @Test
    void createDailyPaymentSumLog_shouldCreateNewLog_whenNoExistingLog() {
        CreateDailyPaymentSumLogRequestDto dto = new CreateDailyPaymentSumLogRequestDto();
        dto.setDate(pastDate);

        DailyPaymentSumLog newLog = DailyPaymentSumLog.builder()
                .date(pastDateInstant)
                .build();

        when(paymentLogsQueryService.findPaymentSumByDateAndStatusSuccessForAllUsers(pastDate))
                .thenReturn(paymentSum);
        when(dailyPaymentSumLogsRepository.findByDate(pastDateInstant))
                .thenReturn(Optional.empty());
        when(dailyPaymentSumLogMapper.toDailyPaymentSumLog(dto))
                .thenReturn(newLog);
        when(dailyPaymentSumLogsRepository.save(any(DailyPaymentSumLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DailyPaymentSumLog result = dailyPaymentSumLogsService.createDailyPaymentSumLog(dto);

        assertNotNull(result);
        assertEquals(pastDateInstant, result.getDate());
        assertEquals(paymentSum, result.getPaymentSum());
        verify(paymentLogsQueryService).findPaymentSumByDateAndStatusSuccessForAllUsers(pastDate);
        verify(dailyPaymentSumLogsRepository).findByDate(pastDateInstant);
        verify(dailyPaymentSumLogMapper).toDailyPaymentSumLog(dto);
        verify(dailyPaymentSumLogsRepository).save(newLog);
    }

    @Test
    void createDailyPaymentSumLog_shouldUpdateExistingLog_whenLogExists() {
        CreateDailyPaymentSumLogRequestDto dto = new CreateDailyPaymentSumLogRequestDto();
        dto.setDate(pastDate);

        DailyPaymentSumLog existingLog = DailyPaymentSumLog.builder()
                .date(pastDateInstant)
                .paymentSum(BigDecimal.valueOf(500))
                .build();

        when(paymentLogsQueryService.findPaymentSumByDateAndStatusSuccessForAllUsers(pastDate))
                .thenReturn(paymentSum);
        when(dailyPaymentSumLogsRepository.findByDate(pastDateInstant))
                .thenReturn(Optional.of(existingLog));
        when(dailyPaymentSumLogsRepository.save(any(DailyPaymentSumLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DailyPaymentSumLog result = dailyPaymentSumLogsService.createDailyPaymentSumLog(dto);

        assertNotNull(result);
        assertEquals(pastDateInstant, result.getDate());
        assertEquals(paymentSum, result.getPaymentSum());
        assertEquals(existingLog, result);
        verify(paymentLogsQueryService).findPaymentSumByDateAndStatusSuccessForAllUsers(pastDate);
        verify(dailyPaymentSumLogsRepository).findByDate(pastDateInstant);
        verify(dailyPaymentSumLogMapper, never()).toDailyPaymentSumLog(any());
        verify(dailyPaymentSumLogsRepository).save(existingLog);
    }

    @Test
    void createDailyPaymentSumLog_shouldThrowException_whenDateIsInFuture() {
        LocalDate futureDate = now.plusDays(1);
        CreateDailyPaymentSumLogRequestDto dto = new CreateDailyPaymentSumLogRequestDto();
        dto.setDate(futureDate);

        InvalidArgumentException exception = assertThrows(
                InvalidArgumentException.class,
                () -> dailyPaymentSumLogsService.createDailyPaymentSumLog(dto)
        );

        assertEquals("Payments for future dates do not exist", exception.getMessage());
        verify(paymentLogsQueryService, never()).findPaymentSumByDateAndStatusSuccessForAllUsers(any());
        verify(dailyPaymentSumLogsRepository, never()).findByDate(any());
        verify(dailyPaymentSumLogMapper, never()).toDailyPaymentSumLog(any());
        verify(dailyPaymentSumLogsRepository, never()).save(any());
    }

    @Test
    void createDailyPaymentSumLog_shouldHandleZeroPaymentSum() {
        CreateDailyPaymentSumLogRequestDto dto = new CreateDailyPaymentSumLogRequestDto();
        dto.setDate(pastDate);

        DailyPaymentSumLog newLog = DailyPaymentSumLog.builder()
                .date(pastDateInstant)
                .build();

        when(paymentLogsQueryService.findPaymentSumByDateAndStatusSuccessForAllUsers(pastDate))
                .thenReturn(BigDecimal.ZERO);
        when(dailyPaymentSumLogsRepository.findByDate(pastDateInstant))
                .thenReturn(Optional.empty());
        when(dailyPaymentSumLogMapper.toDailyPaymentSumLog(dto))
                .thenReturn(newLog);
        when(dailyPaymentSumLogsRepository.save(any(DailyPaymentSumLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DailyPaymentSumLog result = dailyPaymentSumLogsService.createDailyPaymentSumLog(dto);

        assertNotNull(result);
        assertEquals(pastDateInstant, result.getDate());
        assertEquals(BigDecimal.ZERO, result.getPaymentSum());
        verify(dailyPaymentSumLogsRepository).save(newLog);
    }
}
