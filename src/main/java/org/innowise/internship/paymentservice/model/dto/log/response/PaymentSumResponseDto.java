package org.innowise.internship.paymentservice.model.dto.log.response;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentSumResponseDto {

    @NotNull
    private BigDecimal paymentSum;
}
