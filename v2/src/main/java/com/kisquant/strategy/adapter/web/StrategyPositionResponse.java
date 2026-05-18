package com.kisquant.strategy.adapter.web;

public record StrategyPositionResponse(
        long strategyId,
        String symbol,
        long quantity,
        long avgPrice,
        long realizedPnl
) {
}
