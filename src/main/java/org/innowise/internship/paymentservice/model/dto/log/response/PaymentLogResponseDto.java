package org.innowise.internship.paymentservice.model.dto.log.response;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentLogResponseDto {
    @NotBlank
    private String paymentId;

    @NotNull
    private Long orderId;

    @NotBlank
    private String userId;

    @NotBlank
    private String status;

    @NotNull
    private Instant timestamp;

    @NotNull
    private BigDecimal paymentAmount;
}
