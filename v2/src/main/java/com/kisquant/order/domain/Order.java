package com.kisquant.order.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import com.kisquant.shared.domain.Money;
import com.kisquant.shared.domain.OrderId;
import com.kisquant.shared.domain.StrategyId;
import com.kisquant.shared.domain.Symbol;
import com.kisquant.shared.domain.TradeMode;

/**
 * Spring이 접수한 주문.
 */
public final class Order {

	private final OrderId id;
	private final StrategyId strategyId;
	private final Symbol symbol;
	private final OrderSide side;
	private final TradeMode tradeMode;
	private final OrderType orderType;
	private final OrderQuantity quantity;
	private final Money orderPrice;
	private final Instant requestedAt;
	private OrderStatus status;
	private KisOrderNumber kisOrderNumber;
	private KisOrderOrgNumber kisOrderOrgNumber;
	private Instant acceptedAt;
	private Instant lastSyncedAt;
	private String rejectCode;
	private String rejectMessage;

	private Order(
			OrderId id,
			StrategyId strategyId,
			Symbol symbol,
			OrderSide side,
			TradeMode tradeMode,
			OrderType orderType,
			OrderQuantity quantity,
			Money orderPrice,
			Instant requestedAt
	) {
		this.id = Objects.requireNonNull(id, "id must not be null");
		this.strategyId = Objects.requireNonNull(strategyId, "strategyId must not be null");
		this.symbol = Objects.requireNonNull(symbol, "symbol must not be null");
		this.side = Objects.requireNonNull(side, "side must not be null");
		this.tradeMode = Objects.requireNonNull(tradeMode, "tradeMode must not be null");
		this.orderType = Objects.requireNonNull(orderType, "orderType must not be null");
		this.quantity = Objects.requireNonNull(quantity, "quantity must not be null");
		this.orderPrice = validateOrderPrice(orderType, orderPrice);
		this.requestedAt = Objects.requireNonNull(requestedAt, "requestedAt must not be null");
		this.status = OrderStatus.REQUESTED;
	}

	public static Order requested(
			OrderId id,
			StrategyId strategyId,
			Symbol symbol,
			OrderSide side,
			TradeMode tradeMode,
			OrderType orderType,
			OrderQuantity quantity,
			Money orderPrice,
			Instant requestedAt
	) {
		return new Order(id, strategyId, symbol, side, tradeMode, orderType, quantity, orderPrice, requestedAt);
	}

	public void accept(KisOrderNumber kisOrderNumber, KisOrderOrgNumber kisOrderOrgNumber, Instant acceptedAt) {
		requireStatus(OrderStatus.REQUESTED);
		this.kisOrderNumber = Objects.requireNonNull(kisOrderNumber, "kisOrderNumber must not be null");
		this.kisOrderOrgNumber = Objects.requireNonNull(kisOrderOrgNumber, "kisOrderOrgNumber must not be null");
		this.acceptedAt = Objects.requireNonNull(acceptedAt, "acceptedAt must not be null");
		this.status = OrderStatus.ACCEPTED;
	}

	public void reject(String rejectCode, String rejectMessage) {
		requireStatus(OrderStatus.REQUESTED);
		this.rejectCode = rejectCode;
		this.rejectMessage = rejectMessage;
		this.status = OrderStatus.REJECTED;
	}

	public void markUnknown(String message) {
		requireStatus(OrderStatus.REQUESTED);
		this.rejectMessage = message;
		this.status = OrderStatus.UNKNOWN;
	}

	public void applyExecution(long cumulativeExecutedQuantity, Instant syncedAt) {
		Objects.requireNonNull(syncedAt, "syncedAt must not be null");
		if (cumulativeExecutedQuantity <= 0L) {
			throw new IllegalArgumentException("cumulative executed quantity must be positive");
		}
		this.lastSyncedAt = syncedAt;
		this.status = cumulativeExecutedQuantity >= this.quantity.value()
				? OrderStatus.FILLED
				: OrderStatus.PARTIALLY_FILLED;
	}

	public OrderId id() {
		return this.id;
	}

	public StrategyId strategyId() {
		return this.strategyId;
	}

	public Symbol symbol() {
		return this.symbol;
	}

	public OrderSide side() {
		return this.side;
	}

	public TradeMode tradeMode() {
		return this.tradeMode;
	}

	public OrderType orderType() {
		return this.orderType;
	}

	public OrderQuantity quantity() {
		return this.quantity;
	}

	public Money orderPrice() {
		return this.orderPrice;
	}

	public Instant requestedAt() {
		return this.requestedAt;
	}

	public OrderStatus status() {
		return this.status;
	}

	public Optional<KisOrderNumber> kisOrderNumber() {
		return Optional.ofNullable(this.kisOrderNumber);
	}

	public Optional<KisOrderOrgNumber> kisOrderOrgNumber() {
		return Optional.ofNullable(this.kisOrderOrgNumber);
	}

	public Optional<Instant> acceptedAt() {
		return Optional.ofNullable(this.acceptedAt);
	}

	public Optional<Instant> lastSyncedAt() {
		return Optional.ofNullable(this.lastSyncedAt);
	}

	public Optional<String> rejectCode() {
		return Optional.ofNullable(this.rejectCode);
	}

	public Optional<String> rejectMessage() {
		return Optional.ofNullable(this.rejectMessage);
	}

	private static Money validateOrderPrice(OrderType orderType, Money orderPrice) {
		Money price = Objects.requireNonNull(orderPrice, "orderPrice must not be null");
		if (orderType == OrderType.MARKET && !price.equals(Money.ZERO)) {
			throw new IllegalArgumentException("market order price must be zero");
		}
		if (orderType == OrderType.LIMIT && !price.isPositive()) {
			throw new IllegalArgumentException("limit order price must be positive");
		}
		return price;
	}

	private void requireStatus(OrderStatus expected) {
		if (this.status != expected) {
			throw new IllegalStateException("order status must be " + expected);
		}
	}
}
