package com.kisquant.shared.domain;

/**
 * 전략 식별자.
 */
public record StrategyId(long value) {

	public StrategyId {
		if (value <= 0L) {
			throw new IllegalArgumentException("strategy id must be positive");
		}
	}

	public static StrategyId of(long value) {
		return new StrategyId(value);
	}
}
