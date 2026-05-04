package com.kisquant.support;

import java.util.List;
import java.util.regex.Pattern;

/**
 * KIS 응답을 화면에서 볼 때 위험한 값만 가린다.
 * raw response는 확인해야 해서 다 숨기지는 않는다.
 */
public final class SensitiveMasker {

	private static final Pattern SENSITIVE_JSON_FIELD = Pattern.compile(
			"(?i)(\"(?:authorization|access_token|token|appkey|app_key|appsecret|app_secret|hashkey)\"\\s*:\\s*\")([^\"]+)(\")"
	);

	private final List<String> exactValuesToMask;

	public SensitiveMasker(String accountNumber, String accountProductCode) {
		this.exactValuesToMask = List.of();
	}

	public String mask(String value) {
		if (value == null || value.isBlank()) {
			return value;
		}
		String masked = SENSITIVE_JSON_FIELD.matcher(value).replaceAll("$1****$3");
		for (String exactValue : this.exactValuesToMask) {
			if (!exactValue.isBlank()) {
				masked = masked.replace(exactValue, "****");
			}
		}
		return masked;
	}
}
