package com.kisquant.strategy.adapter.web;

import com.kisquant.strategylog.domain.LogLevel;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateStrategyLogRequest(
        @Min(1L) long strategyId,
        @NotNull LogLevel level,
        @NotBlank String message,
        String payloadJson
) {
}
