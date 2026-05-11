package com.kisquant.position.domain;

import static org.assertj.core.api.Assertions.assertThat;

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

class PositionUpdateServiceTest {

    private final PositionUpdateService service = new PositionUpdateService();

    @Test
    void liveAndSimulationExecutionsUseSamePositionRule() {
        StrategyPosition position = StrategyPosition.empty(StrategyId.of(1L), Symbol.of("001510"));

        service.apply(position, execution(ExecutionSource.LIVE, OrderSide.BUY, 1L, 1_000L));
        service.apply(position, execution(ExecutionSource.SIMULATION, OrderSide.BUY, 1L, 1_200L));

        assertThat(position.quantity()).isEqualTo(2L);
        assertThat(position.avgPrice()).isEqualTo(Money.won(1_100L));
    }

    private Execution execution(ExecutionSource source, OrderSide side, long quantity, long price) {
        return Execution.create(
                ExecutionId.of(source == ExecutionSource.LIVE ? 1L : 2L),
                OrderId.of(source == ExecutionSource.LIVE ? 1L : 2L),
                StrategyId.of(1L),
                Symbol.of("001510"),
                side,
                source,
                quantity,
                Money.won(price),
                Instant.parse("2026-05-06T01:00:00Z"),
                ExecutionDedupKey.of(source + "-" + side + "-" + quantity + "-" + price));
    }
}
