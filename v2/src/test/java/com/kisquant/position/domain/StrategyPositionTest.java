package com.kisquant.position.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kisquant.execution.domain.Execution;
import com.kisquant.execution.domain.ExecutionDedupKey;
import com.kisquant.execution.domain.ExecutionId;
import com.kisquant.execution.domain.ExecutionSource;
import com.kisquant.order.domain.OrderSide;
import com.kisquant.shared.domain.Money;
import com.kisquant.shared.domain.OrderId;
import com.kisquant.shared.domain.StrategyId;
import com.kisquant.shared.domain.Symbol;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class StrategyPositionTest {

    @Test
    void buyExecutionsIncreaseQuantityAndRecalculateAveragePrice() {
        StrategyPosition position = StrategyPosition.empty(StrategyId.of(1L), Symbol.of("001510"));

        position.applyExecution(execution(OrderSide.BUY, 2L, 1_000L));
        position.applyExecution(execution(OrderSide.BUY, 1L, 1_300L));

        assertThat(position.quantity()).isEqualTo(3L);
        assertThat(position.avgPrice()).isEqualTo(Money.won(1_100L));
    }

    @Test
    void sellExecutionsDecreaseQuantityAndIncreaseRealizedProfit() {
        StrategyPosition position = StrategyPosition.empty(StrategyId.of(1L), Symbol.of("001510"));
        position.applyExecution(execution(OrderSide.BUY, 2L, 1_000L));

        position.applyExecution(execution(OrderSide.SELL, 1L, 1_200L));

        assertThat(position.quantity()).isEqualTo(1L);
        assertThat(position.realizedPnl()).isEqualTo(Money.won(200L));
    }

    @Test
    void sellExecutionCannotMakeNegativeQuantity() {
        StrategyPosition position = StrategyPosition.empty(StrategyId.of(1L), Symbol.of("001510"));

        assertThatThrownBy(() -> position.applyExecution(execution(OrderSide.SELL, 1L, 1_200L)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private Execution execution(OrderSide side, long quantity, long price) {
        return Execution.create(
                ExecutionId.of(1L),
                OrderId.of(1L),
                StrategyId.of(1L),
                Symbol.of("001510"),
                side,
                ExecutionSource.LIVE,
                quantity,
                Money.won(price),
                Instant.parse("2026-05-06T01:00:00Z"),
                ExecutionDedupKey.of("dedup-" + side + "-" + quantity + "-" + price));
    }
}
