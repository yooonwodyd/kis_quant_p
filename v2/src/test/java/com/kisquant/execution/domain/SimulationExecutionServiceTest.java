package com.kisquant.execution.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kisquant.order.domain.Order;
import com.kisquant.order.domain.OrderQuantity;
import com.kisquant.order.domain.OrderSide;
import com.kisquant.order.domain.OrderType;
import com.kisquant.shared.domain.Money;
import com.kisquant.shared.domain.OrderId;
import com.kisquant.shared.domain.StrategyId;
import com.kisquant.shared.domain.Symbol;
import com.kisquant.shared.domain.TradeMode;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class SimulationExecutionServiceTest {

    private final SimulationExecutionService service = new SimulationExecutionService();

    @Test
    void simulationOrderIsFilledAtOrderPrice() {
        Order order = Order.requested(
                OrderId.of(1L),
                StrategyId.of(1L),
                Symbol.of("001510"),
                OrderSide.BUY,
                TradeMode.SIMULATION,
                OrderType.LIMIT,
                OrderQuantity.of(2L),
                Money.won(1_000L),
                Instant.parse("2026-05-06T00:00:00Z"));

        Execution execution = service.execute(order, ExecutionId.of(1L), Instant.parse("2026-05-06T01:00:00Z"));

        assertThat(execution.source()).isEqualTo(ExecutionSource.SIMULATION);
        assertThat(execution.executedQuantity()).isEqualTo(2L);
        assertThat(execution.executedPrice()).isEqualTo(Money.won(1_000L));
        assertThat(order.isFilled()).isTrue();
    }

    @Test
    void liveOrderCannotBeExecutedBySimulationService() {
        Order order = Order.requested(
                OrderId.of(1L),
                StrategyId.of(1L),
                Symbol.of("001510"),
                OrderSide.BUY,
                TradeMode.LIVE,
                OrderType.LIMIT,
                OrderQuantity.of(1L),
                Money.won(1_000L),
                Instant.parse("2026-05-06T00:00:00Z"));

        assertThatThrownBy(() -> service.execute(order, ExecutionId.of(1L), Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
