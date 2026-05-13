package com.kisquant.strategylog.domain;

import com.kisquant.shared.domain.StrategyId;
import java.time.Instant;
import java.util.Objects;

public record StrategyLog(
        StrategyId strategyId,
        LogLevel level,
        String message,
        String payloadJson,
        Instant createdAt
) {

    public StrategyLog {
        Objects.requireNonNull(strategyId, "strategyId must not be null");
        Objects.requireNonNull(level, "level must not be null");
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("strategy log message must not be blank");
        }
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static StrategyLog create(
            StrategyId strategyId,
            LogLevel level,
            String message,
            String payloadJson,
            Instant createdAt
    ) {
        return new StrategyLog(strategyId, level, message, payloadJson, createdAt);
    }
}
