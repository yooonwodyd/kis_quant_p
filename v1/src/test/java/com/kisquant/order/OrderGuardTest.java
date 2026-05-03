package com.kisquant.order;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class OrderGuardTest {

	private final OrderGuard guard = new OrderGuard("001510", BigDecimal.valueOf(30_000), 1);

	@Test
	void rejectsOtherSymbols() {
		assertThatThrownBy(() -> this.guard.validateSymbol("005930"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("001510");
	}

	@Test
	void rejectsQuantityOtherThanOneShare() {
		assertThatThrownBy(() -> this.guard.validateMarketOrder("001510", 2))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("1");
	}
}
