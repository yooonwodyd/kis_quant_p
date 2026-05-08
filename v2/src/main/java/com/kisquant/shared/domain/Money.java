package com.kisquant.shared.domain;

import java.util.Objects;

/**
 * 원화 금액.
 */
public record Money(long amount) implements Comparable<Money> {

	public static final Money ZERO = new Money(0L);

	public static Money won(long amount) {
		return new Money(amount);
	}

	public Money plus(Money other) {
		Objects.requireNonNull(other, "other must not be null");
		return new Money(Math.addExact(this.amount, other.amount));
	}

	public Money minus(Money other) {
		Objects.requireNonNull(other, "other must not be null");
		return new Money(Math.subtractExact(this.amount, other.amount));
	}

	public Money multiply(long multiplier) {
		return new Money(Math.multiplyExact(this.amount, multiplier));
	}

	public boolean isPositive() {
		return this.amount > 0L;
	}

	public boolean isGreaterThan(Money other) {
		return compareTo(other) > 0;
	}

	@Override
	public int compareTo(Money other) {
		Objects.requireNonNull(other, "other must not be null");
		return Long.compare(this.amount, other.amount);
	}
}
