package com.kisquant.kis;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.kisquant.order.OrderGuard;
import com.kisquant.support.SensitiveMasker;
import com.kisquant.web.CallResult;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * 한국투자증권 실전 REST API를 호출.
 * token, 잔고, 현재가, 주문 요청을 하나씩 실제로 확인한다.
 * access token은 DB에 저장하지 않고 현재 프로세스 메모리에만 둔다.
 */
@Component
final class KisRestClient implements KisClient {

	private static final String CUSTOMER_TYPE_PERSONAL = "P";

	private final RestClient restClient;
	private final KisProperties properties;
	private final OrderGuard orderGuard;
	private final SensitiveMasker masker;
	private final ObjectMapper objectMapper = new ObjectMapper();

	private volatile String accessToken; // 토큰 갱신 경쟁 완화용. 지금 단계에서는 조금 과할 수도 있음.

	KisRestClient(RestClient restClient, KisProperties properties, OrderGuard orderGuard, SensitiveMasker masker) {
		this.restClient = restClient;
		this.properties = properties;
		this.orderGuard = orderGuard;
		this.masker = masker;
	}

	/**
	 * KIS access token 발급.
	 * 화면에는 마스킹된 응답을 내려주고, 다음 요청을 위해 원문 token만 메모리에 보관한다.
	 */
	@Override
	public CallResult issueToken() {
		Map<String, String> body = new LinkedHashMap<>();
		body.put("grant_type", "client_credentials");
		body.put("appkey", this.properties.appKey());
		body.put("appsecret", this.properties.appSecret());

		Object safeRequest = Map.of("method", "POST", "path", "/oauth2/tokenP");
		TokenCall tokenCall = tokenPost(safeRequest, body);
		CallResult result = tokenCall.result();
		if (result.ok()) {
			String token = extractText(tokenCall.rawResponse(), "access_token");
			this.accessToken = token == null ? this.accessToken : token;
		}
		return result;
	}

	private TokenCall tokenPost(Object safeRequest, Map<String, String> body) {
		try {
			String responseBody = this.restClient.post()
					.uri("/oauth2/tokenP")
					.contentType(MediaType.APPLICATION_JSON)
					.body(body)
					.retrieve()
					.toEntity(String.class)
					.getBody();
			return new TokenCall(responseBody, result("token", 200, safeRequest, responseBody));
		}
		catch (RestClientResponseException ex) {
			return new TokenCall(ex.getResponseBodyAsString(), result("token", ex.getStatusCode(), safeRequest, ex.getResponseBodyAsString()));
		}
	}

	/**
	 * token 없으면 먼저 발급.
	 * 실패했는데 재시도 루프를 돌리지는 않는다. 여기서는 연결 확인이 먼저다.
	 */
	private void ensureToken() {
		if (this.accessToken == null || this.accessToken.isBlank()) {
			issueToken();
		}
		if (this.accessToken == null || this.accessToken.isBlank()) {
			throw new IllegalStateException("KIS access token is not available");
		}
	}

	private void applyKisHeaders(HttpHeaders headers, String trId) {
		headers.setBearerAuth(this.accessToken);
		headers.set("appkey", this.properties.appKey());
		headers.set("appsecret", this.properties.appSecret());
		headers.set("tr_id", trId);
		headers.set("custtype", CUSTOMER_TYPE_PERSONAL);
		headers.set("tr_cont", "");
	}

	private CallResult result(String name, HttpStatusCode status, Object request, String rawResponse) {
		return result(name, status.value(), request, rawResponse);
	}

	private CallResult result(String name, int status, Object request, String rawResponse) {
		String maskedRaw = this.masker.mask(rawResponse == null ? "" : rawResponse);
		boolean ok = status >= 200 && status < 300 && isKisOk(rawResponse);
		return new CallResult(name, ok, status, request, parse(maskedRaw), parseJsonOrRaw(maskedRaw));
	}

	private Object parse(String raw) {
		Map<String, String> fields = new LinkedHashMap<>();
		for (String field : List.of("rt_cd", "msg_cd", "msg1", "KRX_FWDG_ORD_ORGNO", "ODNO", "ORD_TMD")) {
			String value = extractText(raw, field);
			if (value != null) {
				fields.put(field, value);
			}
		}
		return fields.isEmpty() ? raw : fields;
	}

	private Object parseJsonOrRaw(String raw) {
		if (raw == null || raw.isBlank()) {
			return raw;
		}
		try {
			return this.objectMapper.readValue(raw, Object.class);
		}
		catch (JacksonException ex) {
			return raw;
		}
	}

	private boolean isKisOk(String raw) {
		String rtCd = extractText(raw, "rt_cd");
		return rtCd == null || "0".equals(rtCd);
	}

	private String extractText(String raw, String fieldName) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		Pattern pattern = Pattern.compile("\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*\"([^\"]*)\"");
		Matcher matcher = pattern.matcher(raw);
		if (!matcher.find()) {
			return null;
		}
		return matcher.group(1);
	}

	private record TokenCall(String rawResponse, CallResult result) {
	}
}
