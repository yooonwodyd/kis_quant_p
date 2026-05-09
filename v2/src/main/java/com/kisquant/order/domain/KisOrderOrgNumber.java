package com.kisquant.order.domain;

/**
 * KIS 거래소 주문조직번호.
 */
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
