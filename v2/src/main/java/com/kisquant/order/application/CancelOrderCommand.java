package com.kisquant.order.application;

import com.kisquant.order.domain.KisOrderNumber;
import com.kisquant.order.domain.KisOrderOrgNumber;
import com.kisquant.shared.domain.OrderId;

public record CancelOrderCommand(
        OrderId orderId,
        KisOrderNumber kisOrderNumber,
        KisOrderOrgNumber kisOrderOrgNumber
) {
}
