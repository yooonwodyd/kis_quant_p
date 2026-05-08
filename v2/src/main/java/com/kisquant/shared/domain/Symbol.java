package com.kisquant.shared.domain;

/**
 * 국내주식 종목코드.
 */
public record Symbol(String value) {

	public Symbol {
		if (value == null || !value.matches("\\d{6}")) {
			throw new IllegalArgumentException("symbol must be six digits");
		}
	}

	public static Symbol of(String value) {
		return new Symbol(value);
	}
}
