package com.kisquant.order.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kisquant.shared.domain.Money;
import com.kisquant.shared.domain.StrategyId;
import com.kisquant.shared.domain.Symbol;
import com.kisquant.shared.domain.TradeMode;
import com.kisquant.strategy.domain.Strategy;
import java.util.Set;
import org.junit.jupiter.api.Test;

class OrderEligibilityServiceTest {

    private final OrderEligibilityService service = new OrderEligibilityService();

    @Test
    void eligibleWhenStrategyIsActiveSymbolAllowedAndBudgetRemains() {
        Strategy strategy = activeStrategy();

        assertThatCode(() -> service.validate(
                        strategy,
                        Symbol.of("001510"),
                        OrderSide.BUY,
                        Money.won(10_000L),
                        Money.won(15_000L),
                        false,
                        AllowedSymbolPolicy.of(Set.of(Symbol.of("001510")))))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsWhenOpenOrderAlreadyExistsForStrategyAndSymbol() {
        assertThatThrownBy(() -> service.validate(
                        activeStrategy(),
                        Symbol.of("001510"),
                        OrderSide.BUY,
                        Money.won(10_000L),
                        Money.ZERO,
                        true,
                        AllowedSymbolPolicy.of(Set.of(Symbol.of("001510")))))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsWhenOrderWouldExceedStrategyBudget() {
        assertThatThrownBy(() -> service.validate(
                        activeStrategy(),
                        Symbol.of("001510"),
                        OrderSide.BUY,
                        Money.won(20_000L),
                        Money.won(15_000L),
                        false,
                        AllowedSymbolPolicy.of(Set.of(Symbol.of("001510")))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sellOrderDoesNotConsumeStrategyBudget() {
        assertThatCode(() -> service.validate(
                        activeStrategy(),
                        Symbol.of("001510"),
                        OrderSide.SELL,
                        Money.won(30_000L),
                        Money.won(30_000L),
                        false,
                        AllowedSymbolPolicy.of(Set.of(Symbol.of("001510")))))
                .doesNotThrowAnyException();
    }

    private Strategy activeStrategy() {
        return Strategy.create(
                StrategyId.of(1L),
                "수동 주문 테스트 전략",
                TradeMode.LIVE,
                Money.won(30_000L),
                Money.won(30_000L),
                Money.won(30_000L));
    }
}
