package org.innowise.internship.paymentservice.model.dto.messagerequest;

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
public class CreatePaymentInboxRequestDto {
    @NotBlank
    private String paymentId;

    @NotNull
    private Long orderId;

    @NotBlank
    private String userId;

    @NotNull
    private BigDecimal paymentAmount;
}


