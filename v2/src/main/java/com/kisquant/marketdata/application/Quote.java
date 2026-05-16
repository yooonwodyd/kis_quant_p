package com.kisquant.marketdata.application;

import com.kisquant.shared.domain.Money;
import com.kisquant.shared.domain.Symbol;

public record Quote(Symbol symbol, Money currentPrice) {
}
