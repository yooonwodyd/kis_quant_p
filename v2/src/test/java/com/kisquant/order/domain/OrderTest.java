package com.kisquant.order.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kisquant.shared.domain.Money;
import com.kisquant.shared.domain.OrderId;
import com.kisquant.shared.domain.StrategyId;
import com.kisquant.shared.domain.Symbol;
import com.kisquant.shared.domain.TradeMode;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class OrderTest {

    @Test
    void liveOrderKeepsKisIdentifiersAfterAccepted() {
        Order order = requestedLiveLimitOrder();

        order.accept(KisOrderNumber.of("1234567890"), KisOrderOrgNumber.of("06010"), Instant.parse("2026-05-06T01:00:00Z"));

        assertThat(order.status()).isEqualTo(OrderStatus.ACCEPTED);
        assertThat(order.kisOrderNumber()).hasValue(KisOrderNumber.of("1234567890"));
        assertThat(order.isPollingTarget()).isTrue();
    }

    @Test
    void simulationOrderCannotHaveKisIdentifiers() {
        Order order = Order.requested(
                OrderId.of(1L),
                StrategyId.of(1L),
                Symbol.of("001510"),
                OrderSide.BUY,
                TradeMode.SIMULATION,
                OrderType.LIMIT,
                OrderQuantity.of(1L),
                Money.won(1_000L),
                Instant.parse("2026-05-06T00:00:00Z"));

        assertThatThrownBy(() -> order.accept(KisOrderNumber.of("1234567890"), KisOrderOrgNumber.of("06010"), Instant.now()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void unknownOrderIsPollingTargetButCannotBeAcceptedAgain() {
        Order order = requestedLiveLimitOrder();

        order.markUnknown("timeout");

        assertThat(order.status()).isEqualTo(OrderStatus.UNKNOWN);
        assertThat(order.isPollingTarget()).isTrue();
        assertThatThrownBy(() -> order.accept(KisOrderNumber.of("1234567890"), KisOrderOrgNumber.of("06010"), Instant.now()))
                .isInstanceOf(IllegalStateException.class);
    }

    private Order requestedLiveLimitOrder() {
        return Order.requested(
                OrderId.of(1L),
                StrategyId.of(1L),
                Symbol.of("001510"),
                OrderSide.BUY,
                TradeMode.LIVE,
                OrderType.LIMIT,
                OrderQuantity.of(1L),
                Money.won(1_000L),
                Instant.parse("2026-05-06T00:00:00Z"));
    }
}
