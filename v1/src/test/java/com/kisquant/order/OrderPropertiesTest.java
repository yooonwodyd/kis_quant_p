package com.kisquant.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;

class OrderPropertiesTest {

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	void rejectsUnsafeOrderLimits() {
		OrderProperties properties = new OrderProperties("", BigDecimal.ZERO, 0);

		assertThat(this.validator.validate(properties)).hasSize(3);
	}

	@Test
	void acceptsSmallRealAccountTestLimits() {
		OrderProperties properties = new OrderProperties("001510", BigDecimal.valueOf(30_000), 1);

		assertThat(this.validator.validate(properties)).isEmpty();
	}
}
