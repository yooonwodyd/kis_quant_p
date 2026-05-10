package com.kisquant.position.domain;

import java.util.Objects;

import com.kisquant.execution.domain.Execution;
import com.kisquant.order.domain.OrderSide;
import com.kisquant.shared.domain.Money;
import com.kisquant.shared.domain.StrategyId;
import com.kisquant.shared.domain.Symbol;

/**
 * 전략 기준 보유 수량과 평균단가.
 */
public final class StrategyPosition {

	private final StrategyId strategyId;
	private final Symbol symbol;
	private long quantity;
	private Money avgPrice;
	private Money realizedPnl;

	private StrategyPosition(StrategyId strategyId, Symbol symbol, long quantity, Money avgPrice, Money realizedPnl) {
		this.strategyId = Objects.requireNonNull(strategyId, "strategyId must not be null");
		this.symbol = Objects.requireNonNull(symbol, "symbol must not be null");
		if (quantity < 0L) {
			throw new IllegalArgumentException("quantity must not be negative");
		}
		this.quantity = quantity;
		this.avgPrice = Objects.requireNonNull(avgPrice, "avgPrice must not be null");
		this.realizedPnl = Objects.requireNonNull(realizedPnl, "realizedPnl must not be null");
	}

	public static StrategyPosition empty(StrategyId strategyId, Symbol symbol) {
		return new StrategyPosition(strategyId, symbol, 0L, Money.ZERO, Money.ZERO);
	}

	public static StrategyPosition restore(
			StrategyId strategyId,
			Symbol symbol,
			long quantity,
			Money avgPrice,
			Money realizedPnl
	) {
		return new StrategyPosition(strategyId, symbol, quantity, avgPrice, realizedPnl);
	}

	public void applyExecution(Execution execution) {
		Execution targetExecution = Objects.requireNonNull(execution, "execution must not be null");
		validateSamePosition(targetExecution);
		if (targetExecution.side() == OrderSide.BUY) {
			applyBuy(targetExecution);
			return;
		}
		applySell(targetExecution);
	}

	public Money acquisitionAmount() {
		return this.avgPrice.multiply(this.quantity);
	}

	public StrategyId strategyId() {
		return this.strategyId;
	}

	public Symbol symbol() {
		return this.symbol;
	}

	public long quantity() {
		return this.quantity;
	}

	public Money avgPrice() {
		return this.avgPrice;
	}

	public Money realizedPnl() {
		return this.realizedPnl;
	}

	private void applyBuy(Execution execution) {
		long newQuantity = Math.addExact(this.quantity, execution.executedQuantity());
		Money newAmount = acquisitionAmount().plus(execution.executedAmount());
		this.quantity = newQuantity;
		this.avgPrice = Money.won(newAmount.amount() / newQuantity);
	}

	private void applySell(Execution execution) {
		if (execution.executedQuantity() > this.quantity) {
			throw new IllegalArgumentException("sell quantity exceeds position quantity");
		}
		Money unitProfit = execution.executedPrice().minus(this.avgPrice);
		this.realizedPnl = this.realizedPnl.plus(unitProfit.multiply(execution.executedQuantity()));
		this.quantity = Math.subtractExact(this.quantity, execution.executedQuantity());
		if (this.quantity == 0L) {
			this.avgPrice = Money.ZERO;
		}
	}

	private void validateSamePosition(Execution execution) {
		if (!this.strategyId.equals(execution.strategyId()) || !this.symbol.equals(execution.symbol())) {
			throw new IllegalArgumentException("execution is not for this position");
		}
	}
}
