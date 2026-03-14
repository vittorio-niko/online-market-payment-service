package org.innowise.internship.paymentservice.scheduler;

import lombok.RequiredArgsConstructor;
import org.innowise.internship.paymentservice.model.dto.log.request.CreateDailyPaymentSumLogRequestDto;
import org.innowise.internship.paymentservice.service.DailyPaymentSumLogsService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class DailyPaymentSumLogScheduler {

    private final DailyPaymentSumLogsService dailyPaymentSumLogsService;

    @Scheduled(cron = "0 0 0 * * *") //midnight
    public void createDailySumLogForYesterday() {
        LocalDate yesterday = LocalDate.now().minusDays(1);

        var dailyPaymentSumRequestLog = new CreateDailyPaymentSumLogRequestDto();
        dailyPaymentSumRequestLog.setDate(yesterday);
        dailyPaymentSumLogsService.createDailyPaymentSumLog(dailyPaymentSumRequestLog);
    }
}
