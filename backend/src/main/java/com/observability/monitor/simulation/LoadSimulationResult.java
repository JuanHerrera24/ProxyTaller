package com.observability.monitor.simulation;

import java.time.Instant;
import java.util.Map;

public record LoadSimulationResult(
        int requestedCalls,
        long successCount,
        long errorCount,
        long elapsedMs,
        Instant startedAt,
        Map<String, Long> callsByService,
        Map<String, Long> errorsByService) {
}
