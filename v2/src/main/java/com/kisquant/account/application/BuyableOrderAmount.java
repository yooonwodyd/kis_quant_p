package com.kisquant.account.application;

import com.kisquant.shared.domain.Money;
import com.kisquant.shared.domain.Symbol;

public record BuyableOrderAmount(
        Symbol symbol,
        Money orderableCash,
        long maxBuyQuantity
) {
}
