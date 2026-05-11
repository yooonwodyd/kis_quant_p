package com.kisquant.strategy.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kisquant.shared.domain.Money;
import com.kisquant.shared.domain.StrategyId;
import com.kisquant.shared.domain.TradeMode;
import org.junit.jupiter.api.Test;

class StrategyTest {

    @Test
    void activeStrategyAllowsNewOrders() {
        Strategy strategy = Strategy.create(
                StrategyId.of(1L),
                "수동 주문 테스트 전략",
                TradeMode.LIVE,
                Money.won(30_000L),
                Money.won(30_000L),
                Money.won(30_000L));

        assertThat(strategy.canPlaceNewOrder()).isTrue();
    }

    @Test
    void inactiveStrategyRejectsNewOrders() {
        Strategy strategy = Strategy.create(
                StrategyId.of(1L),
                "수동 주문 테스트 전략",
                TradeMode.SIMULATION,
                Money.won(30_000L),
                Money.won(30_000L),
                Money.won(30_000L));

        strategy.deactivate();

        assertThat(strategy.canPlaceNewOrder()).isFalse();
    }

    @Test
    void dailyLimitMustBeGreaterThanOrEqualToSingleOrderLimit() {
        assertThatThrownBy(() -> Strategy.create(
                        StrategyId.of(1L),
                        "수동 주문 테스트 전략",
                        TradeMode.LIVE,
                        Money.won(30_000L),
                        Money.won(30_000L),
                        Money.won(10_000L)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void initialBudgetCanBeChangedWhenItStillCoversOrderLimits() {
        Strategy strategy = Strategy.create(
                StrategyId.of(1L),
                "수동 주문 테스트 전략",
                TradeMode.SIMULATION,
                Money.won(30_000L),
                Money.won(10_000L),
                Money.won(30_000L));

        strategy.changeInitialBudget(Money.won(40_000L));

        assertThat(strategy.initialBudget()).isEqualTo(Money.won(40_000L));
    }

    @Test
    void initialBudgetCannotBeLowerThanSingleOrderLimit() {
        Strategy strategy = Strategy.create(
                StrategyId.of(1L),
                "수동 주문 테스트 전략",
                TradeMode.SIMULATION,
                Money.won(30_000L),
                Money.won(10_000L),
                Money.won(30_000L));

        assertThatThrownBy(() -> strategy.changeInitialBudget(Money.won(9_000L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("initial budget");
    }
}
