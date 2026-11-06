package com.observability.monitor.proxy;

import java.util.Set;

/**
 * A microservice that can be registered in the gateway and wrapped by a {@link LoggingProxy}.
 *
 * @param <T> type returned by the service operations
 */
public interface ManagedService<T> extends MicroserviceProxy<T> {

    /**
     * Stable identifier used in routes, logs and metrics, for example {@code payments}.
     */
    String serviceId();

    /**
     * Human readable name rendered by clients.
     */
    String displayName();

    /**
     * Operations accepted by {@link #execute(String, Object...)}.
     */
    Set<String> supportedOperations();
}
