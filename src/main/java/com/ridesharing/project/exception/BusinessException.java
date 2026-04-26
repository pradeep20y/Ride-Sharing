package com.ridesharing.project.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when an operation is technically valid (no missing resource, no bad JSON)
 * but violates a business rule — for example, registering a license plate that is
 * already in use by another driver.
 *
 * <p>Maps to HTTP 409 Conflict, which correctly signals "the request could not be
 * completed due to a conflict with the current state of the resource."
 *
 * <p>Usage:
 * <pre>{@code
 *   throw new BusinessException("License plate '" + plate + "' is already registered to another driver");
 * }</pre>
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class BusinessException extends RuntimeException {

    /**
     * @param message human-readable description of the violated business rule
     */
    public BusinessException(String message) {
        super(message);
    }

    /**
     * @param message human-readable description of the violated business rule
     * @param cause   the underlying exception that triggered this violation
     */
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
