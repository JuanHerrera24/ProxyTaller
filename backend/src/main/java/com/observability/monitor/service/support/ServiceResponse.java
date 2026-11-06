package com.observability.monitor.service.support;

/**
 * Standard payload returned by every microservice operation.
 *
 * @param message short, human readable description of the outcome
 * @param data    operation specific result
 */
public record ServiceResponse(String message, Object data) {

    public static ServiceResponse of(String message, Object data) {
        return new ServiceResponse(message, data);
    }
}
