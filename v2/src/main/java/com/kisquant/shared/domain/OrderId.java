package com.kisquant.shared.domain;

public record OrderId(long value) {

    public OrderId {
        if (value <= 0L) {
            throw new IllegalArgumentException("order id must be positive");
        }
    }

    public static OrderId of(long value) {
        return new OrderId(value);
    }
}
