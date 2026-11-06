package com.observability.monitor.metrics;

import java.time.Instant;

/**
 * Aggregated usage indicators of a single service. Rates are expressed as percentages.
 */
public record ServiceMetrics(
        String serviceId,
        String displayName,
        long totalCalls,
        long successCount,
        long errorCount,
        double successRate,
        double errorRate,
        double averageResponseTimeMs,
        long minResponseTimeMs,
        long maxResponseTimeMs,
        Instant lastCallAt,
        boolean degraded) {
}
