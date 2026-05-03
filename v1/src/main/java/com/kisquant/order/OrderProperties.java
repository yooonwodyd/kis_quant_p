package com.kisquant.order;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 실전 주문 테스트 범위.
 * 처음 연결 확인할 때는 작게 고정해둔다. 편하게 열어두면 위험하다.
 */
@Validated
@ConfigurationProperties(prefix = "order")
public record OrderProperties(
		@NotBlank String allowedSymbol,
		@NotNull @Positive BigDecimal maxOrderKrw,
		@Positive int fixedOrderQuantity
) {
}
