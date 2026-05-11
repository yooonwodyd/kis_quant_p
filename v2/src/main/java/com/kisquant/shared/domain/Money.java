package com.kisquant.shared.domain;

import java.util.Objects;

/*
국내주식을 기준으로 작성. 이후 외화거래가 있다면 리펙토링 필요.
 */
public record Money(long amount) implements Comparable<Money> {

    public static final Money ZERO = new Money(0L);

    public static Money won(long amount) {
        return new Money(amount);
    }

    public Money plus(Money other) {
        Objects.requireNonNull(other, "other must not be null");
        return new Money(Math.addExact(amount, other.amount));
    }

    public Money minus(Money other) {
        Objects.requireNonNull(other, "other must not be null");
        return new Money(Math.subtractExact(amount, other.amount));
    }

    public Money multiply(long multiplier) {
        return new Money(Math.multiplyExact(amount, multiplier));
    }

    public boolean isPositive() {
        return amount > 0L;
    }

    public boolean isGreaterThan(Money other) {
        return compareTo(other) > 0;
    }

    @Override
    public int compareTo(Money other) {
        Objects.requireNonNull(other, "other must not be null");
        return Long.compare(amount, other.amount);
    }
}
