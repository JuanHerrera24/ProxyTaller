package com.observability.monitor.exception;

/**
 * Represents a transient failure of a downstream dependency.
 */
public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException(String message) {
        super(message);
    }
}
