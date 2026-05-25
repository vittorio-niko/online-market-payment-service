package org.innowise.internship.paymentservice.model.dto.log.response;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyPaymentSumLogResponseDto {
    @NotNull
    private LocalDate date;

    @NotNull
    private BigDecimal paymentSum;
}
