package com.kisquant.marketdata.adapter.kis;

import com.kisquant.account.application.AccountBalance;
import com.kisquant.account.application.AccountGateway;
import com.kisquant.account.application.BuyableOrderAmount;
import com.kisquant.account.application.Holding;
import com.kisquant.marketdata.application.DailyCandle;
import com.kisquant.marketdata.application.MarketDataGateway;
import com.kisquant.marketdata.application.Quote;
import com.kisquant.order.adapter.kis.KisTokenManager;
import com.kisquant.shared.config.KisQuantProperties;
import com.kisquant.shared.domain.Money;
import com.kisquant.shared.domain.Symbol;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.ObjectMapper;

@Component
public class KisRestQueryGateway implements MarketDataGateway, AccountGateway {

    private static final DateTimeFormatter KIS_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final RestClient restClient;
    private final KisTokenManager tokenManager;
    private final KisQuantProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public KisRestQueryGateway(RestClient kisRestClient, KisTokenManager tokenManager, KisQuantProperties properties) {
        this.restClient = kisRestClient;
        this.tokenManager = tokenManager;
        this.properties = properties;
    }

    @Override
    public Quote quote(Symbol symbol) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("FID_COND_MRKT_DIV_CODE", "J");
        params.put("FID_INPUT_ISCD", symbol.value());
        Map<String, Object> response = get("/uapi/domestic-stock/v1/quotations/inquire-price", "FHKST01010100", params);
        Map<?, ?> output = map(response.get("output"));
        return new Quote(symbol, Money.won(number(output.get("stck_prpr"))));
    }

    @Override
    public List<DailyCandle> dailyCandles(Symbol symbol) {
        return dailyCandles(symbol, 120);
    }

    @Override
    public List<DailyCandle> dailyCandles(Symbol symbol, int limit) {
        LocalDate today = LocalDate.now();
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("FID_COND_MRKT_DIV_CODE", "J");
        params.put("FID_INPUT_ISCD", symbol.value());
        params.put("FID_INPUT_DATE_1", today.minusDays(Math.multiplyExact(limit, 2L)).format(KIS_DATE));
        params.put("FID_INPUT_DATE_2", today.format(KIS_DATE));
        params.put("FID_PERIOD_DIV_CODE", "D");
        params.put("FID_ORG_ADJ_PRC", "0");
        Map<String, Object> response = get("/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice", "FHKST03010100", params);
        List<DailyCandle> candles = new ArrayList<>();
        for (Object item : list(response.get("output2"))) {
            Map<?, ?> row = map(item);
            candles.add(new DailyCandle(
                    LocalDate.parse(text(row.get("stck_bsop_date")), KIS_DATE),
                    Money.won(number(row.get("stck_clpr")))));
        }
        return candles.stream()
                .limit(limit)
                .toList();
    }

    @Override
    public AccountBalance balance() {
        Map<String, Object> response = get("/uapi/domestic-stock/v1/trading/inquire-balance", "TTTC8434R", balanceParams());
        Map<?, ?> output = list(response.get("output2")).isEmpty() ? Map.of() : map(list(response.get("output2")).getFirst());
        return new AccountBalance(Money.won(number(output.get("dnca_tot_amt"))));
    }

    @Override
    public List<Holding> holdings() {
        Map<String, Object> response = get("/uapi/domestic-stock/v1/trading/inquire-balance", "TTTC8434R", balanceParams());
        List<Holding> holdings = new ArrayList<>();
        for (Object item : list(response.get("output1"))) {
            Map<?, ?> row = map(item);
            long quantity = number(row.get("hldg_qty"));
            if (quantity <= 0L) {
                continue;
            }
            holdings.add(new Holding(
                    Symbol.of(text(row.get("pdno"))),
                    text(row.get("prdt_name")),
                    quantity,
                    number(row.get("ord_psbl_qty")),
                    Money.won(number(row.get("pchs_avg_pric"))),
                    Money.won(number(row.get("prpr"))),
                    Money.won(number(row.get("evlu_amt")))));
        }
        return holdings;
    }

    @Override
    public BuyableOrderAmount buyable(Symbol symbol, Money price) {
        Map<String, Object> params = accountParams();
        params.put("PDNO", symbol.value());
        params.put("ORD_UNPR", String.valueOf(price.amount()));
        params.put("ORD_DVSN", "00");
        params.put("CMA_EVLU_AMT_ICLD_YN", "N");
        params.put("OVRS_ICLD_YN", "N");
        Map<String, Object> response = get("/uapi/domestic-stock/v1/trading/inquire-psbl-order", "TTTC8908R", params);
        Map<?, ?> output = map(response.get("output"));
        return new BuyableOrderAmount(symbol, Money.won(number(output.get("ord_psbl_cash"))), number(output.get("max_buy_qty")));
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

    private Map<String, Object> balanceParams() {
        Map<String, Object> params = accountParams();
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

    private Map<String, Object> accountParams() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("CANO", properties.kis().accountNumber());
        params.put("ACNT_PRDT_CD", properties.kis().accountProductCode());
        return params;
    }

    private void applyHeaders(HttpHeaders headers, String trId) {
        headers.setBearerAuth(tokenManager.accessToken());
        headers.set("appkey", properties.kis().appKey());
        headers.set("appsecret", properties.kis().appSecret());
        headers.set("tr_id", trId);
        headers.set("custtype", "P");
        headers.set("tr_cont", "");
    }

    private Map<?, ?> map(Object value) {
        return value instanceof Map<?, ?> map ? map : Map.of();
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

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
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
}
