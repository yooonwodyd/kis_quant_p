package com.kisquant.order;

import java.math.BigDecimal;

/**
 * 실전 주문 테스트용 가드.
 * 처음에는 종목 하나, 1주, 작은 금액만 통과시킨다.
 */
public final class OrderGuard {

	private final String allowedSymbol;
	private final BigDecimal maxOrderKrw;
	private final int fixedOrderQuantity;

	public OrderGuard(String allowedSymbol, BigDecimal maxOrderKrw, int fixedOrderQuantity) {
		this.allowedSymbol = allowedSymbol;
		this.maxOrderKrw = maxOrderKrw;
		this.fixedOrderQuantity = fixedOrderQuantity;
	}

	public String allowedSymbol() {
		return this.allowedSymbol;
	}

	public int fixedOrderQuantity() {
		return this.fixedOrderQuantity;
	}

	public void validateSymbol(String symbol) {
		if (!this.allowedSymbol.equals(symbol)) {
			throw new IllegalArgumentException("Only symbol " + this.allowedSymbol + " is allowed");
		}
	}

	public void validateMarketOrder(String symbol, int quantity) {
		validateSymbol(symbol);
		if (quantity != this.fixedOrderQuantity) {
			throw new IllegalArgumentException("Only " + this.fixedOrderQuantity + " share order is allowed");
		}
	}
}
