package com.kisquant.order.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import com.kisquant.shared.domain.Money;
import com.kisquant.shared.domain.OrderId;
import com.kisquant.shared.domain.StrategyId;
import com.kisquant.shared.domain.Symbol;
import com.kisquant.shared.domain.TradeMode;
import org.junit.jupiter.api.Test;

class OrderTest {

	@Test
	void liveOrderCanBeAcceptedWithKisIdentifiers() {
		Order order = liveOrder();

		order.accept(KisOrderNumber.of("0007103300"), KisOrderOrgNumber.of("06010"), Instant.parse("2026-05-09T02:00:01Z"));

		assertThat(order.status()).isEqualTo(OrderStatus.ACCEPTED);
		assertThat(order.kisOrderNumber()).contains(KisOrderNumber.of("0007103300"));
	}

	@Test
	void timeoutOrderBecomesUnknown() {
		Order order = liveOrder();

		order.markUnknown("KIS 주문 응답 확인 필요");

		assertThat(order.status()).isEqualTo(OrderStatus.UNKNOWN);
		assertThat(order.rejectMessage()).contains("KIS 주문 응답 확인 필요");
	}

	@Test
	void liveOrderAppliesExecutionQuantity() {
		Order order = liveOrder();
		order.accept(KisOrderNumber.of("0007103300"), KisOrderOrgNumber.of("06010"), Instant.parse("2026-05-09T02:00:01Z"));

		order.applyExecution(1L, Instant.parse("2026-05-09T02:01:00Z"));

		assertThat(order.status()).isEqualTo(OrderStatus.FILLED);
		assertThat(order.lastSyncedAt()).contains(Instant.parse("2026-05-09T02:01:00Z"));
	}

	@Test
	void rejectsMarketOrderWithPositiveOrderPrice() {
		assertThatThrownBy(() -> Order.requested(
				OrderId.of(2L),
				StrategyId.of(1L),
				Symbol.of("001510"),
				OrderSide.BUY,
				TradeMode.LIVE,
				OrderType.MARKET,
				OrderQuantity.of(1L),
				Money.won(1_000L),
				Instant.parse("2026-05-09T02:00:00Z")
		)).isInstanceOf(IllegalArgumentException.class);
	}

	private Order liveOrder() {
		return Order.requested(
				OrderId.of(1L),
				StrategyId.of(1L),
				Symbol.of("001510"),
				OrderSide.BUY,
				TradeMode.LIVE,
				OrderType.LIMIT,
				OrderQuantity.of(1L),
				Money.won(1_000L),
				Instant.parse("2026-05-09T02:00:00Z")
		);
	}
}
