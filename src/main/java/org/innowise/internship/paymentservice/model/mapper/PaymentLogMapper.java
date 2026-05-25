package org.innowise.internship.paymentservice.model.mapper;

import lombok.NonNull;
import org.innowise.internship.paymentservice.model.dto.log.request.CreatePaymentLogRequestDto;
import org.innowise.internship.paymentservice.model.dto.log.response.PaymentLogResponseDto;
import org.innowise.internship.paymentservice.model.dto.log.response.PaymentLogSummaryResponseDto;
import org.innowise.internship.paymentservice.model.entity.PaymentLog;
import org.innowise.internship.paymentservice.model.entity.PaymentStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentLogMapper {

    @Mapping(target = "id", ignore = true)
    PaymentLog toPaymentLog(@NonNull CreatePaymentLogRequestDto dto);

    @Mapping(target = "status", expression = "java(paymentLog.getStatus().name())")
    PaymentLogResponseDto toPaymentLogResponseDto(@NonNull PaymentLog paymentLog);

    @Mapping(target = "status", expression = "java(paymentLog.getStatus().name())")
    PaymentLogSummaryResponseDto toPaymentLogSummaryResponseDto(@NonNull PaymentLog paymentLog);

    default PaymentStatus map(String status) {
        return PaymentStatus.valueOf(status.toUpperCase());
    }
}