package com.kisquant.execution.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import com.kisquant.order.domain.Order;
import com.kisquant.order.domain.OrderQuantity;
import com.kisquant.order.domain.OrderSide;
import com.kisquant.order.domain.OrderStatus;
import com.kisquant.order.domain.OrderType;
import com.kisquant.shared.domain.ExecutionId;
import com.kisquant.shared.domain.Money;
import com.kisquant.shared.domain.OrderId;
import com.kisquant.shared.domain.StrategyId;
import com.kisquant.shared.domain.Symbol;
import com.kisquant.shared.domain.TradeMode;
import org.junit.jupiter.api.Test;

class SimulationExecutionServiceTest {

	private final SimulationExecutionService service = new SimulationExecutionService();

	@Test
	void simulationOrderCreatesExecutionAndFillsOrder() {
		Order order = order(TradeMode.SIMULATION);
		Instant executedAt = Instant.parse("2026-05-10T09:52:00Z");

		Execution execution = this.service.execute(order, ExecutionId.of(1L), executedAt);

		assertThat(execution.source()).isEqualTo(ExecutionSource.SIMULATION);
		assertThat(execution.executedPrice()).isEqualTo(Money.won(1_000L));
		assertThat(execution.dedupKey()).isEqualTo(ExecutionDedupKey.of("SIMULATION-1"));
		assertThat(order.status()).isEqualTo(OrderStatus.FILLED);
	}

	@Test
	void liveOrderCannotBeExecutedBySimulationService() {
		Order order = order(TradeMode.LIVE);

		assertThatThrownBy(() -> this.service.execute(order, ExecutionId.of(1L), Instant.parse("2026-05-10T09:52:00Z")))
				.isInstanceOf(IllegalArgumentException.class);
	}

	private Order order(TradeMode tradeMode) {
		return Order.requested(
				OrderId.of(1L),
				StrategyId.of(1L),
				Symbol.of("001510"),
				OrderSide.BUY,
				tradeMode,
				OrderType.LIMIT,
				OrderQuantity.of(1L),
				Money.won(1_000L),
				Instant.parse("2026-05-10T09:50:00Z")
		);
	}
}
