package com.kisquant.order.adapter.kis;

import com.kisquant.execution.application.KisExecutionGateway;
import com.kisquant.execution.application.KisExecutionSnapshot;
import com.kisquant.order.application.CancelOrderCommand;
import com.kisquant.order.application.CancelOrderResult;
import com.kisquant.order.application.KisOrderCommand;
import com.kisquant.order.application.KisOrderGateway;
import com.kisquant.order.application.KisOrderResult;
import com.kisquant.order.application.KisTimeoutException;
import com.kisquant.order.domain.KisOrderNumber;
import com.kisquant.order.domain.KisOrderOrgNumber;
import com.kisquant.order.domain.Order;
import com.kisquant.shared.config.KisQuantProperties;
import com.kisquant.shared.domain.Money;
import java.net.SocketTimeoutException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.ObjectMapper;

@Component
public class KisRestTradingGateway implements KisOrderGateway, KisExecutionGateway {

    private static final String CUSTOMER_TYPE_PERSONAL = "P";
    private static final DateTimeFormatter KIS_DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final RestClient restClient;
    private final KisTokenManager tokenManager;
    private final KisQuantProperties properties;
    private final KisRequestFactory requestFactory;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public KisRestTradingGateway(
            RestClient kisRestClient,
            KisTokenManager tokenManager,
            KisQuantProperties properties
    ) {
        this.restClient = kisRestClient;
        this.tokenManager = tokenManager;
        this.properties = properties;
        this.requestFactory = new KisRequestFactory(properties.kis().accountNumber(), properties.kis().accountProductCode());
    }

    @Override
    public KisOrderResult placeOrder(KisOrderCommand command) {
        KisPreparedRequest request = requestFactory.orderCash(command);
        Map<String, Object> response = post(request);
        String rtCd = text(response.get("rt_cd"));
        if (!rtCd.isBlank() && !"0".equals(rtCd)) {
            return KisOrderResult.rejected(text(response.get("msg_cd")), text(response.get("msg1")));
        }
        Map<?, ?> output = response.get("output") instanceof Map<?, ?> outputMap ? outputMap : Map.of();
        return KisOrderResult.accepted(
                KisOrderNumber.of(text(output.get("ODNO"))),
                KisOrderOrgNumber.of(text(output.get("KRX_FWDG_ORD_ORGNO"))));
    }

    @Override
    public CancelOrderResult cancel(CancelOrderCommand command) {
        Map<String, Object> response = post(requestFactory.cancel(command));
        String rtCd = text(response.get("rt_cd"));
        if ("0".equals(rtCd) || rtCd.isBlank()) {
            return CancelOrderResult.succeeded();
        }
        return CancelOrderResult.failed(text(response.get("msg1")));
    }

    @Override
    public List<KisExecutionSnapshot> findExecutionSnapshots(Order order) {
        Map<String, Object> params = dailyExecutionParams(order);
        Map<String, Object> response = get("/uapi/domestic-stock/v1/trading/inquire-daily-ccld", "TTTC0081R", params);
        List<KisExecutionSnapshot> snapshots = new ArrayList<>();
        for (Object item : list(response.get("output1"))) {
            Map<?, ?> row = item instanceof Map<?, ?> rowMap ? rowMap : Map.of();
            long executedQuantity = number(row.get("tot_ccld_qty"));
            if (executedQuantity <= 0L) {
                continue;
            }
            String orderNumber = text(row.get("odno"));
            if (orderNumber.isBlank()) {
                orderNumber = order.kisOrderNumber().map(KisOrderNumber::value).orElse("");
            }
            if (orderNumber.isBlank()) {
                continue;
            }
            long averagePrice = number(row.get("avg_prvs"));
            long cumulativeAmount = number(row.get("tot_ccld_amt"));
            if (cumulativeAmount <= 0L) {
                cumulativeAmount = Math.multiplyExact(averagePrice, executedQuantity);
            }
            if (averagePrice <= 0L && cumulativeAmount > 0L) {
                averagePrice = Math.floorDiv(cumulativeAmount, executedQuantity);
            }
            snapshots.add(new KisExecutionSnapshot(
                    KisOrderNumber.of(orderNumber),
                    executedQuantity,
                    Money.won(cumulativeAmount),
                    Money.won(averagePrice),
                    executionTime(row)));
        }
        return snapshots;
    }

    private Map<String, Object> post(KisPreparedRequest request) {
        Map<String, Object> response = postOnce(request);
        if (tokenManager.isExpiredTokenResponse(response)) {
            tokenManager.refreshAccessToken();
            return postOnce(request);
        }
        return response;
    }

    private Map<String, Object> postOnce(KisPreparedRequest request) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri(request.path())
                    .headers(headers -> applyHeaders(headers, request.trId()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(writeJsonBody(request.body()))
                    .retrieve()
                    .body(Map.class);
            return response;
        } catch (ResourceAccessException exception) {
            if (exception.getCause() instanceof SocketTimeoutException) {
                throw new KisTimeoutException("KIS request timeout");
            }
            throw exception;
        } catch (RestClientResponseException exception) {
            return parseBody(exception.getResponseBodyAsString());
        }
    }

    private Map<String, Object> get(String path, String trId, Map<String, Object> params) {
        Map<String, Object> response = getOnce(path, trId, params);
        if (tokenManager.isExpiredTokenResponse(response)) {
            tokenManager.refreshAccessToken();
            return getOnce(path, trId, params);
        }
        return response;
    }

    private Map<String, Object> getOnce(String path, String trId, Map<String, Object> params) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder.path(path);
                        params.forEach(builder::queryParam);
                        return builder.build();
                    })
                    .headers(headers -> applyHeaders(headers, trId))
                    .retrieve()
                    .body(Map.class);
            return response;
        } catch (RestClientResponseException exception) {
            Map<String, Object> response = parseBody(exception.getResponseBodyAsString());
            if (tokenManager.isExpiredTokenResponse(response)) {
                return response;
            }
            throw exception;
        }
    }

    private Map<String, Object> dailyExecutionParams(Order order) {
        String today = LocalDate.now(SEOUL).format(KIS_DATE);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("CANO", properties.kis().accountNumber());
        params.put("ACNT_PRDT_CD", properties.kis().accountProductCode());
        params.put("INQR_STRT_DT", today);
        params.put("INQR_END_DT", today);
        params.put("SLL_BUY_DVSN_CD", "00");
        params.put("PDNO", order.symbol().value());
        params.put("CCLD_DVSN", "00");
        params.put("INQR_DVSN", "00");
        params.put("INQR_DVSN_3", "00");
        params.put("ORD_GNO_BRNO", order.kisOrderOrgNumber().map(KisOrderOrgNumber::value).orElse(""));
        params.put("ODNO", order.kisOrderNumber().map(KisOrderNumber::value).orElse(""));
        params.put("INQR_DVSN_1", "");
        params.put("CTX_AREA_FK100", "");
        params.put("CTX_AREA_NK100", "");
        params.put("EXCG_ID_DVSN_CD", "KRX");
        return params;
    }

    private java.time.Instant executionTime(Map<?, ?> row) {
        String date = text(row.get("ord_dt"));
        String time = text(row.get("ord_tmd"));
        if (date.length() == 8 && time.length() >= 6) {
            return LocalDate.parse(date, KIS_DATE)
                    .atTime(LocalTime.parse(time.substring(0, 6), DateTimeFormatter.ofPattern("HHmmss")))
                    .atZone(SEOUL)
                    .toInstant();
        }
        return java.time.Instant.now();
    }

    private void applyHeaders(HttpHeaders headers, String trId) {
        headers.setBearerAuth(tokenManager.accessToken());
        headers.set("appkey", properties.kis().appKey());
        headers.set("appsecret", properties.kis().appSecret());
        headers.set("tr_id", trId);
        headers.set("custtype", CUSTOMER_TYPE_PERSONAL);
        headers.set("tr_cont", "");
    }

    private byte[] writeJsonBody(Map<?, ?> body) {
        try {
            return objectMapper.writeValueAsBytes(body);
        } catch (Exception exception) {
            throw new IllegalStateException("failed to serialize KIS request body", exception);
        }
    }

    private Map<String, Object> parseBody(String raw) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = objectMapper.readValue(raw, Map.class);
            return parsed;
        } catch (Exception exception) {
            return Map.of("rt_cd", "1", "msg1", raw == null ? "" : raw);
        }
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private List<?> list(Object value) {
        return value instanceof List<?> list ? list : List.of();
    }

    private long number(Object value) {
        String text = text(value).replace(",", "").trim();
        if (text.isBlank()) {
            return 0L;
        }
        return Long.parseLong(text.split("\\.")[0]);
    }
}
