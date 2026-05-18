package com.kisquant.strategy.adapter.web;

public record OrderResponse(
        long orderId,
        String status,
        String kisOrderNumber,
        String message
) {
}
