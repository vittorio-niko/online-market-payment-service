package org.innowise.internship.paymentservice.model.dto.log.request;

import jakarta.validation.constraints.NotBlank;
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
public class CreatePaymentLogRequestDto {
    @NotNull
    private Long orderId;

    @NotBlank
    private String userId;

    @NotBlank
    private String status;

    @NotNull
    private BigDecimal paymentAmount;
}
