package com.kisquant.operationlog.application;

import com.kisquant.operationlog.domain.OperationLog;
import com.kisquant.operationlog.domain.OperationLogCategory;
import com.kisquant.operationlog.domain.OperationLogLevel;
import com.kisquant.shared.config.KisQuantProperties;
import com.kisquant.shared.time.CurrentTimeProvider;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OperationLogBuffer {

    private final CurrentTimeProvider timeProvider;
    private final int capacity;
    private final AtomicLong sequence = new AtomicLong();
    private final Deque<OperationLog> logs = new ArrayDeque<>();

    @Autowired
    public OperationLogBuffer(CurrentTimeProvider timeProvider, KisQuantProperties properties) {
        this(timeProvider, properties.operationLogs().capacity());
    }

    public OperationLogBuffer(CurrentTimeProvider timeProvider, int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("operation log capacity must be positive");
        }
        this.timeProvider = timeProvider;
        this.capacity = capacity;
    }

    public synchronized OperationLog append(
            OperationLogLevel level,
            OperationLogCategory category,
            String message,
            String payloadJson
    ) {
        OperationLog log = new OperationLog(
                sequence.incrementAndGet(),
                level,
                category,
                message,
                normalizePayload(payloadJson),
                timeProvider.now());
        logs.addFirst(log);
        while (logs.size() > capacity) {
            logs.removeLast();
        }
        return log;
    }

    public synchronized List<OperationLog> recent(OperationLogCategory category, int limit) {
        int normalizedLimit = Math.max(1, limit);
        return logs.stream()
                .filter(log -> category == null || log.category() == category)
                .limit(normalizedLimit)
                .toList();
    }

    private String normalizePayload(String payloadJson) {
        return payloadJson == null || payloadJson.isBlank() ? "{}" : payloadJson;
    }
}
