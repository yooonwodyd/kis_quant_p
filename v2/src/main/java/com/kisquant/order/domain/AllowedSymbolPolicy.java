package com.kisquant.order.domain;

import com.kisquant.shared.domain.Symbol;
import java.util.Set;

public record AllowedSymbolPolicy(Set<Symbol> symbols) {

    public AllowedSymbolPolicy {
        if (symbols == null || symbols.isEmpty()) {
            throw new IllegalArgumentException("allowed symbols must not be empty");
        }
        symbols = Set.copyOf(symbols);
    }

    public static AllowedSymbolPolicy of(Set<Symbol> symbols) {
        return new AllowedSymbolPolicy(symbols);
    }

    public boolean allows(Symbol symbol) {
        return symbols.contains(symbol);
    }
}
