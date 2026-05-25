package org.innowise.internship.paymentservice.model.mapper;

import lombok.NonNull;
import org.innowise.internship.paymentservice.model.dto.log.request.CreateDailyPaymentSumLogRequestDto;
import org.innowise.internship.paymentservice.model.dto.log.response.DailyPaymentSumLogResponseDto;
import org.innowise.internship.paymentservice.model.entity.DailyPaymentSumLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Mapper(componentModel = "spring")
public interface DailyPaymentSumLogMapper {

    @Mapping(target = "id", ignore = true)
    DailyPaymentSumLog toDailyPaymentSumLog(
            @NonNull CreateDailyPaymentSumLogRequestDto dto
    );

    DailyPaymentSumLogResponseDto toDailyPaymentSumLogResponseDto(
            @NonNull DailyPaymentSumLog dailyPaymentSumLog
    );

    default LocalDate map(Instant instant) {
        if (instant == null) {
            return null;
        }
        return instant.atZone(ZoneOffset.UTC).toLocalDate();
    }

    default Instant mapToInstant(LocalDate date) {
        if (date == null) {
            return null;
        }

        return date.atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}
