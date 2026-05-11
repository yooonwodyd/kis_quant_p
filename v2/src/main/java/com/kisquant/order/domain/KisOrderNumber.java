package com.kisquant.order.domain;

public record KisOrderNumber(String value) {

    public KisOrderNumber {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("KIS order number must not be blank");
        }
    }

    public static KisOrderNumber of(String value) {
        return new KisOrderNumber(value);
    }
}
