package com.kisquant.kis;

import java.math.BigDecimal;
import java.util.ArrayList;
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

	private static final String LIMIT_ORDER = "00";
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

	@Override
	public CallResult balance() {
		return getWithAuth("balance", "/uapi/domestic-stock/v1/trading/inquire-balance", "TTTC8434R", balanceParams());
	}

	@Override
	public CallResult holdings() {
		ensureToken();
		Map<String, Object> params = balanceParams();
		Object safeRequest = safeRequest("GET", "/uapi/domestic-stock/v1/trading/inquire-balance", "TTTC8434R", params);
		try {
			String body = this.restClient.get()
					.uri(uriBuilder -> {
						var builder = uriBuilder.path("/uapi/domestic-stock/v1/trading/inquire-balance");
						params.forEach(builder::queryParam);
						return builder.build();
					})
					.headers(headers -> applyKisHeaders(headers, "TTTC8434R"))
					.retrieve()
					.toEntity(String.class)
					.getBody();
			return holdingsResult(200, safeRequest, body);
		}
		catch (RestClientResponseException ex) {
			return holdingsResult(ex.getStatusCode().value(), safeRequest, ex.getResponseBodyAsString());
		}
	}

	@Override
	public CallResult quote(String symbol) {
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("FID_COND_MRKT_DIV_CODE", "J");
		params.put("FID_INPUT_ISCD", symbol);
		return getWithAuth("quote", "/uapi/domestic-stock/v1/quotations/inquire-price", "FHKST01010100", params);
	}

	@Override
	public CallResult buyable(String symbol, BigDecimal price) {
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("CANO", this.properties.accountNumber());
		params.put("ACNT_PRDT_CD", this.properties.accountProductCode());
		params.put("PDNO", symbol);
		params.put("ORD_UNPR", price.toPlainString());
		params.put("ORD_DVSN", LIMIT_ORDER);
		params.put("CMA_EVLU_AMT_ICLD_YN", "N");
		params.put("OVRS_ICLD_YN", "N");
		return getWithAuth("buyable", "/uapi/domestic-stock/v1/trading/inquire-psbl-order", "TTTC8908R", params);
	}

	/**
	 * KIS 잔고 조회 파라미터.
	 * 일단 문서에서 요구하는 기본값을 그대로 맞춰본다.
	 */
	private Map<String, Object> balanceParams() {
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("CANO", this.properties.accountNumber());
		params.put("ACNT_PRDT_CD", this.properties.accountProductCode());
		params.put("AFHR_FLPR_YN", "N");
		params.put("OFL_YN", "");
		params.put("INQR_DVSN", "01");
		params.put("UNPR_DVSN", "01");
		params.put("FUND_STTL_ICLD_YN", "N");
		params.put("FNCG_AMT_AUTO_RDPT_YN", "N");
		params.put("PRCS_DVSN", "00");
		params.put("CTX_AREA_FK100", "");
		params.put("CTX_AREA_NK100", "");
		return params;
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

	/**
	 * 인증이 필요한 KIS GET API 호출.
	 * URL만으로 업무가 끝나지 않고 tr_id까지 맞춰야 한다.
	 */
	private CallResult getWithAuth(String name, String path, String trId, Map<String, Object> params) {
		ensureToken();
		Object safeRequest = safeRequest("GET", path, trId, params);
		try {
			String body = this.restClient.get()
					.uri(uriBuilder -> {
						var builder = uriBuilder.path(path);
						params.forEach(builder::queryParam);
						return builder.build();
					})
					.headers(headers -> applyKisHeaders(headers, trId))
					.retrieve()
					.toEntity(String.class)
					.getBody();
			return result(name, 200, safeRequest, body);
		}
		catch (RestClientResponseException ex) {
			return result(name, ex.getStatusCode(), safeRequest, ex.getResponseBodyAsString());
		}
	}

	private Object safeRequest(String method, String path, String trId, Map<String, Object> values) {
		Map<String, Object> safe = new LinkedHashMap<>();
		safe.put("method", method);
		safe.put("path", path);
		safe.put("trId", trId);
		safe.put("values", safeValues(values));
		return safe;
	}

	private Map<String, Object> safeValues(Map<String, Object> values) {
		Map<String, Object> safe = new LinkedHashMap<>();
		values.forEach((key, value) -> safe.put(key, value instanceof String text ? this.masker.mask(text) : value));
		return safe;
	}

	private CallResult result(String name, HttpStatusCode status, Object request, String rawResponse) {
		return result(name, status.value(), request, rawResponse);
	}

	private CallResult result(String name, int status, Object request, String rawResponse) {
		String maskedRaw = this.masker.mask(rawResponse == null ? "" : rawResponse);
		boolean ok = status >= 200 && status < 300 && isKisOk(rawResponse);
		return new CallResult(name, ok, status, request, parse(maskedRaw), parseJsonOrRaw(maskedRaw));
	}

	private CallResult holdingsResult(int status, Object request, String rawResponse) {
		String maskedRaw = this.masker.mask(rawResponse == null ? "" : rawResponse);
		Object structuredRaw = parseJsonOrRaw(maskedRaw);
		Object parsed = Map.of("holdings", holdingsFrom(structuredRaw));
		boolean ok = status >= 200 && status < 300 && isKisOk(rawResponse);
		return new CallResult("holdings", ok, status, request, parsed, structuredRaw);
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

	private List<Map<String, String>> holdingsFrom(Object raw) {
		List<Map<String, String>> holdings = new ArrayList<>();
		if (!(raw instanceof Map<?, ?> root) || !(root.get("output1") instanceof List<?> items)) {
			return holdings;
		}
		for (Object item : items) {
			if (item instanceof Map<?, ?> row) {
				String holdingQuantity = text(row.get("hldg_qty"));
				if (hasHoldingQuantity(holdingQuantity)) {
					Map<String, String> holding = new LinkedHashMap<>();
					holding.put("symbol", text(row.get("pdno")));
					holding.put("name", text(row.get("prdt_name")));
					holding.put("holdingQuantity", holdingQuantity);
					holding.put("orderableQuantity", text(row.get("ord_psbl_qty")));
					holding.put("averagePrice", text(row.get("pchs_avg_pric")));
					holding.put("currentPrice", text(row.get("prpr")));
					holding.put("valuationAmount", text(row.get("evlu_amt")));
					holdings.add(holding);
				}
			}
		}
		return holdings;
	}

	private boolean hasHoldingQuantity(String value) {
		if (value.isBlank()) {
			return false;
		}
		try {
			return new BigDecimal(value).compareTo(BigDecimal.ZERO) > 0;
		}
		catch (NumberFormatException ex) {
			return true;
		}
	}

	private String text(Object value) {
		return value == null ? "" : String.valueOf(value);
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
