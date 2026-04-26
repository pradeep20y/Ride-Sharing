package com.ridesharing.project.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a requested resource (Driver, User, etc.) does not exist in the database.
 *
 * <p>The {@code @ResponseStatus} annotation ensures Spring MVC returns HTTP 404
 * automatically if this exception escapes to the servlet layer, but in practice
 * the {@link GlobalExceptionHandler} catches it first for a richer error body.
 *
 * <p>Usage:
 * <pre>{@code
 *   throw new ResourceNotFoundException("Driver", "id", driverId);
 *   // produces: "Driver not found with id: 'abc-123'"
 * }</pre>
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    private final String resourceName;
    private final String fieldName;
    private final Object fieldValue;

    /**
     * Constructs the exception with a descriptive message built from the
     * resource type, the lookup field, and the value that was searched for.
     *
     * @param resourceName simple class name of the resource (e.g., "Driver")
     * @param fieldName    name of the field used for the lookup (e.g., "id")
     * @param fieldValue   the value that was not found
     */
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue));
        this.resourceName = resourceName;
        this.fieldName    = fieldName;
        this.fieldValue   = fieldValue;
    }

    public String getResourceName() { return resourceName; }
    public String getFieldName()    { return fieldName; }
    public Object getFieldValue()   { return fieldValue; }
}
