package com.ridesharing.project.exception;

import com.ridesharing.project.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Centralised exception handler for the entire API.
 *
 * <p>{@code @RestControllerAdvice} intercepts exceptions thrown from any
 * {@code @RestController} and converts them into a uniform {@link ErrorResponse}
 * JSON body. This keeps controllers thin — they never catch exceptions themselves.
 *
 * <p>Handler priority (Spring picks the most specific matching handler):
 * <ol>
 *   <li>{@link ResourceNotFoundException}  → 404 Not Found</li>
 *   <li>{@link BusinessException}          → 409 Conflict</li>
 *   <li>{@link MethodArgumentNotValidException} → 400 (bean validation on @RequestBody)</li>
 *   <li>{@link ConstraintViolationException}    → 400 (bean validation on @PathVariable / @RequestParam)</li>
 *   <li>{@link HttpMessageNotReadableException} → 400 (malformed / missing JSON body)</li>
 *   <li>{@link MethodArgumentTypeMismatchException} → 400 (wrong type in path/query param)</li>
 *   <li>{@link HttpRequestMethodNotSupportedException} → 405 Method Not Allowed</li>
 *   <li>{@link NoResourceFoundException}   → 404 (no handler matched the path)</li>
 *   <li>{@link Exception}                  → 500 Internal Server Error (catch-all)</li>
 * </ol>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ── Domain exceptions ────────────────────────────────────────────────────

    /**
     * Handles lookups for drivers / users that do not exist.
     * Returns HTTP 404 with the descriptive message built by the exception.
     *
     * @param ex      the thrown exception carrying resource/field/value info
     * @param request the current HTTP request (used to populate the path field)
     * @return 404 error response
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {

        log.warn("Resource not found: {}", ex.getMessage());

        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .error("RESOURCE_NOT_FOUND")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    /**
     * Handles business-rule violations such as duplicate license plates.
     * Returns HTTP 409 Conflict because the client request itself is valid
     * but conflicts with the current state of the data.
     *
     * @param ex      the thrown exception carrying the human-readable rule message
     * @param request the current HTTP request
     * @return 409 error response
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex, HttpServletRequest request) {

        log.warn("Business rule violation: {}", ex.getMessage());

        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.CONFLICT.value())
                .error("BUSINESS_RULE_VIOLATION")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    // ── Validation exceptions ────────────────────────────────────────────────

    /**
     * Handles {@code @Valid} failures on {@code @RequestBody} parameters.
     *
     * <p>Each failing field becomes an entry in the {@code fieldErrors} list so
     * clients can highlight the exact fields that need correction.
     *
     * @param ex      the Spring MVC validation exception containing all field errors
     * @param request the current HTTP request
     * @return 400 error response with per-field error details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toFieldError)
                .collect(Collectors.toList());

        log.warn("Validation failed on {} field(s) for path {}: {}",
                fieldErrors.size(), request.getRequestURI(),
                fieldErrors.stream().map(ErrorResponse.FieldError::getMessage).collect(Collectors.joining("; ")));

        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("VALIDATION_FAILED")
                .message("Request contains " + fieldErrors.size() + " validation error(s). See fieldErrors for details.")
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .fieldErrors(fieldErrors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Handles constraint violations triggered on {@code @PathVariable} or
     * {@code @RequestParam} parameters annotated with JSR-303 constraints.
     *
     * @param ex      the constraint violation exception
     * @param request the current HTTP request
     * @return 400 error response
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {

        List<ErrorResponse.FieldError> fieldErrors = ex.getConstraintViolations()
                .stream()
                .map(cv -> ErrorResponse.FieldError.builder()
                        .field(cv.getPropertyPath().toString())
                        .rejectedValue(cv.getInvalidValue() != null ? cv.getInvalidValue().toString() : null)
                        .message(cv.getMessage())
                        .build())
                .collect(Collectors.toList());

        log.warn("Constraint violation at path {}: {}", request.getRequestURI(), ex.getMessage());

        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("CONSTRAINT_VIOLATION")
                .message("One or more request parameters failed validation. See fieldErrors for details.")
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .fieldErrors(fieldErrors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // ── HTTP / MVC exceptions ────────────────────────────────────────────────

    /**
     * Handles cases where the request body is missing, malformed JSON, or cannot
     * be deserialised into the expected type.
     *
     * @param ex      the parsing exception
     * @param request the current HTTP request
     * @return 400 error response
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableMessage(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        log.warn("Malformed request body at path {}: {}", request.getRequestURI(), ex.getMessage());

        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("MALFORMED_REQUEST")
                .message("Request body is missing, malformed, or contains an unrecognised value. Please check your JSON.")
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Handles type-mismatch errors when a path variable or query parameter
     * cannot be converted to its declared type (e.g., passing "abc" for a Long id).
     *
     * @param ex      the type-mismatch exception
     * @param request the current HTTP request
     * @return 400 error response
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        String expected = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        String message  = String.format("Parameter '%s' must be of type %s", ex.getName(), expected);

        log.warn("Type mismatch at path {}: {}", request.getRequestURI(), message);

        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("TYPE_MISMATCH")
                .message(message)
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Handles requests made with an HTTP method not supported by the endpoint
     * (e.g., DELETE on a read-only resource).
     *
     * @param ex      the method-not-supported exception
     * @param request the current HTTP request
     * @return 405 error response
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {

        log.warn("Method not allowed at path {}: {}", request.getRequestURI(), ex.getMessage());

        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.METHOD_NOT_ALLOWED.value())
                .error("METHOD_NOT_ALLOWED")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(body);
    }

    /**
     * Handles requests to paths that have no matching controller handler.
     *
     * @param ex      the no-handler exception
     * @param request the current HTTP request
     * @return 404 error response
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFound(
            NoResourceFoundException ex, HttpServletRequest request) {

        log.warn("No handler found for path: {}", request.getRequestURI());

        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .error("ENDPOINT_NOT_FOUND")
                .message("No endpoint found for " + request.getMethod() + " " + request.getRequestURI())
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    // ── Catch-all ────────────────────────────────────────────────────────────

    /**
     * Catch-all handler for any unexpected exception that escapes the more
     * specific handlers above.  The full stack-trace is logged at ERROR level
     * but a sanitised message (no internal detail) is returned to the client.
     *
     * @param ex      the unexpected exception
     * @param request the current HTTP request
     * @return 500 error response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {

        // Log the full stack-trace so developers can diagnose the root cause
        log.error("Unexpected error at path {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("INTERNAL_SERVER_ERROR")
                .message("An unexpected error occurred. Our team has been notified. Please try again later.")
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Converts a Spring {@link FieldError} into the API's {@link ErrorResponse.FieldError} shape.
     *
     * @param fe the Spring field error from bean validation
     * @return mapped FieldError for the error response
     */
    private ErrorResponse.FieldError toFieldError(FieldError fe) {
        return ErrorResponse.FieldError.builder()
                .field(fe.getField())
                .rejectedValue(fe.getRejectedValue() != null ? fe.getRejectedValue().toString() : null)
                .message(fe.getDefaultMessage())
                .build();
    }
}
