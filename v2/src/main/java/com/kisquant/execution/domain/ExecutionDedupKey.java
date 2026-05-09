package com.kisquant.execution.domain;

/**
 * 이미 반영한 체결인지 구분하는 키.
 */
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
