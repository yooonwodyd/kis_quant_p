package com.kisquant.strategy.domain;

import com.kisquant.shared.domain.Money;
import com.kisquant.shared.domain.StrategyId;
import com.kisquant.shared.domain.TradeMode;
import java.util.Objects;

/*

 */
public final class Strategy {

    private final StrategyId id;
    private final String name;
    private TradeMode tradeMode;
    private Money initialBudget;
    private Money maxOrderAmount;
    private Money maxDailyOrderAmount;
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
        if (maxOrderAmount.isGreaterThan(maxDailyOrderAmount)) {
            throw new IllegalArgumentException("daily order amount must be greater than or equal to max order amount");
        }
        if (maxOrderAmount.isGreaterThan(initialBudget)) {
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

    public static Strategy restore(
            StrategyId id,
            String name,
            TradeMode tradeMode,
            Money initialBudget,
            Money maxOrderAmount,
            Money maxDailyOrderAmount,
            boolean enabled
    ) {
        return new Strategy(id, name, tradeMode, initialBudget, maxOrderAmount, maxDailyOrderAmount, enabled);
    }

    public void activate() {
        enabled = true;
    }

    public void deactivate() {
        enabled = false;
    }

    public boolean canPlaceNewOrder() {
        return enabled;
    }

    public void changeTradeMode(TradeMode tradeMode) {
        this.tradeMode = Objects.requireNonNull(tradeMode, "tradeMode must not be null");
    }

    public void changeInitialBudget(Money initialBudget) {
        Money newInitialBudget = requirePositive(initialBudget, "initialBudget");
        if (maxOrderAmount.isGreaterThan(newInitialBudget)) {
            throw new IllegalArgumentException("initial budget must be greater than or equal to max order amount");
        }
        this.initialBudget = newInitialBudget;
    }

    public void validateOrderAmount(Money orderAmount, Money currentPositionAmount) {
        Money positiveOrderAmount = requirePositive(orderAmount, "orderAmount");
        Money currentAmount = Objects.requireNonNull(currentPositionAmount, "currentPositionAmount must not be null");
        if (positiveOrderAmount.isGreaterThan(maxOrderAmount)) {
            throw new IllegalArgumentException("order amount exceeds max order amount");
        }
        if (currentAmount.plus(positiveOrderAmount).isGreaterThan(initialBudget)) {
            throw new IllegalArgumentException("order amount exceeds strategy budget");
        }
    }

    public StrategyId id() {
        return id;
    }

    public String name() {
        return name;
    }

    public TradeMode tradeMode() {
        return tradeMode;
    }

    public Money initialBudget() {
        return initialBudget;
    }

    public Money maxOrderAmount() {
        return maxOrderAmount;
    }

    public Money maxDailyOrderAmount() {
        return maxDailyOrderAmount;
    }

    public boolean enabled() {
        return enabled;
    }

    private static String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("strategy name must not be blank");
        }
        return name;
    }

    private static Money requirePositive(Money money, String fieldName) {
        Money value = Objects.requireNonNull(money, fieldName + " must not be null");
        if (!value.isPositive()) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }
}
