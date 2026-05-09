package com.kisquant.order.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kisquant.shared.domain.Money;
import com.kisquant.shared.domain.StrategyId;
import com.kisquant.shared.domain.Symbol;
import com.kisquant.shared.domain.TradeMode;
import com.kisquant.strategy.domain.Strategy;
import org.junit.jupiter.api.Test;

class OrderEligibilityServiceTest {

	private final OrderEligibilityService service = new OrderEligibilityService();
	private final AllowedSymbolPolicy allowed001510 = symbol -> symbol.equals(Symbol.of("001510"));

	@Test
	void acceptsAllowedBuyOrderInsideBudget() {
		assertThatCode(() -> this.service.validate(
				strategy(),
				Symbol.of("001510"),
				OrderSide.BUY,
				Money.won(10_000L),
				Money.won(20_000L),
				false,
				this.allowed001510
		)).doesNotThrowAnyException();
	}

	@Test
	void rejectsInactiveStrategyOrDisallowedSymbol() {
		Strategy inactive = strategy();
		inactive.deactivate();

		assertThatThrownBy(() -> this.service.validate(
				inactive, Symbol.of("001510"), OrderSide.BUY, Money.won(10_000L), Money.ZERO, false, this.allowed001510
		)).isInstanceOf(IllegalStateException.class);

		assertThatThrownBy(() -> this.service.validate(
				strategy(), Symbol.of("005930"), OrderSide.BUY, Money.won(10_000L), Money.ZERO, false, this.allowed001510
		)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void rejectsOpenOrderAndBuyAmountOverBudget() {
		assertThatThrownBy(() -> this.service.validate(
				strategy(), Symbol.of("001510"), OrderSide.BUY, Money.won(10_000L), Money.ZERO, true, this.allowed001510
		)).isInstanceOf(IllegalStateException.class);

		assertThatThrownBy(() -> this.service.validate(
				strategy(), Symbol.of("001510"), OrderSide.BUY, Money.won(11_000L), Money.ZERO, false, this.allowed001510
		)).isInstanceOf(IllegalArgumentException.class);
	}

	private Strategy strategy() {
		return Strategy.create(
				StrategyId.of(1L),
				"수동 실전 투자 전략",
				TradeMode.LIVE,
				Money.won(30_000L),
				Money.won(10_000L),
				Money.won(30_000L)
		);
	}
}
