package com.observability.monitor.metrics;

import com.observability.monitor.logging.CallLog;
import com.observability.monitor.logging.CallStatus;

import java.time.Instant;
import java.util.function.Predicate;

/**
 * Filter criteria for the log search. Every criterion is optional; the time range is inclusive.
 */
public record LogQuery(String serviceId, CallStatus status, Instant from, Instant to, int page, int size) {

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    public LogQuery {
        if (page < 0) {
            throw new IllegalArgumentException("page must be zero or greater");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_PAGE_SIZE);
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("'from' must be earlier than or equal to 'to'");
        }
    }

    public Predicate<CallLog> asPredicate() {
        return log -> (serviceId == null || serviceId.equals(log.serviceId()))
                && (status == null || status == log.status())
                && (from == null || !log.timestamp().isBefore(from))
                && (to == null || !log.timestamp().isAfter(to));
    }
}
