package org.innowise.internship.paymentservice.model.dto.messagerequest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.innowise.internship.paymentservice.model.entity.outbox.PaymentOutboxStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePaymentOutboxRequestDto {
    @NotBlank
    private String paymentId;

    @NotNull
    private Long orderId;

    @NotBlank
    private String userId;

    @NotNull
    private PaymentOutboxStatus status;
}
