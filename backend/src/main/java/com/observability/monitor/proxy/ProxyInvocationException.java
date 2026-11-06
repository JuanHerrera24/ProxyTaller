package com.observability.monitor.proxy;

/**
 * Raised by a {@link LoggingProxy} when the wrapped service fails. It carries the tracing metadata of the
 * failed call so that the API layer can correlate the error response with the persisted log entry.
 */
public class ProxyInvocationException extends RuntimeException {

    private final String requestId;
    private final String serviceId;
    private final String operation;
    private final long durationMs;

    public ProxyInvocationException(String requestId, String serviceId, String operation,
                                    long durationMs, RuntimeException cause) {
        super(cause.getMessage(), cause);
        this.requestId = requestId;
        this.serviceId = serviceId;
        this.operation = operation;
        this.durationMs = durationMs;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getServiceId() {
        return serviceId;
    }

    public String getOperation() {
        return operation;
    }

    public long getDurationMs() {
        return durationMs;
    }
}
