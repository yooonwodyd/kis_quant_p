package com.kisquant.strategy.application;

import com.kisquant.shared.domain.Money;
import com.kisquant.shared.domain.TradeMode;

public record CreateStrategyCommand(
        String name,
        TradeMode tradeMode,
        Money initialBudget,
        Money maxOrderAmount,
        Money maxDailyOrderAmount
) {
}
