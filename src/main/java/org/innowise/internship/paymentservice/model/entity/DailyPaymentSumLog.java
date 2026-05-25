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
@Document(collection = "daily_payment_sums")
public class DailyPaymentSumLog {
    @Id
    private String id;

    @Field("date")
    private Instant date;

    @Field(name = "payment_sum", targetType = FieldType.DECIMAL128)
    private BigDecimal paymentSum;
}
