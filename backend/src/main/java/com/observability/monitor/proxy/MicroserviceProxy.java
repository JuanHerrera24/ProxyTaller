package com.observability.monitor.proxy;

/**
 * Uniform contract shared by every microservice and by every proxy that wraps one.
 * Callers interact with a concrete service or with a decorated version of it in exactly the same way.
 *
 * @param <T> type returned by the service operations
 */
@FunctionalInterface
public interface MicroserviceProxy<T> {

    /**
     * Executes a named operation of the service.
     *
     * @param operation operation identifier, for example {@code reserve-stock}
     * @param params    operation arguments; services expect a single key/value payload
     * @return the operation result
     */
    T execute(String operation, Object... params);
}
