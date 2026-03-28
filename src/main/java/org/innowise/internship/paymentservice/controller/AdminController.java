package org.innowise.internship.paymentservice.controller;

import lombok.RequiredArgsConstructor;
import org.innowise.internship.paymentservice.model.dto.log.response.DailyPaymentSumLogResponseDto;
import org.innowise.internship.paymentservice.model.dto.log.response.PaymentLogResponseDto;
import org.innowise.internship.paymentservice.model.dto.log.response.PaymentLogSummaryResponseDto;
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

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/admin/payments")
public class AdminController {
    private final DailyPaymentSumLogsQueryService dailyPaymentSumLogsQueryService;
    private final PaymentLogsQueryService paymentLogsQueryService;

    private final DailyPaymentSumLogMapper dailyPaymentSumLogMapper;
    private final PaymentLogMapper paymentLogMapper;

    @GetMapping("/{payment_id}")
    ResponseEntity<PaymentLogResponseDto> getPaymentLogByPaymentId(@PathVariable String paymentId) {

        PaymentLogResponseDto response = paymentLogMapper.toPaymentLogResponseDto(
            paymentLogsQueryService.findByPaymentId(paymentId)
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<List<PaymentLogSummaryResponseDto>> getPaymentLogsByOrderId(
            @PathVariable Long orderId
    ) {
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
        var result = paymentLogsQueryService.findByUserId(userId, pageable)
                .map(paymentLogMapper::toPaymentLogSummaryResponseDto);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/users/{userId}/payment-sum")
    public ResponseEntity<BigDecimal> getPaymentSumByUserIdAndDate(
            @PathVariable String userId,
            @RequestParam LocalDate date
    ) {
        return ResponseEntity.ok(
                paymentLogsQueryService.findPaymentSumByUserIdAndDateAndStatusSuccess(
                        userId, date
                )
        );
    }

    @GetMapping("/users/{userId}/payment-sum/range")
    public ResponseEntity<BigDecimal> getPaymentSumByUserIdAndDateRange(
            @PathVariable String userId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate
    ) {
        return ResponseEntity.ok(
                paymentLogsQueryService.findPaymentSumByUserIdAndDateRangeAndStatusSuccess(
                        userId, startDate, endDate
                )
        );
    }

    @GetMapping("/daily-sum")
    public ResponseEntity<DailyPaymentSumLogResponseDto> getDailyPaymentSumByDate(
            @RequestParam LocalDate date
    ) {
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
        Page<DailyPaymentSumLogResponseDto> result = dailyPaymentSumLogsQueryService
                .findAllByDateBetween(startDate, endDate, pageable)
                .map(dailyPaymentSumLogMapper::toDailyPaymentSumLogResponseDto);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/daily-sum/range/total")
    public ResponseEntity<BigDecimal> getTotalPaymentSumForAllUsersByDateRange(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate)
    {
        return ResponseEntity.ok(
                dailyPaymentSumLogsQueryService.findPaymentSumByDateRangeForAllUsers(startDate, endDate)
        );
    }
}
