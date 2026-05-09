package com.kisquant.order.domain;

import java.util.Objects;

import com.kisquant.shared.domain.Money;
import com.kisquant.shared.domain.Symbol;
import com.kisquant.strategy.domain.Strategy;

/**
 * 주문을 만들기 전 확인할 운영 규칙.
 */
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
		OrderSide targetSide = Objects.requireNonNull(side, "side must not be null");
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
		if (targetSide == OrderSide.SELL) {
			return;
		}
		targetStrategy.validateOrderAmount(orderAmount, currentPositionAmount);
	}
}
