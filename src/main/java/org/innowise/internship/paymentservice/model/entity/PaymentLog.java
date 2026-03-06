package org.innowise.internship.paymentservice.model.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.Instant;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "payments")
public class PaymentLog {
    @Id
    private String id;

    @Field("payment_id")
    private String paymentId;

    @Field("order_id")
    private Long orderId;

    @Field("user_id")
    private String userId;

    @Field("status")
    private PaymentStatus status;

    @Field("timestamp")
    private Instant timestamp;

    @Field(name = "payment_amount", targetType = FieldType.DECIMAL128)
    private BigDecimal paymentAmount;
}
