package com.kisquant.strategy.adapter.web;

import java.time.Instant;

public record StrategyLogResponse(
        long strategyId,
        String level,
        String message,
        String payloadJson,
        Instant createdAt
) {
}
