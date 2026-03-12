package org.innowise.internship.paymentservice.service.exception.businessexception;

public class InvalidArgumentException extends BusinessException {
    public InvalidArgumentException(String message) {
        super(message);
    }
    public InvalidArgumentException(String message, Throwable cause) { super(message, cause); }
}
