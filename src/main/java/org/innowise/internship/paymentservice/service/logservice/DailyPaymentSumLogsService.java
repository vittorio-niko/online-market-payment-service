package org.innowise.internship.paymentservice.service.logservice;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.innowise.internship.paymentservice.model.dto.log.request.CreateDailyPaymentSumLogRequestDto;
import org.innowise.internship.paymentservice.model.entity.log.DailyPaymentSumLog;
import org.innowise.internship.paymentservice.model.mapper.DailyPaymentSumLogMapper;
import org.innowise.internship.paymentservice.repository.DailyPaymentSumLogsRepository;
import org.innowise.internship.paymentservice.service.exception.businessexception.InvalidArgumentException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DailyPaymentSumLogsService {
    private final DailyPaymentSumLogsRepository dailyPaymentSumLogsRepository;
    private final DailyPaymentSumLogMapper dailyPaymentSumLogMapper;

    private final PaymentLogsQueryService paymentLogsQueryService;

    @Transactional
    public DailyPaymentSumLog createDailyPaymentSumLog(@NonNull CreateDailyPaymentSumLogRequestDto dto) {
        var date = dto.getDate();
        if (date.isAfter(LocalDate.now())) {
            throw new InvalidArgumentException("Payments for future dates do not exist");
        }

        Instant day = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        BigDecimal paymentSum = paymentLogsQueryService.findPaymentSumByDateForAllUsers(date);

        Optional<DailyPaymentSumLog> existingLog = dailyPaymentSumLogsRepository.findByDate(day);

        DailyPaymentSumLog log;
        if (existingLog.isPresent()) {
            log = existingLog.get();
            log.setPaymentSum(paymentSum);
        } else {
            log = dailyPaymentSumLogMapper.toDailyPaymentSumLog(dto);
            log.setPaymentSum(paymentSum);
        }

        return dailyPaymentSumLogsRepository.save(log);
    }
}
