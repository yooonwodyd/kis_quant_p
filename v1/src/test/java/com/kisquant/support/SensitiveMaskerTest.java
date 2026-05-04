package com.kisquant.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SensitiveMaskerTest {

	@Test
	void masksTokenAndSecretFields() {
		SensitiveMasker masker = new SensitiveMasker("12345678", "01");

		String masked = masker.mask("""
				{
				  "authorization": "Bearer abcdefghijklmnop",
				  "access_token": "token-value",
				  "appsecret": "secret-value",
				  "safe": "001510"
				}
				""");

		assertThat(masked).doesNotContain("abcdefghijklmnop");
		assertThat(masked).doesNotContain("token-value");
		assertThat(masked).doesNotContain("secret-value");
		assertThat(masked).contains("001510");
	}
}
