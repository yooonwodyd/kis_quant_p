package com.kisquant.order.domain;

import com.kisquant.shared.domain.Symbol;

@FunctionalInterface
public interface AllowedSymbolPolicy {

	boolean allows(Symbol symbol);
}
