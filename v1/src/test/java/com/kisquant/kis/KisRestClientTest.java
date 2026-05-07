package com.kisquant.kis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpHeaders;
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

	@Test
	void orderPostUsesContentLengthInsteadOfChunkedTransfer() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://kis.test");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		KisRestClient client = client(builder.build());

		server.expect(requestTo("https://kis.test/oauth2/tokenP"))
				.andRespond(withSuccess("""
						{"access_token":"real-access-token","token_type":"Bearer","expires_in":86400}
						""", MediaType.APPLICATION_JSON));
		server.expect(requestTo("https://kis.test/uapi/domestic-stock/v1/trading/order-cash"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(content().json("""
						{"PDNO":"001510","ORD_DVSN":"00","ORD_QTY":"1","ORD_UNPR":"5190"}
						"""))
				.andExpect(request -> {
					assertThat(request.getHeaders().getFirst(HttpHeaders.TRANSFER_ENCODING)).isNull();
					assertThat(request.getHeaders().getContentLength()).isPositive();
				})
				.andRespond(withSuccess("""
						{"rt_cd":"0","msg_cd":"0","msg1":"OK","output":{"ODNO":"1","KRX_FWDG_ORD_ORGNO":"2","ORD_TMD":"152000"}}
						""", MediaType.APPLICATION_JSON));

		CallResult result = client.limitBuy(BigDecimal.valueOf(5190));

		assertThat(result.ok()).isTrue();
		server.verify();
	}

	@Test
	void marketBuyUsesMarketOrderCodeAndZeroPrice() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://kis.test");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		KisRestClient client = client(builder.build());

		server.expect(requestTo("https://kis.test/oauth2/tokenP"))
				.andRespond(withSuccess("""
						{"access_token":"real-access-token","token_type":"Bearer","expires_in":86400}
						""", MediaType.APPLICATION_JSON));
		server.expect(requestTo("https://kis.test/uapi/domestic-stock/v1/trading/order-cash"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(content().json("""
						{"PDNO":"001510","ORD_DVSN":"01","ORD_QTY":"1","ORD_UNPR":"0"}
						"""))
				.andRespond(withSuccess("""
						{"rt_cd":"0","msg_cd":"0","msg1":"OK","output":{"ODNO":"1","KRX_FWDG_ORD_ORGNO":"2","ORD_TMD":"152000"}}
						""", MediaType.APPLICATION_JSON));

		CallResult result = client.marketBuy();

		assertThat(result.ok()).isTrue();
		server.verify();
	}

	@Test
	@SuppressWarnings("unchecked")
	void holdingsSummarizesOwnedStocksAndQuantities() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://kis.test");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		KisRestClient client = client(builder.build());

		server.expect(requestTo("https://kis.test/oauth2/tokenP"))
				.andRespond(withSuccess("""
						{"access_token":"real-access-token","token_type":"Bearer","expires_in":86400}
						""", MediaType.APPLICATION_JSON));
		server.expect(request -> assertThat(request.getURI().getPath()).isEqualTo("/uapi/domestic-stock/v1/trading/inquire-balance"))
				.andRespond(withSuccess("""
						{
						  "output1": [
						    {
						      "pdno": "001510",
						      "prdt_name": "SK증권",
						      "hldg_qty": "2",
						      "ord_psbl_qty": "1",
						      "pchs_avg_pric": "5495.0000",
						      "prpr": "5430",
						      "evlu_amt": "10860"
						    }
						  ],
						  "rt_cd": "0",
						  "msg_cd": "KIOK0510",
						  "msg1": "조회가 완료되었습니다"
						}
						""", MediaType.APPLICATION_JSON));

		CallResult result = client.holdings();

		assertThat(result.ok()).isTrue();
		Map<String, Object> parsed = (Map<String, Object>) result.parsedResponse();
		List<Map<String, String>> holdings = (List<Map<String, String>>) parsed.get("holdings");
		assertThat(holdings).containsExactly(Map.of(
				"symbol", "001510",
				"name", "SK증권",
				"holdingQuantity", "2",
				"orderableQuantity", "1",
				"averagePrice", "5495.0000",
				"currentPrice", "5430",
				"valuationAmount", "10860"
		));
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
