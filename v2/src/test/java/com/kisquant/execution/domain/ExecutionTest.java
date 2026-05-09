package com.kisquant.execution.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import com.kisquant.order.domain.OrderSide;
import com.kisquant.shared.domain.ExecutionId;
import com.kisquant.shared.domain.Money;
import com.kisquant.shared.domain.OrderId;
import com.kisquant.shared.domain.StrategyId;
import com.kisquant.shared.domain.Symbol;
import org.junit.jupiter.api.Test;

class ExecutionTest {

	@Test
	void createsExecutionAmountFromPriceAndQuantity() {
		Execution execution = Execution.create(
				ExecutionId.of(1L),
				OrderId.of(1L),
				StrategyId.of(1L),
				Symbol.of("001510"),
				OrderSide.BUY,
				ExecutionSource.LIVE,
				3L,
				Money.won(1_000L),
				Instant.parse("2026-05-10T00:30:00Z"),
				ExecutionDedupKey.of("LIVE-1")
		);

		assertThat(execution.executedAmount()).isEqualTo(Money.won(3_000L));
		assertThat(execution.source()).isEqualTo(ExecutionSource.LIVE);
	}

	@Test
	void rejectsInvalidExecutionValues() {
		assertThatThrownBy(() -> ExecutionDedupKey.of("")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Execution.create(
				ExecutionId.of(1L),
				OrderId.of(1L),
				StrategyId.of(1L),
				Symbol.of("001510"),
				OrderSide.BUY,
				ExecutionSource.LIVE,
				0L,
				Money.won(1_000L),
				Instant.parse("2026-05-10T00:30:00Z"),
				ExecutionDedupKey.of("LIVE-1")
		)).isInstanceOf(IllegalArgumentException.class);
	}
}
