package com.kisquant.order.application;

import com.kisquant.order.domain.OrderSide;
import com.kisquant.order.domain.OrderType;
import com.kisquant.shared.domain.Money;
import com.kisquant.shared.domain.StrategyId;
import com.kisquant.shared.domain.Symbol;

public record PlaceOrderCommand(
        StrategyId strategyId,
        Symbol symbol,
        OrderSide side,
        OrderType orderType,
        long quantity,
        Money orderPrice
) {
}
