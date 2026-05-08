package com.kisquant.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MoneyTest {

	@Test
	void calculatesWonAmountsWithoutUsingPrimitiveLongsEverywhere() {
		Money base = Money.won(10_000L);

		assertThat(base.plus(Money.won(5_000L))).isEqualTo(Money.won(15_000L));
		assertThat(base.minus(Money.won(3_000L))).isEqualTo(Money.won(7_000L));
		assertThat(base.multiply(3L)).isEqualTo(Money.won(30_000L));
	}

	@Test
	void detectsPositiveAmounts() {
		assertThat(Money.won(1L).isPositive()).isTrue();
		assertThat(Money.ZERO.isPositive()).isFalse();
	}

	@Test
	void failsOnArithmeticOverflow() {
		Money max = Money.won(Long.MAX_VALUE);

		assertThatThrownBy(() -> max.plus(Money.won(1L)))
				.isInstanceOf(ArithmeticException.class);
	}
}
