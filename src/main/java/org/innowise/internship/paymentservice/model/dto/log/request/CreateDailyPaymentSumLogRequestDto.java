package org.innowise.internship.paymentservice.model.dto.log.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateDailyPaymentSumLogRequestDto {
    @NotNull
    private LocalDate date;
}
