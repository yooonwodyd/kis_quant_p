package com.kisquant.order.application;

import com.kisquant.order.domain.KisOrderNumber;
import com.kisquant.order.domain.KisOrderOrgNumber;
import com.kisquant.order.domain.Order;
import com.kisquant.shared.domain.OrderId;
import com.kisquant.shared.time.CurrentTimeProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CancelOrderApplicationService {

    private final OrderRepository orderRepository;
    private final KisOrderGateway kisOrderGateway;
    private final CurrentTimeProvider timeProvider;

    public CancelOrderApplicationService(
            OrderRepository orderRepository,
            KisOrderGateway kisOrderGateway,
            CurrentTimeProvider timeProvider
    ) {
        this.orderRepository = orderRepository;
        this.kisOrderGateway = kisOrderGateway;
        this.timeProvider = timeProvider;
    }

    @Transactional
    public Order cancel(OrderId orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("order not found"));
        KisOrderNumber orderNumber = order.kisOrderNumber()
                .orElseThrow(() -> new IllegalStateException("KIS order number is required"));
        KisOrderOrgNumber orderOrgNumber = order.kisOrderOrgNumber()
                .orElseThrow(() -> new IllegalStateException("KIS order org number is required"));
        CancelOrderResult result = kisOrderGateway.cancel(new CancelOrderCommand(orderId, orderNumber, orderOrgNumber));
        if (!result.successful()) {
            throw new IllegalStateException(result.message());
        }
        order.markCanceled(timeProvider.now());
        return orderRepository.save(order);
    }
}
