package org.innowise.internship.paymentservice.model.entity.inbox;

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
@Document(collection = "inbox_payment_requests")
public class PaymentInboxRequest {
    @Id
    private String id;

    @Field("msg_id")
    private String msgId;

    @Field("order_id")
    private Long orderId;

    @Field("user_id")
    private String userId;

    @Field("timestamp")
    private Instant timestamp;

    @Field("status")
    private PaymentInboxStatus status;

    @Field(value = "payment_amount", targetType = FieldType.DECIMAL128)
    private BigDecimal paymentAmount;
}
