package com.observability.monitor.metrics;

import com.observability.monitor.logging.CallLog;
import com.observability.monitor.logging.CallStatus;

import java.time.Instant;

public record TimelinePoint(String requestId, String operation, CallStatus status, long durationMs, Instant timestamp) {

    public static TimelinePoint from(CallLog log) {
        return new TimelinePoint(log.requestId(), log.operation(), log.status(), log.durationMs(), log.timestamp());
    }
}
