package com.kisquant.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class DomainIdTest {

	@Test
	void createsTypedIds() {
		assertThat(StrategyId.of(1L).value()).isEqualTo(1L);
		assertThat(OrderId.of(2L).value()).isEqualTo(2L);
		assertThat(ExecutionId.of(3L).value()).isEqualTo(3L);
	}

	@Test
	void rejectsNonPositiveIds() {
		assertThatThrownBy(() -> StrategyId.of(0L)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> OrderId.of(-1L)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> ExecutionId.of(0L)).isInstanceOf(IllegalArgumentException.class);
	}
}
