package com.kisquant.order.domain;

import com.kisquant.shared.domain.Money;
import com.kisquant.shared.domain.Symbol;
import com.kisquant.strategy.domain.Strategy;
import java.util.Objects;

public final class OrderEligibilityService {

    public void validate(
            Strategy strategy,
            Symbol symbol,
            OrderSide side,
            Money orderAmount,
            Money currentPositionAmount,
            boolean hasOpenOrder,
            AllowedSymbolPolicy allowedSymbolPolicy
    ) {
        Strategy targetStrategy = Objects.requireNonNull(strategy, "strategy must not be null");
        Symbol targetSymbol = Objects.requireNonNull(symbol, "symbol must not be null");
        AllowedSymbolPolicy policy = Objects.requireNonNull(allowedSymbolPolicy, "allowedSymbolPolicy must not be null");

        if (!targetStrategy.canPlaceNewOrder()) {
            throw new IllegalStateException("inactive strategy cannot place orders");
        }
        if (!policy.allows(targetSymbol)) {
            throw new IllegalArgumentException("symbol is not allowed");
        }
        if (hasOpenOrder) {
            throw new IllegalStateException("open order already exists");
        }
        if (side == OrderSide.SELL) {
            return;
        }
        targetStrategy.validateOrderAmount(orderAmount, currentPositionAmount);
    }
}
