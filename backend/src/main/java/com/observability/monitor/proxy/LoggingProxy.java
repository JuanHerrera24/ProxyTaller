package com.observability.monitor.proxy;

import com.observability.monitor.logging.CallLog;
import com.observability.monitor.logging.CallLogRepository;
import com.observability.monitor.logging.CallStatus;
import com.observability.monitor.logging.ErrorDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Auditing proxy that decorates any {@link ManagedService}.
 * <ul>
 *     <li>Before the call: assigns a unique request id and records the start event.</li>
 *     <li>After the call: records the duration and the returned result.</li>
 *     <li>On failure: records the error with a summarized stack trace and rethrows it.</li>
 * </ul>
 * The proxy is transparent for callers because it implements the same contract as the wrapped service.
 */
public class LoggingProxy<T> implements ManagedService<T> {

    private static final Logger log = LoggerFactory.getLogger(LoggingProxy.class);

    private final ManagedService<T> target;
    private final CallLogRepository repository;
    private final Clock clock;

    public LoggingProxy(ManagedService<T> target, CallLogRepository repository) {
        this(target, repository, Clock.systemUTC());
    }

    public LoggingProxy(ManagedService<T> target, CallLogRepository repository, Clock clock) {
        this.target = Objects.requireNonNull(target, "target service is required");
        this.repository = Objects.requireNonNull(repository, "log repository is required");
        this.clock = Objects.requireNonNull(clock, "clock is required");
    }

    @Override
    public T execute(String operation, Object... params) {
        return invoke(operation, params).data();
    }

    /**
     * Executes the operation and returns the result together with its tracing metadata.
     *
     * @throws ProxyInvocationException when the wrapped service fails
     */
    public ProxyExecution<T> invoke(String operation, Object... params) {
        String requestId = UUID.randomUUID().toString();
        String serviceId = target.serviceId();
        Instant startedAt = clock.instant();
        long startNanos = System.nanoTime();
        Object auditedParameters = describeParameters(params);

        log.info("[{}] START   service={} operation={} params={}", requestId, serviceId, operation, auditedParameters);

        try {
            T result = target.execute(operation, params);
            long durationMs = elapsedMillisSince(startNanos);

            repository.save(new CallLog(requestId, serviceId, operation, CallStatus.SUCCESS, durationMs,
                    startedAt, auditedParameters, result, null));
            log.info("[{}] SUCCESS service={} operation={} durationMs={}", requestId, serviceId, operation, durationMs);

            return new ProxyExecution<>(requestId, serviceId, operation, CallStatus.SUCCESS, durationMs, startedAt, result);
        } catch (RuntimeException failure) {
            long durationMs = elapsedMillisSince(startNanos);
            ErrorDetails error = ErrorDetails.from(failure);

            repository.save(new CallLog(requestId, serviceId, operation, CallStatus.ERROR, durationMs,
                    startedAt, auditedParameters, null, error));
            log.warn("[{}] ERROR   service={} operation={} durationMs={} error={}: {}",
                    requestId, serviceId, operation, durationMs, error.type(), error.message());

            throw new ProxyInvocationException(requestId, serviceId, operation, durationMs, failure);
        }
    }

    @Override
    public String serviceId() {
        return target.serviceId();
    }

    @Override
    public String displayName() {
        return target.displayName();
    }

    @Override
    public Set<String> supportedOperations() {
        return target.supportedOperations();
    }

    private static long elapsedMillisSince(long startNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }

    private static Object describeParameters(Object... params) {
        if (params == null || params.length == 0) {
            return Collections.emptyMap();
        }
        if (params.length == 1) {
            return params[0] == null ? Collections.emptyMap() : params[0];
        }
        return Arrays.asList(params);
    }
}
