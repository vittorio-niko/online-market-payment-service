package org.innowise.internship.paymentservice.service.logservice;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.innowise.internship.paymentservice.model.dto.log.request.CreatePaymentLogRequestDto;
import org.innowise.internship.paymentservice.model.entity.log.PaymentLog;
import org.innowise.internship.paymentservice.model.mapper.PaymentLogMapper;
import org.innowise.internship.paymentservice.repository.PaymentLogsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentLogsService {
    private final PaymentLogsRepository paymentLogsRepository;
    private final PaymentLogMapper paymentLogMapper;

    @Transactional
    public PaymentLog createPaymentLog(@NonNull CreatePaymentLogRequestDto dto) {
        PaymentLog log = paymentLogMapper.toPaymentLog(dto);
        log.setTimestamp(Instant.now());

        return paymentLogsRepository.save(log);
    }
}
