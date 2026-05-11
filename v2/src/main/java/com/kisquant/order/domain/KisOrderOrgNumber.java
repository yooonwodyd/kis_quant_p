package com.kisquant.order.domain;

public record KisOrderOrgNumber(String value) {

    public KisOrderOrgNumber {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("KIS order org number must not be blank");
        }
    }

    public static KisOrderOrgNumber of(String value) {
        return new KisOrderOrgNumber(value);
    }
}
