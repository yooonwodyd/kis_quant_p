package com.kisquant.kis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.kisquant.order.OrderGuard;
import com.kisquant.support.SensitiveMasker;
import com.kisquant.web.CallResult;

class KisRestClientTest {

	@Test
	void storesRawAccessTokenButReturnsMaskedTokenResponse() throws Exception {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://kis.test");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		KisRestClient client = client(builder.build());

		server.expect(requestTo("https://kis.test/oauth2/tokenP"))
				.andExpect(method(HttpMethod.POST))
				.andRespond(withSuccess("""
						{"access_token":"real-access-token","access_token_token_expired":"2026-05-05 15:19:25","token_type":"Bearer","expires_in":86400}
						""", MediaType.APPLICATION_JSON));

		CallResult result = client.issueToken();

		assertThat(result.rawResponse().toString()).doesNotContain("real-access-token");
		assertThat(accessToken(client)).isEqualTo("real-access-token");
		server.verify();
	}

	private static KisRestClient client(RestClient restClient) {
		return new KisRestClient(
				restClient,
				new KisProperties("https://kis.test", "app-key", "app-secret", "12345678", "01", Duration.ofSeconds(5)),
				new OrderGuard("001510", BigDecimal.valueOf(30_000), 1),
				new SensitiveMasker("12345678", "01")
		);
	}

	private static String accessToken(KisRestClient client) throws Exception {
		Field field = KisRestClient.class.getDeclaredField("accessToken");
		field.setAccessible(true);
		return (String) field.get(client);
	}
}
