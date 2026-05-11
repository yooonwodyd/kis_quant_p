package com.kisquant.shared.domain;

import java.util.regex.Pattern;

public record Symbol(String value) {

    private static final Pattern DOMESTIC_SYMBOL_PATTERN = Pattern.compile("\\d{6}");

    public Symbol {
        if (value == null || !DOMESTIC_SYMBOL_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("domestic stock symbol must be six digits");
        }
    }

    public static Symbol of(String value) {
        return new Symbol(value);
    }
}
