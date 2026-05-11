package com.kisquant.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kisquant.order.domain.OrderQuantity;
import org.junit.jupiter.api.Test;

class ValueObjectTest {

    @Test
    void symbolMustBeSixDigits() {
        assertThat(Symbol.of("001510").value()).isEqualTo("001510");

        assertThatThrownBy(() -> Symbol.of("SK"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void identifiersMustBePositive() {
        assertThat(StrategyId.of(1L).value()).isEqualTo(1L);
        assertThat(OrderId.of(1L).value()).isEqualTo(1L);

        assertThatThrownBy(() -> StrategyId.of(0L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> OrderId.of(-1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void orderQuantityMustBePositive() {
        assertThat(OrderQuantity.of(1L).value()).isEqualTo(1L);

        assertThatThrownBy(() -> OrderQuantity.of(0L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void moneySupportsProfitAndLossCalculations() {
        assertThat(Money.won(1_000L).minus(Money.won(1_200L))).isEqualTo(Money.won(-200L));
    }
}
