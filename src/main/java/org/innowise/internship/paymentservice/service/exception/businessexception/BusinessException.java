package org.innowise.internship.paymentservice.service.exception.businessexception;

public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
