package com.observability.monitor.service.payment;

import com.observability.monitor.exception.ServiceUnavailableException;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;

/**
 * Chaos component that makes a configurable share of calls fail with realistic gateway errors.
 */
public class FailureInjector {

    private static final List<String> GATEWAY_FAILURES = List.of(
            "Payment gateway timed out after 3000 ms",
            "Card network returned HTTP 502 Bad Gateway",
            "Fraud screening provider is unavailable",
            "Connection reset by acquiring bank",
            "Payment processor rate limit exceeded");

    private final double failureRate;
    private final DoubleSupplier randomSource;

    public FailureInjector(double failureRate) {
        this(failureRate, () -> ThreadLocalRandom.current().nextDouble());
    }

    public FailureInjector(double failureRate, DoubleSupplier randomSource) {
        if (failureRate < 0 || failureRate > 1) {
            throw new IllegalArgumentException("failureRate must be between 0 and 1");
        }
        this.failureRate = failureRate;
        this.randomSource = randomSource;
    }

    public void maybeFail(String operation) {
        if (randomSource.getAsDouble() < failureRate) {
            String reason = GATEWAY_FAILURES.get(ThreadLocalRandom.current().nextInt(GATEWAY_FAILURES.size()));
            throw new ServiceUnavailableException("%s while executing '%s'".formatted(reason, operation));
        }
    }

    public double failureRate() {
        return failureRate;
    }
}
