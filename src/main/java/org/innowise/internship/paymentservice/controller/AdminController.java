package org.innowise.internship.paymentservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.innowise.internship.paymentservice.model.dto.log.response.DailyPaymentSumLogResponseDto;
import org.innowise.internship.paymentservice.model.dto.log.response.PaymentLogResponseDto;
import org.innowise.internship.paymentservice.model.dto.log.response.PaymentLogSummaryResponseDto;
import org.innowise.internship.paymentservice.model.dto.log.response.PaymentSumResponseDto;
import org.innowise.internship.paymentservice.model.entity.log.PaymentLog;
import org.innowise.internship.paymentservice.model.mapper.DailyPaymentSumLogMapper;
import org.innowise.internship.paymentservice.model.mapper.PaymentLogMapper;
import org.innowise.internship.paymentservice.service.logservice.DailyPaymentSumLogsQueryService;
import org.innowise.internship.paymentservice.service.logservice.DailyPaymentSumLogsService;
import org.innowise.internship.paymentservice.service.logservice.PaymentLogsQueryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/admin/payments")
public class AdminController {
    private final DailyPaymentSumLogsQueryService dailyPaymentSumLogsQueryService;
    private final PaymentLogsQueryService paymentLogsQueryService;

    private final DailyPaymentSumLogMapper dailyPaymentSumLogMapper;
    private final PaymentLogMapper paymentLogMapper;

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentLogResponseDto> getPaymentLogByPaymentId(
            @PathVariable String paymentId
    ) {
        log.info("Fetching detailed payment log for ID: {}", paymentId);

        PaymentLogResponseDto response = paymentLogMapper.toPaymentLogResponseDto(
                paymentLogsQueryService.findByPaymentId(paymentId)
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<List<PaymentLogSummaryResponseDto>> getPaymentLogsByOrderId(
            @PathVariable Long orderId
    ) {
        log.info("Fetching payment logs summary for order ID: {}", orderId);

        var logs = paymentLogsQueryService.getPaymentLogsByOrderId(orderId)
                .stream()
                .map(paymentLogMapper::toPaymentLogSummaryResponseDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<Page<PaymentLogSummaryResponseDto>> getPaymentLogsByUserId(
            @PathVariable String userId,
            Pageable pageable
    ) {
        log.info("Admin request: payment logs for user ID: {}, page: {}",
                userId, pageable.getPageNumber());

        var result = paymentLogsQueryService.findByUserId(userId, pageable)
                .map(paymentLogMapper::toPaymentLogSummaryResponseDto);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/users/{userId}/payment-sum")
    public ResponseEntity<PaymentSumResponseDto> getPaymentSumByUserIdAndDate(
            @PathVariable String userId,
            @RequestParam LocalDate date
    ) {
        log.info("Calculating payment sum for user {} on date {}", userId, date);

        BigDecimal sum = paymentLogsQueryService.findPaymentSumByUserIdAndDateAndStatusSuccess(
                userId, date
        );
        return ResponseEntity.ok(new PaymentSumResponseDto(sum));
    }

    @GetMapping("/users/{userId}/payment-sum/range")
    public ResponseEntity<PaymentSumResponseDto> getPaymentSumByUserIdAndDateRange(
            @PathVariable String userId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate
    ) {
        log.info("Calculating payment sum for user {} from {} to {}", userId, startDate, endDate);

        BigDecimal sum = paymentLogsQueryService.findPaymentSumByUserIdAndDateRangeAndStatusSuccess(
                userId, startDate, endDate
        );
        return ResponseEntity.ok(new PaymentSumResponseDto(sum));
    }

    @GetMapping("/daily-sum")
    public ResponseEntity<DailyPaymentSumLogResponseDto> getDailyPaymentSumByDate(
            @RequestParam LocalDate date
    ) {
        log.info("Fetching system-wide daily payment sum for date: {}", date);

        var response = dailyPaymentSumLogMapper.toDailyPaymentSumLogResponseDto(
                dailyPaymentSumLogsQueryService.findByDate(date)
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/daily-sum/range")
    public ResponseEntity<Page<DailyPaymentSumLogResponseDto>> getDailyPaymentSumsByDateRange(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            Pageable pageable
    ) {
        log.info("Fetching daily payment sums range: {} to {}, page: {}",
                startDate, endDate, pageable.getPageNumber());

        Page<DailyPaymentSumLogResponseDto> result = dailyPaymentSumLogsQueryService
                .findAllByDateBetween(startDate, endDate, pageable)
                .map(dailyPaymentSumLogMapper::toDailyPaymentSumLogResponseDto);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/daily-sum/range/total")
    public ResponseEntity<PaymentSumResponseDto> getTotalPaymentSumForAllUsersByDateRange(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate)
    {
        log.info("Calculating total system-wide sum from {} to {}", startDate, endDate);

        BigDecimal sum = dailyPaymentSumLogsQueryService.findPaymentSumByDateRangeForAllUsers(startDate, endDate);
        return ResponseEntity.ok(new PaymentSumResponseDto(sum));
    }
}
