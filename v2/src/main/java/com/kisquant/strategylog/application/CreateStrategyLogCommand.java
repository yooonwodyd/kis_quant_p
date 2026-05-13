package com.kisquant.strategylog.application;

import com.kisquant.shared.domain.StrategyId;
import com.kisquant.strategylog.domain.LogLevel;

public record CreateStrategyLogCommand(
        StrategyId strategyId,
        LogLevel level,
        String message,
        String payloadJson
) {
}
