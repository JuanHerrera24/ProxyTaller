package com.observability.monitor.web;

import com.observability.monitor.logging.CallLog;
import com.observability.monitor.logging.CallLogRepository;
import com.observability.monitor.logging.CallStatus;
import com.observability.monitor.metrics.LogQuery;
import com.observability.monitor.metrics.MetricsService;
import com.observability.monitor.metrics.MetricsSummary;
import com.observability.monitor.metrics.PageResponse;
import com.observability.monitor.metrics.TimelinePoint;
import com.observability.monitor.exception.ResourceNotFoundException;
import com.observability.monitor.simulation.LoadSimulationResult;
import com.observability.monitor.simulation.LoadSimulator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/metrics")
public class MetricsController {

    private static final int MAX_TIMELINE_LIMIT = 100;

    private final MetricsService metricsService;
    private final LoadSimulator loadSimulator;
    private final CallLogRepository repository;

    public MetricsController(MetricsService metricsService, LoadSimulator loadSimulator, CallLogRepository repository) {
        this.metricsService = metricsService;
        this.loadSimulator = loadSimulator;
        this.repository = repository;
    }

    @GetMapping("/summary")
    public MetricsSummary summary() {
        return metricsService.summary();
    }

    @GetMapping("/timeline")
    public Map<String, List<TimelinePoint>> timeline(@RequestParam(defaultValue = "20") int limit) {
        if (limit < 1 || limit > MAX_TIMELINE_LIMIT) {
            throw new IllegalArgumentException("limit must be between 1 and " + MAX_TIMELINE_LIMIT);
        }
        return metricsService.timeline(limit);
    }

    @GetMapping("/logs")
    public PageResponse<CallLog> logs(@RequestParam(required = false) String service,
                                      @RequestParam(required = false) String status,
                                      @RequestParam(required = false) String from,
                                      @RequestParam(required = false) String to,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "" + LogQuery.DEFAULT_PAGE_SIZE) int size) {
        LogQuery query = new LogQuery(
                blankToNull(service),
                isBlank(status) ? null : CallStatus.parse(status),
                parseInstant("from", from, false),
                parseInstant("to", to, true),
                page,
                size);
        return metricsService.searchLogs(query);
    }

    @GetMapping("/logs/{requestId}")
    public CallLog logDetail(@PathVariable String requestId) {
        return repository.findByRequestId(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Log entry", requestId));
    }

    @PostMapping("/simulate-load")
    public LoadSimulationResult simulateLoad(@RequestParam(required = false) Integer calls) {
        return loadSimulator.simulate(calls != null ? calls : loadSimulator.defaultCalls());
    }

    /**
     * Accepts ISO-8601 instants ({@code 2025-01-31T10:15:30Z}), offset date-times, local date-times
     * (interpreted as UTC) and plain dates. A plain date used as an upper bound covers the whole day.
     */
    private static Instant parseInstant(String name, String value, boolean endOfRange) {
        if (isBlank(value)) {
            return null;
        }
        String text = value.trim();
        try {
            return OffsetDateTime.parse(text).toInstant();
        } catch (DateTimeParseException ignored) {
            // Try the next supported format.
        }
        try {
            return LocalDateTime.parse(text).toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException ignored) {
            // Try the next supported format.
        }
        try {
            LocalDate date = LocalDate.parse(text);
            return endOfRange
                    ? date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).minusMillis(1)
                    : date.atStartOfDay().toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(
                    "Parameter '%s' must be an ISO-8601 date or date-time, for example 2025-01-31T10:15:30Z".formatted(name));
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }
}
