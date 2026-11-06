package com.observability.monitor.logging;

import com.observability.monitor.config.MonitorProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-safe, bounded, in-memory store for call logs. Entries are kept newest first and the oldest
 * entries are evicted once the configured capacity is reached.
 */
@Repository
public class CallLogRepository {

    private final Deque<CallLog> entries = new ArrayDeque<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final int capacity;

    @Autowired
    public CallLogRepository(MonitorProperties properties) {
        this(properties.logs().capacity());
    }

    public CallLogRepository(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be greater than zero");
        }
        this.capacity = capacity;
    }

    public void save(CallLog callLog) {
        Objects.requireNonNull(callLog, "callLog is required");
        lock.writeLock().lock();
        try {
            entries.addFirst(callLog);
            while (entries.size() > capacity) {
                entries.removeLast();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Returns a snapshot of every stored entry, newest first.
     */
    public List<CallLog> findAll() {
        lock.readLock().lock();
        try {
            return List.copyOf(entries);
        } finally {
            lock.readLock().unlock();
        }
    }

    public Optional<CallLog> findByRequestId(String requestId) {
        lock.readLock().lock();
        try {
            return entries.stream().filter(entry -> entry.requestId().equals(requestId)).findFirst();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns the latest {@code limit} entries of a service ordered chronologically (oldest first).
     */
    public List<CallLog> findLatestByService(String serviceId, int limit) {
        List<CallLog> latest = new ArrayList<>(limit);
        lock.readLock().lock();
        try {
            Iterator<CallLog> newestFirst = entries.iterator();
            while (newestFirst.hasNext() && latest.size() < limit) {
                CallLog entry = newestFirst.next();
                if (entry.serviceId().equals(serviceId)) {
                    latest.add(entry);
                }
            }
        } finally {
            lock.readLock().unlock();
        }
        return latest.reversed();
    }

    public int count() {
        lock.readLock().lock();
        try {
            return entries.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    public void clear() {
        lock.writeLock().lock();
        try {
            entries.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }
}
