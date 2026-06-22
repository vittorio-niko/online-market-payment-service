package org.innowise.internship.paymentservice.service.exception.businessexception;

public class NotFoundException extends BusinessException {
    public NotFoundException(String message) {
        super(message);
    }
    public NotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
