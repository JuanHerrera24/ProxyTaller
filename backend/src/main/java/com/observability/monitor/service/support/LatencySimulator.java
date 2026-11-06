package com.observability.monitor.service.support;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Emulates the network and processing latency of a remote service so that the collected response times
 * resemble those of a distributed system.
 */
public final class LatencySimulator {

    private final int minMillis;
    private final int maxMillis;

    public LatencySimulator(int minMillis, int maxMillis) {
        if (minMillis < 0 || maxMillis < minMillis) {
            throw new IllegalArgumentException("Invalid latency range [%d, %d]".formatted(minMillis, maxMillis));
        }
        this.minMillis = minMillis;
        this.maxMillis = maxMillis;
    }

    public static LatencySimulator none() {
        return new LatencySimulator(0, 0);
    }

    public void pause() {
        if (maxMillis == 0) {
            return;
        }
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(minMillis, maxMillis + 1));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
