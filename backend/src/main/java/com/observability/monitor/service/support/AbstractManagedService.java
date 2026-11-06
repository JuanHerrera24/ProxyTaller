package com.observability.monitor.service.support;

import com.observability.monitor.exception.InvalidRequestException;
import com.observability.monitor.exception.UnsupportedServiceOperationException;
import com.observability.monitor.proxy.ManagedService;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Base class that dispatches operation names to handler functions and applies the simulated latency.
 * Subclasses only declare their operations through {@link #register(String, Function)}.
 */
public abstract class AbstractManagedService implements ManagedService<ServiceResponse> {

    private final Map<String, Function<OperationParameters, ServiceResponse>> handlers = new LinkedHashMap<>();
    private final LatencySimulator latency;

    protected AbstractManagedService(LatencySimulator latency) {
        this.latency = latency;
    }

    protected final void register(String operation, Function<OperationParameters, ServiceResponse> handler) {
        handlers.put(operation, handler);
    }

    @Override
    public final ServiceResponse execute(String operation, Object... params) {
        if (operation == null || operation.isBlank()) {
            throw new InvalidRequestException("Operation name is required");
        }
        String normalizedOperation = operation.trim().toLowerCase(Locale.ROOT);
        Function<OperationParameters, ServiceResponse> handler = handlers.get(normalizedOperation);
        if (handler == null) {
            throw new UnsupportedServiceOperationException(serviceId(), operation, handlers.keySet());
        }

        latency.pause();
        beforeOperation(normalizedOperation);
        return handler.apply(OperationParameters.from(params));
    }

    /**
     * Hook executed after the latency and before the handler. Useful for cross-cutting concerns such as
     * fault injection.
     */
    protected void beforeOperation(String operation) {
    }

    @Override
    public final Set<String> supportedOperations() {
        return Collections.unmodifiableSet(handlers.keySet());
    }
}
