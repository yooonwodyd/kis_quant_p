package com.kisquant.order.adapter.persistence;

import com.kisquant.order.domain.Order;
import com.kisquant.order.domain.KisOrderNumber;
import com.kisquant.order.domain.KisOrderOrgNumber;
import com.kisquant.order.domain.OrderQuantity;
import com.kisquant.order.domain.OrderSide;
import com.kisquant.order.domain.OrderStatus;
import com.kisquant.order.domain.OrderType;
import com.kisquant.shared.domain.Money;
import com.kisquant.shared.domain.OrderId;
import com.kisquant.shared.domain.StrategyId;
import com.kisquant.shared.domain.Symbol;
import com.kisquant.shared.domain.TradeMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "orders")
class JpaOrderEntity {

    @Id
    private Long id;

    @Column(nullable = false)
    private Long strategyId;

    @Column(nullable = false, length = 6)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OrderSide side;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TradeMode tradeMode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderType orderType;

    @Column(nullable = false)
    private Long quantity;

    @Column(nullable = false)
    private Long orderPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    @Column(length = 30)
    private String kisOrderNo;

    @Column(length = 30)
    private String kisOrderOrgNo;

    @Column(nullable = false)
    private Instant requestedAt;

    private Instant acceptedAt;

    private Instant lastSyncedAt;

    @Column(length = 100)
    private String rejectCode;

    @Column(length = 500)
    private String rejectMessage;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected JpaOrderEntity() {
    }

    static JpaOrderEntity from(Order order) {
        JpaOrderEntity entity = new JpaOrderEntity();
        entity.updateFrom(order);
        return entity;
    }

    void updateFrom(Order order) {
        id = order.id().value();
        strategyId = order.strategyId().value();
        symbol = order.symbol().value();
        side = order.side();
        tradeMode = order.tradeMode();
        orderType = order.orderType();
        quantity = order.quantity().value();
        orderPrice = order.orderPrice().amount();
        status = order.status();
        kisOrderNo = order.kisOrderNumber().map(kisOrderNumber -> kisOrderNumber.value()).orElse(null);
        kisOrderOrgNo = order.kisOrderOrgNumber().map(kisOrderOrgNumber -> kisOrderOrgNumber.value()).orElse(null);
        requestedAt = order.requestedAt();
        acceptedAt = order.acceptedAt().orElse(null);
        lastSyncedAt = order.lastSyncedAt().orElse(null);
        rejectCode = order.rejectCode().orElse(null);
        rejectMessage = order.rejectMessage().orElse(null);
    }

    Order toDomain() {
        return Order.restore(
                OrderId.of(id),
                StrategyId.of(strategyId),
                Symbol.of(symbol),
                side,
                tradeMode,
                orderType,
                OrderQuantity.of(quantity),
                Money.won(orderPrice),
                status,
                requestedAt,
                acceptedAt,
                lastSyncedAt,
                kisOrderNo == null ? null : KisOrderNumber.of(kisOrderNo),
                kisOrderOrgNo == null ? null : KisOrderOrgNumber.of(kisOrderOrgNo),
                rejectCode,
                rejectMessage);
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
