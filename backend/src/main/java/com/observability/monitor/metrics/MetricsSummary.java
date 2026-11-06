package com.observability.monitor.metrics;

import java.time.Instant;
import java.util.List;

public record MetricsSummary(
        Instant generatedAt,
        long totalCalls,
        double errorRate,
        double averageResponseTimeMs,
        double degradedThreshold,
        List<ServiceMetrics> services) {
}
