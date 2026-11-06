package com.observability.monitor.exception;

/**
 * Signals a request that is well formed but conflicts with the current state of the domain.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
