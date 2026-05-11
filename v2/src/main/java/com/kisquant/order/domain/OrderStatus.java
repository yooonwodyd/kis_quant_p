package com.kisquant.order.domain;

public enum OrderStatus {
    REQUESTED,
    ACCEPTED,
    PARTIALLY_FILLED,
    FILLED,
    CANCELED,
    REJECTED,
    UNKNOWN
}
