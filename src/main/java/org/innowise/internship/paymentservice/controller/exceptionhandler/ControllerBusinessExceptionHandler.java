package org.innowise.internship.paymentservice.controller.exceptionhandler;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.innowise.internship.paymentservice.service.exception.businessexception.InvalidArgumentException;
import org.innowise.internship.paymentservice.service.exception.businessexception.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@ControllerAdvice
public class ControllerBusinessExceptionHandler {

    @ExceptionHandler(InvalidArgumentException.class)
    public ResponseEntity<ErrorResponse> handleInvalidArgument(
            @NonNull InvalidArgumentException e
    ) {
        log.warn("Business validation failed: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        ErrorCode.INVALID_ARGUMENT,
                        e.getMessage(),
                        null
                ));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            @NonNull NotFoundException e
    ) {
        log.info("Resource not found: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        e.getMessage(),
                        null
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationFailed(
            @NonNull MethodArgumentNotValidException e
    ) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult()
                .getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        log.warn("Validation failed for request: {}", errors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        ErrorCode.INVALID_ARGUMENT,
                        "Validation failed",
                        errors
                ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleInvalidJson(
            @NonNull HttpMessageNotReadableException e
    ) {
        log.warn("Malformed JSON received: {}", e.getMostSpecificCause().getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        ErrorCode.INVALID_ARGUMENT,
                        "Malformed JSON request",
                        null
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            @NonNull Exception e
    ) {
        log.error("Unexpected system error occurred: ", e);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(
                        ErrorCode.INTERNAL_ERROR,
                        "Unexpected error",
                        null
                ));
    }
}