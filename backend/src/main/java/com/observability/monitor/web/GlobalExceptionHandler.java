package com.observability.monitor.web;

import com.observability.monitor.exception.BusinessRuleException;
import com.observability.monitor.exception.InvalidRequestException;
import com.observability.monitor.exception.ResourceNotFoundException;
import com.observability.monitor.exception.ServiceUnavailableException;
import com.observability.monitor.exception.UnsupportedServiceOperationException;
import com.observability.monitor.proxy.ProxyInvocationException;
import com.observability.monitor.web.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;

/**
 * Translates exceptions into consistent {@link ApiError} responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ProxyInvocationException.class)
    public ResponseEntity<ApiError> handleProxyFailure(ProxyInvocationException ex, HttpServletRequest request) {
        HttpStatus status = statusFor(ex.getCause());
        ApiError body = new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI(),
                ex.getRequestId(),
                ex.getServiceId(),
                ex.getOperation(),
                ex.getDurationMs());
        return ResponseEntity.status(status)
                .header(ServiceGatewayController.REQUEST_ID_HEADER, ex.getRequestId())
                .body(body);
    }

    @ExceptionHandler({IllegalArgumentException.class, InvalidRequestException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiError> handleBadRequest(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Request body must be a valid JSON object", request);
    }

    @ExceptionHandler({ResourceNotFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ApiError> handleNotFound(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled error on {}", request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request);
    }

    private static HttpStatus statusFor(Throwable cause) {
        if (cause instanceof ResourceNotFoundException) {
            return HttpStatus.NOT_FOUND;
        }
        if (cause instanceof InvalidRequestException
                || cause instanceof UnsupportedServiceOperationException
                || cause instanceof IllegalArgumentException) {
            return HttpStatus.BAD_REQUEST;
        }
        if (cause instanceof BusinessRuleException) {
            return HttpStatus.CONFLICT;
        }
        if (cause instanceof ServiceUnavailableException) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private static ResponseEntity<ApiError> build(HttpStatus status, String message, HttpServletRequest request) {
        return ResponseEntity.status(status)
                .body(ApiError.of(status.value(), status.getReasonPhrase(), message, request.getRequestURI()));
    }
}
