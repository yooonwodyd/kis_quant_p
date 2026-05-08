package com.kisquant.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SymbolTest {

	@Test
	void createsDomesticStockSymbol() {
		Symbol symbol = Symbol.of("001510");

		assertThat(symbol.value()).isEqualTo("001510");
	}

	@Test
	void rejectsBlankOrNonSixDigitSymbol() {
		assertThatThrownBy(() -> Symbol.of("")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Symbol.of("1510")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Symbol.of("ABCDEF")).isInstanceOf(IllegalArgumentException.class);
	}
}
