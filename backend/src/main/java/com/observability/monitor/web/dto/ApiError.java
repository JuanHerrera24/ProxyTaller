package com.observability.monitor.web.dto;

import java.time.Instant;

/**
 * Error payload returned by every endpoint. Tracing fields are populated only for proxied service calls.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        String requestId,
        String serviceId,
        String operation,
        Long durationMs) {

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(Instant.now(), status, error, message, path, null, null, null, null);
    }
}
