package com.kisquant.execution.domain;

public record ExecutionId(long value) {

    public ExecutionId {
        if (value <= 0L) {
            throw new IllegalArgumentException("execution id must be positive");
        }
    }

    public static ExecutionId of(long value) {
        return new ExecutionId(value);
    }
}
