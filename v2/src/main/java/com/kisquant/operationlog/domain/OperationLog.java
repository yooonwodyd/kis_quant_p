package com.kisquant.operationlog.domain;

import java.time.Instant;

public record OperationLog(
        long id,
        OperationLogLevel level,
        OperationLogCategory category,
        String message,
        String payloadJson,
        Instant createdAt
) {

    public OperationLog {
        if (id < 1L) {
            throw new IllegalArgumentException("operation log id must be positive");
        }
        if (level == null) {
            throw new IllegalArgumentException("operation log level is required");
        }
        if (category == null) {
            throw new IllegalArgumentException("operation log category is required");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("operation log message is required");
        }
        if (payloadJson == null || payloadJson.isBlank()) {
            throw new IllegalArgumentException("operation log payload is required");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("operation log createdAt is required");
        }
    }
}
