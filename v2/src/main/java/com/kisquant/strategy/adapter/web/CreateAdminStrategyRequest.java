package com.kisquant.strategy.adapter.web;

import com.kisquant.shared.domain.TradeMode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAdminStrategyRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull TradeMode tradeMode,
        @Min(1L) long initialBudgetAmount,
        @Min(1L) long maxOrderAmount,
        @Min(1L) long maxDailyOrderAmount
) {
}
