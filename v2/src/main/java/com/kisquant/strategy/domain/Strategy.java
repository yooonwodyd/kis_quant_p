package com.kisquant.strategy.domain;

import java.util.Objects;

import com.kisquant.shared.domain.Money;
import com.kisquant.shared.domain.StrategyId;
import com.kisquant.shared.domain.TradeMode;

/**
 * 전략 실행 상태와 주문 한도.
 */
public final class Strategy {

	private final StrategyId id;
	private final String name;
	private TradeMode tradeMode;
	private Money initialBudget;
	private final Money maxOrderAmount;
	private final Money maxDailyOrderAmount;
	private boolean enabled;

	private Strategy(
			StrategyId id,
			String name,
			TradeMode tradeMode,
			Money initialBudget,
			Money maxOrderAmount,
			Money maxDailyOrderAmount,
			boolean enabled
	) {
		this.id = Objects.requireNonNull(id, "id must not be null");
		this.name = validateName(name);
		this.tradeMode = Objects.requireNonNull(tradeMode, "tradeMode must not be null");
		this.initialBudget = requirePositive(initialBudget, "initialBudget");
		this.maxOrderAmount = requirePositive(maxOrderAmount, "maxOrderAmount");
		this.maxDailyOrderAmount = requirePositive(maxDailyOrderAmount, "maxDailyOrderAmount");
		if (this.maxOrderAmount.isGreaterThan(this.maxDailyOrderAmount)) {
			throw new IllegalArgumentException("max order amount cannot exceed max daily order amount");
		}
		if (this.maxOrderAmount.isGreaterThan(this.initialBudget)) {
			throw new IllegalArgumentException("max order amount cannot exceed initial budget");
		}
		this.enabled = enabled;
	}

	public static Strategy create(
			StrategyId id,
			String name,
			TradeMode tradeMode,
			Money initialBudget,
			Money maxOrderAmount,
			Money maxDailyOrderAmount
	) {
		return new Strategy(id, name, tradeMode, initialBudget, maxOrderAmount, maxDailyOrderAmount, true);
	}

	public void deactivate() {
		this.enabled = false;
	}

	public boolean canPlaceNewOrder() {
		return this.enabled;
	}

	public void changeTradeMode(TradeMode tradeMode) {
		this.tradeMode = Objects.requireNonNull(tradeMode, "tradeMode must not be null");
	}

	public void validateOrderAmount(Money orderAmount, Money currentPositionAmount) {
		Money targetOrderAmount = requirePositive(orderAmount, "orderAmount");
		Money currentAmount = Objects.requireNonNull(currentPositionAmount, "currentPositionAmount must not be null");
		if (targetOrderAmount.isGreaterThan(this.maxOrderAmount)) {
			throw new IllegalArgumentException("order amount exceeds max order amount");
		}
		if (currentAmount.plus(targetOrderAmount).isGreaterThan(this.initialBudget)) {
			throw new IllegalArgumentException("order amount exceeds strategy budget");
		}
	}

	public StrategyId id() {
		return this.id;
	}

	public String name() {
		return this.name;
	}

	public TradeMode tradeMode() {
		return this.tradeMode;
	}

	public Money initialBudget() {
		return this.initialBudget;
	}

	public Money maxOrderAmount() {
		return this.maxOrderAmount;
	}

	public Money maxDailyOrderAmount() {
		return this.maxDailyOrderAmount;
	}

	public boolean enabled() {
		return this.enabled;
	}

	private static String validateName(String name) {
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("strategy name must not be blank");
		}
		return name;
	}

	private static Money requirePositive(Money money, String fieldName) {
		Money target = Objects.requireNonNull(money, fieldName + " must not be null");
		if (!target.isPositive()) {
			throw new IllegalArgumentException(fieldName + " must be positive");
		}
		return target;
	}
}
