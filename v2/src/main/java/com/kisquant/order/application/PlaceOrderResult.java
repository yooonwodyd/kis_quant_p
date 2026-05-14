package com.kisquant.order.application;

import com.kisquant.order.domain.OrderStatus;
import com.kisquant.shared.domain.OrderId;
import java.util.Optional;

public record PlaceOrderResult(
        OrderId orderId,
        OrderStatus status,
        String kisOrderNo,
        String message
) {

    public Optional<String> kisOrderNumber() {
        return Optional.ofNullable(kisOrderNo);
    }
}
