package com.observability.monitor.simulation;

import com.observability.monitor.config.MonitorProperties;
import com.observability.monitor.logging.CallStatus;
import com.observability.monitor.proxy.ProxyInvocationException;
import com.observability.monitor.registry.ServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Fires a batch of random requests through the logging proxies, concurrently, to populate the audit logs.
 */
@Service
public class LoadSimulator {

    private static final Logger log = LoggerFactory.getLogger(LoadSimulator.class);

    private final ServiceRegistry registry;
    private final TrafficGenerator trafficGenerator;
    private final MonitorProperties.Simulation settings;

    public LoadSimulator(ServiceRegistry registry, TrafficGenerator trafficGenerator, MonitorProperties properties) {
        this.registry = registry;
        this.trafficGenerator = trafficGenerator;
        this.settings = properties.simulation();
    }

    public int defaultCalls() {
        return settings.defaultCalls();
    }

    public LoadSimulationResult simulate(int calls) {
        if (calls < 1 || calls > settings.maxCalls()) {
            throw new IllegalArgumentException("calls must be between 1 and " + settings.maxCalls());
        }

        Instant startedAt = Instant.now();
        long startNanos = System.nanoTime();
        log.info("Starting load simulation with {} calls", calls);

        List<Outcome> outcomes = runConcurrently(calls);

        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
        long errorCount = outcomes.stream().filter(outcome -> outcome.status() == CallStatus.ERROR).count();

        log.info("Load simulation finished: {} calls, {} errors, {} ms", calls, errorCount, elapsedMs);

        return new LoadSimulationResult(
                calls,
                calls - errorCount,
                errorCount,
                elapsedMs,
                startedAt,
                countByService(outcomes, false),
                countByService(outcomes, true));
    }

    private List<Outcome> runConcurrently(int calls) {
        List<Future<Outcome>> futures = new ArrayList<>(calls);
        try (ExecutorService executor = Executors.newFixedThreadPool(settings.parallelism())) {
            for (int i = 0; i < calls; i++) {
                futures.add(executor.submit(this::executeRandomCall));
            }
        }

        List<Outcome> outcomes = new ArrayList<>(calls);
        for (Future<Outcome> future : futures) {
            try {
                outcomes.add(future.get());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Load simulation was interrupted", ex);
            } catch (ExecutionException ex) {
                throw new IllegalStateException("Load simulation task failed unexpectedly", ex.getCause());
            }
        }
        return outcomes;
    }

    private Outcome executeRandomCall() {
        SimulatedCall call = trafficGenerator.next();
        try {
            registry.proxyFor(call.serviceId()).invoke(call.operation(), call.parameters());
            return new Outcome(call.serviceId(), CallStatus.SUCCESS);
        } catch (ProxyInvocationException ex) {
            return new Outcome(call.serviceId(), CallStatus.ERROR);
        }
    }

    private Map<String, Long> countByService(List<Outcome> outcomes, boolean errorsOnly) {
        Map<String, Long> counts = outcomes.stream()
                .filter(outcome -> !errorsOnly || outcome.status() == CallStatus.ERROR)
                .collect(Collectors.groupingBy(Outcome::serviceId, TreeMap::new, Collectors.counting()));
        registry.serviceIds().forEach(serviceId -> counts.putIfAbsent(serviceId, 0L));
        return counts;
    }

    private record Outcome(String serviceId, CallStatus status) {
    }
}
