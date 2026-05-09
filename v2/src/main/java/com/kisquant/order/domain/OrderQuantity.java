package com.kisquant.order.domain;

/**
 * 주문 수량.
 */
public record OrderQuantity(long value) {

	public OrderQuantity {
		if (value <= 0L) {
			throw new IllegalArgumentException("order quantity must be positive");
		}
	}

	public static OrderQuantity of(long value) {
		return new OrderQuantity(value);
	}
}
