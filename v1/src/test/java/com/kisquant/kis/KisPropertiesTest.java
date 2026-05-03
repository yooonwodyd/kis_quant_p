package com.kisquant.kis;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;

class KisPropertiesTest {

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	void rejectsBlankRequiredValues() {
		KisProperties properties = new KisProperties("", "", "", "", "", null);

		assertThat(this.validator.validate(properties)).hasSize(6);
	}

	@Test
	void acceptsRealConnectionShape() {
		KisProperties properties = new KisProperties(
				"https://openapi.koreainvestment.com:9443",
				"app-key",
				"app-secret",
				"12345678",
				"01",
				Duration.ofSeconds(5)
		);

		assertThat(this.validator.validate(properties)).isEmpty();
	}
}
