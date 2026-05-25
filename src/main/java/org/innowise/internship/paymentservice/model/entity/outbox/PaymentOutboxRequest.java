package org.innowise.internship.paymentservice.model.entity.outbox;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "outbox_payment_requests")
public class PaymentOutboxRequest {
    @Id
    private String id;

    @Field("payment_id")
    private String paymentId;

    @Field("order_id")
    private Long orderId;

    @Field("user_id")
    private String userId;

    @Field("timestamp")
    private Instant timestamp;

    @Field("status")
    private PaymentOutboxStatus status;

    @Field("attempts")
    @Builder.Default
    private Integer attempts = 0;
}
