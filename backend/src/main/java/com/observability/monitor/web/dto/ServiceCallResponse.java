package com.observability.monitor.web.dto;

import com.observability.monitor.logging.CallStatus;
import com.observability.monitor.proxy.ProxyExecution;
import com.observability.monitor.service.support.ServiceResponse;

import java.time.Instant;

public record ServiceCallResponse(
        String requestId,
        String serviceId,
        String operation,
        CallStatus status,
        long durationMs,
        Instant timestamp,
        String message,
        Object data) {

    public static ServiceCallResponse from(ProxyExecution<ServiceResponse> execution) {
        ServiceResponse response = execution.data();
        return new ServiceCallResponse(
                execution.requestId(),
                execution.serviceId(),
                execution.operation(),
                execution.status(),
                execution.durationMs(),
                execution.timestamp(),
                response.message(),
                response.data());
    }
}
