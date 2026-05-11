package com.kisquant.position.domain;

import com.kisquant.execution.domain.Execution;
import com.kisquant.order.domain.OrderSide;
import com.kisquant.shared.domain.Money;
import com.kisquant.shared.domain.StrategyId;
import com.kisquant.shared.domain.Symbol;
import java.util.Objects;

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
            throw new IllegalArgumentException("position quantity cannot be negative");
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
        if (!strategyId.equals(targetExecution.strategyId()) || !symbol.equals(targetExecution.symbol())) {
            throw new IllegalArgumentException("execution target does not match position");
        }
        if (targetExecution.side() == OrderSide.BUY) {
            applyBuy(targetExecution);
            return;
        }
        applySell(targetExecution);
    }

    public StrategyId strategyId() {
        return strategyId;
    }

    public Symbol symbol() {
        return symbol;
    }

    public long quantity() {
        return quantity;
    }

    public Money avgPrice() {
        return avgPrice;
    }

    public Money realizedPnl() {
        return realizedPnl;
    }

    public Money acquisitionAmount() {
        return avgPrice.multiply(quantity);
    }

    private void applyBuy(Execution execution) {
        long newQuantity = Math.addExact(quantity, execution.executedQuantity());
        long currentAmount = Math.multiplyExact(quantity, avgPrice.amount());
        long executionAmount = execution.executedAmount().amount();
        quantity = newQuantity;
        avgPrice = Money.won(Math.floorDiv(Math.addExact(currentAmount, executionAmount), newQuantity));
    }

    private void applySell(Execution execution) {
        if (execution.executedQuantity() > quantity) {
            throw new IllegalArgumentException("sell quantity exceeds position quantity");
        }
        Money pnl = execution.executedPrice().minus(avgPrice).multiply(execution.executedQuantity());
        realizedPnl = realizedPnl.plus(pnl);
        quantity = Math.subtractExact(quantity, execution.executedQuantity());
        if (quantity == 0L) {
            avgPrice = Money.ZERO;
        }
    }
}
