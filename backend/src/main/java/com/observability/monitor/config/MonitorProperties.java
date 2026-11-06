package com.observability.monitor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;

/**
 * Strongly typed view over the {@code monitor.*} configuration namespace.
 */
@ConfigurationProperties(prefix = "monitor")
public record MonitorProperties(Cors cors, Logs logs, Payments payments, Simulation simulation) {

    public MonitorProperties {
        cors = cors != null ? cors : new Cors("*");
        logs = logs != null ? logs : new Logs(5000);
        payments = payments != null ? payments : new Payments(0.10);
        simulation = simulation != null ? simulation : new Simulation(50, 500, 8);
    }

    public record Cors(String allowedOrigins) {

        public List<String> originPatterns() {
            if (allowedOrigins == null || allowedOrigins.isBlank()) {
                return List.of("*");
            }
            return Arrays.stream(allowedOrigins.split(","))
                    .map(String::trim)
                    .filter(origin -> !origin.isEmpty())
                    .toList();
        }
    }

    public record Logs(int capacity) {

        public Logs {
            if (capacity <= 0) {
                throw new IllegalArgumentException("monitor.logs.capacity must be greater than zero");
            }
        }
    }

    public record Payments(double failureRate) {

        public Payments {
            if (failureRate < 0 || failureRate > 1) {
                throw new IllegalArgumentException("monitor.payments.failure-rate must be between 0 and 1");
            }
        }
    }

    public record Simulation(int defaultCalls, int maxCalls, int parallelism) {

        public Simulation {
            if (defaultCalls <= 0 || maxCalls < defaultCalls || parallelism <= 0) {
                throw new IllegalArgumentException("monitor.simulation values are inconsistent");
            }
        }
    }
}
