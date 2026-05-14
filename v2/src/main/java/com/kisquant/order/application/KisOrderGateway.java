package com.kisquant.order.application;

public interface KisOrderGateway {

    KisOrderResult placeOrder(KisOrderCommand command);

    CancelOrderResult cancel(CancelOrderCommand command);
}
