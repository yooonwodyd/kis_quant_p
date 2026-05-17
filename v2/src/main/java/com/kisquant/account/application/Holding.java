package com.kisquant.account.application;

import com.kisquant.shared.domain.Money;
import com.kisquant.shared.domain.Symbol;

public record Holding(
        Symbol symbol,
        String name,
        long holdingQuantity,
        long orderableQuantity,
        Money averagePrice,
        Money currentPrice,
        Money valuationAmount
) {
}
