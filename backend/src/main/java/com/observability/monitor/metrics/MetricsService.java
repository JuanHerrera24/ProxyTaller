package com.observability.monitor.metrics;

import com.observability.monitor.logging.CallLog;
import com.observability.monitor.logging.CallLogRepository;
import com.observability.monitor.proxy.LoggingProxy;
import com.observability.monitor.registry.ServiceRegistry;
import com.observability.monitor.service.support.ServiceResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Computes observability indicators from the audit logs captured by the logging proxies.
 */
@Service
public class MetricsService {

    /**
     * Error rate (percentage) above which a service is flagged as degraded.
     */
    public static final double DEGRADED_ERROR_RATE_THRESHOLD = 15.0;

    private final CallLogRepository repository;
    private final ServiceRegistry registry;
    private final Clock clock;

    public MetricsService(CallLogRepository repository, ServiceRegistry registry) {
        this.repository = repository;
        this.registry = registry;
        this.clock = Clock.systemUTC();
    }

    public MetricsSummary summary() {
        List<CallLog> logs = repository.findAll();
        Map<String, List<CallLog>> logsByService = logs.stream().collect(Collectors.groupingBy(CallLog::serviceId));

        List<ServiceMetrics> services = registry.proxies().stream()
                .map(proxy -> metricsFor(proxy, logsByService.getOrDefault(proxy.serviceId(), List.of())))
                .toList();

        long errorCount = logs.stream().filter(log -> !log.isSuccessful()).count();
        double averageDuration = logs.stream().mapToLong(CallLog::durationMs).average().orElse(0);

        return new MetricsSummary(
                clock.instant(),
                logs.size(),
                percentage(errorCount, logs.size()),
                round(averageDuration),
                DEGRADED_ERROR_RATE_THRESHOLD,
                services);
    }

    /**
     * Returns, for every service, its latest calls in chronological order.
     */
    public Map<String, List<TimelinePoint>> timeline(int limit) {
        Map<String, List<TimelinePoint>> timeline = new LinkedHashMap<>();
        for (String serviceId : registry.serviceIds()) {
            timeline.put(serviceId, repository.findLatestByService(serviceId, limit).stream()
                    .map(TimelinePoint::from)
                    .toList());
        }
        return timeline;
    }

    public PageResponse<CallLog> searchLogs(LogQuery query) {
        if (query.serviceId() != null) {
            registry.proxyFor(query.serviceId());
        }
        List<CallLog> matches = repository.findAll().stream()
                .filter(query.asPredicate())
                .sorted(Comparator.comparing(CallLog::timestamp).reversed())
                .toList();
        return PageResponse.slice(matches, query.page(), query.size());
    }

    private ServiceMetrics metricsFor(LoggingProxy<ServiceResponse> proxy, List<CallLog> logs) {
        LongSummaryStatistics durations = logs.stream().mapToLong(CallLog::durationMs).summaryStatistics();
        long successCount = logs.stream().filter(CallLog::isSuccessful).count();
        long errorCount = logs.size() - successCount;
        double errorRate = percentage(errorCount, logs.size());
        Instant lastCallAt = logs.stream()
                .map(CallLog::timestamp)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);

        return new ServiceMetrics(
                proxy.serviceId(),
                proxy.displayName(),
                logs.size(),
                successCount,
                errorCount,
                percentage(successCount, logs.size()),
                errorRate,
                round(durations.getCount() == 0 ? 0 : durations.getAverage()),
                durations.getCount() == 0 ? 0 : durations.getMin(),
                durations.getCount() == 0 ? 0 : durations.getMax(),
                lastCallAt,
                errorRate > DEGRADED_ERROR_RATE_THRESHOLD);
    }

    private static double percentage(long part, long total) {
        return total == 0 ? 0 : round(part * 100.0 / total);
    }

    private static double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
