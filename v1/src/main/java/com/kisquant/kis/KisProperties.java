package com.kisquant.kis;

import java.time.Duration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * KIS 연결에 필요한 값들.
 * 실제 값은 p_env에서 읽는다. 코드에 박아두면 바로 사고난다.
 */
@Validated
@ConfigurationProperties(prefix = "kis")
public record KisProperties(
		@NotBlank String baseUrl,
		@NotBlank String appKey,
		@NotBlank String appSecret,
		@NotBlank String accountNumber,
		@NotBlank String accountProductCode,
		@NotNull Duration timeout
) {
}
