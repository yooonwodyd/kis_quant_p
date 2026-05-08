package com.kisquant.strategy.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kisquant.shared.domain.Money;
import com.kisquant.shared.domain.StrategyId;
import com.kisquant.shared.domain.TradeMode;
import org.junit.jupiter.api.Test;

class StrategyTest {

	@Test
	void activeStrategyCanPlaceNewOrder() {
		Strategy strategy = Strategy.create(
				StrategyId.of(1L),
				"수동 모의 투자 전략",
				TradeMode.SIMULATION,
				Money.won(30_000L),
				Money.won(10_000L),
				Money.won(30_000L)
		);

		assertThat(strategy.canPlaceNewOrder()).isTrue();

		strategy.deactivate();

		assertThat(strategy.canPlaceNewOrder()).isFalse();
	}

	@Test
	void validatesOrderAmountWithStrategyBudget() {
		Strategy strategy = Strategy.create(
				StrategyId.of(1L),
				"수동 실전 투자 전략",
				TradeMode.LIVE,
				Money.won(30_000L),
				Money.won(10_000L),
				Money.won(30_000L)
		);

		strategy.validateOrderAmount(Money.won(10_000L), Money.won(20_000L));

		assertThatThrownBy(() -> strategy.validateOrderAmount(Money.won(11_000L), Money.ZERO))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> strategy.validateOrderAmount(Money.won(10_000L), Money.won(25_000L)))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void changesTradeMode() {
		Strategy strategy = Strategy.create(
				StrategyId.of(1L),
				"수동 모의 투자 전략",
				TradeMode.SIMULATION,
				Money.won(30_000L),
				Money.won(10_000L),
				Money.won(30_000L)
		);

		strategy.changeTradeMode(TradeMode.LIVE);

		assertThat(strategy.tradeMode()).isEqualTo(TradeMode.LIVE);
	}
}
