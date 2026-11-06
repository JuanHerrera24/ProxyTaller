package com.observability.monitor.proxy;

import com.observability.monitor.logging.CallStatus;

import java.time.Instant;

/**
 * Outcome of a successful call routed through a {@link LoggingProxy}, enriched with tracing metadata.
 */
public record ProxyExecution<T>(
        String requestId,
        String serviceId,
        String operation,
        CallStatus status,
        long durationMs,
        Instant timestamp,
        T data) {
}
