package com.kisquant.order.domain;

import com.kisquant.shared.domain.Money;
import com.kisquant.shared.domain.OrderId;
import com.kisquant.shared.domain.StrategyId;
import com.kisquant.shared.domain.Symbol;
import com.kisquant.shared.domain.TradeMode;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

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

    public static Order restore(
            OrderId id,
            StrategyId strategyId,
            Symbol symbol,
            OrderSide side,
            TradeMode tradeMode,
            OrderType orderType,
            OrderQuantity quantity,
            Money orderPrice,
            OrderStatus status,
            Instant requestedAt,
            Instant acceptedAt,
            Instant lastSyncedAt,
            KisOrderNumber kisOrderNumber,
            KisOrderOrgNumber kisOrderOrgNumber,
            String rejectCode,
            String rejectMessage
    ) {
        Order order = new Order(id, strategyId, symbol, side, tradeMode, orderType, quantity, orderPrice, requestedAt);
        order.status = Objects.requireNonNull(status, "status must not be null");
        order.acceptedAt = acceptedAt;
        order.lastSyncedAt = lastSyncedAt;
        order.kisOrderNumber = kisOrderNumber;
        order.kisOrderOrgNumber = kisOrderOrgNumber;
        order.rejectCode = rejectCode;
        order.rejectMessage = rejectMessage;
        return order;
    }

    public void accept(KisOrderNumber kisOrderNumber, KisOrderOrgNumber kisOrderOrgNumber, Instant acceptedAt) {
        requireStatus(OrderStatus.REQUESTED);
        if (tradeMode != TradeMode.LIVE) {
            throw new IllegalStateException("only live orders can have KIS identifiers");
        }
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

    public void fillBySimulation(Instant filledAt) {
        Objects.requireNonNull(filledAt, "filledAt must not be null");
        requireStatus(OrderStatus.REQUESTED);
        if (tradeMode != TradeMode.SIMULATION) {
            throw new IllegalArgumentException("only simulation orders can be filled by simulation");
        }
        this.status = OrderStatus.FILLED;
    }

    public void applyExecution(long cumulativeExecutedQuantity, Instant syncedAt) {
        Objects.requireNonNull(syncedAt, "syncedAt must not be null");
        if (!isPollingTarget()) {
            throw new IllegalStateException("order is not a polling target");
        }
        if (cumulativeExecutedQuantity <= 0L) {
            throw new IllegalArgumentException("cumulative executed quantity must be positive");
        }
        this.lastSyncedAt = syncedAt;
        this.status = cumulativeExecutedQuantity >= quantity.value() ? OrderStatus.FILLED : OrderStatus.PARTIALLY_FILLED;
    }

    public void markCanceled(Instant canceledAt) {
        Objects.requireNonNull(canceledAt, "canceledAt must not be null");
        if (status == OrderStatus.FILLED || status == OrderStatus.REJECTED) {
            throw new IllegalStateException("finished order cannot be canceled");
        }
        this.status = OrderStatus.CANCELED;
        this.lastSyncedAt = canceledAt;
    }

    public boolean isPollingTarget() {
        return tradeMode == TradeMode.LIVE
                && (status == OrderStatus.ACCEPTED || status == OrderStatus.PARTIALLY_FILLED || status == OrderStatus.UNKNOWN);
    }

    public boolean isFilled() {
        return status == OrderStatus.FILLED;
    }

    public OrderId id() {
        return id;
    }

    public StrategyId strategyId() {
        return strategyId;
    }

    public Symbol symbol() {
        return symbol;
    }

    public OrderSide side() {
        return side;
    }

    public TradeMode tradeMode() {
        return tradeMode;
    }

    public OrderType orderType() {
        return orderType;
    }

    public OrderQuantity quantity() {
        return quantity;
    }

    public Money orderPrice() {
        return orderPrice;
    }

    public Instant requestedAt() {
        return requestedAt;
    }

    public OrderStatus status() {
        return status;
    }

    public Optional<KisOrderNumber> kisOrderNumber() {
        return Optional.ofNullable(kisOrderNumber);
    }

    public Optional<KisOrderOrgNumber> kisOrderOrgNumber() {
        return Optional.ofNullable(kisOrderOrgNumber);
    }

    public Optional<Instant> acceptedAt() {
        return Optional.ofNullable(acceptedAt);
    }

    public Optional<Instant> lastSyncedAt() {
        return Optional.ofNullable(lastSyncedAt);
    }

    public Optional<String> rejectCode() {
        return Optional.ofNullable(rejectCode);
    }

    public Optional<String> rejectMessage() {
        return Optional.ofNullable(rejectMessage);
    }

    private void requireStatus(OrderStatus requiredStatus) {
        if (status != requiredStatus) {
            throw new IllegalStateException("order status must be " + requiredStatus);
        }
    }

    private static Money validateOrderPrice(OrderType orderType, Money orderPrice) {
        Money price = Objects.requireNonNull(orderPrice, "orderPrice must not be null");
        if (orderType == OrderType.LIMIT && !price.isPositive()) {
            throw new IllegalArgumentException("limit order price must be positive");
        }
        return price;
    }
}
