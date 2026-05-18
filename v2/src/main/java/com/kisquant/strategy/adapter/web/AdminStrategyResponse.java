package com.kisquant.strategy.adapter.web;

public record AdminStrategyResponse(
        long id,
        String name,
        boolean enabled,
        String tradeMode,
        long initialBudgetAmount,
        long maxOrderAmount,
        long maxDailyOrderAmount
) {
}
