package com.observability.monitor.logging;

import java.time.Instant;

/**
 * Immutable audit record produced by the logging proxy for every intercepted call.
 *
 * @param requestId  unique identifier (UUID) of the call
 * @param serviceId  identifier of the invoked service
 * @param operation  invoked operation
 * @param status     final outcome of the call
 * @param durationMs elapsed time in milliseconds
 * @param timestamp  instant at which the call started
 * @param parameters input arguments received by the service
 * @param response   returned payload, present only for successful calls
 * @param error      failure details, present only for failed calls
 */
public record CallLog(
        String requestId,
        String serviceId,
        String operation,
        CallStatus status,
        long durationMs,
        Instant timestamp,
        Object parameters,
        Object response,
        ErrorDetails error) {

    public boolean isSuccessful() {
        return status == CallStatus.SUCCESS;
    }
}
