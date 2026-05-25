package org.innowise.internship.paymentservice.model.entity.outbox;

public enum PaymentOutboxStatus {
    PENDING,
    PROCESSING,
    SENT,
    FAILED
}
