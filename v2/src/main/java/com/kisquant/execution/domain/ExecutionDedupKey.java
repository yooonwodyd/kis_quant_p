package com.kisquant.execution.domain;

public record ExecutionDedupKey(String value) {

    public ExecutionDedupKey {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("execution dedup key must not be blank");
        }
    }

    public static ExecutionDedupKey of(String value) {
        return new ExecutionDedupKey(value);
    }
}
