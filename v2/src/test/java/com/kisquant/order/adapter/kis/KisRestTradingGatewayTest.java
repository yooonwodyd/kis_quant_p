package com.kisquant.order.adapter.kis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.kisquant.order.application.KisOrderCommand;
import com.kisquant.order.application.KisOrderResult;
import com.kisquant.order.application.KisOrderResultStatus;
import com.kisquant.order.domain.OrderSide;
import com.kisquant.order.domain.OrderType;
import com.kisquant.shared.config.KisQuantProperties;
import com.kisquant.shared.domain.Money;
import com.kisquant.shared.domain.Symbol;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class KisRestTradingGatewayTest {

    private static final String BASE_URL = "https://kis.example";

    @Test
    void reissuesTokenAndRetriesOrderOnceWhenKisReportsExpiredToken() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        KisQuantProperties properties = properties();
        KisRestTradingGateway gateway = new KisRestTradingGateway(
                restClient,
                new KisTokenManager(restClient, properties),
                properties);

        server.expect(requestTo(BASE_URL + "/oauth2/tokenP"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(tokenResponse("expired-token"), MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/uapi/domestic-stock/v1/trading/order-cash"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer expired-token"))
                .andRespond(withSuccess("""
                        {"rt_cd":"1","msg_cd":"EGW00123","msg1":"기간이 만료된 token 입니다."}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/oauth2/tokenP"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(tokenResponse("fresh-token"), MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/uapi/domestic-stock/v1/trading/order-cash"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer fresh-token"))
                .andRespond(withSuccess("""
                        {"rt_cd":"0","output":{"ODNO":"1234567890","KRX_FWDG_ORD_ORGNO":"00123"}}
                        """, MediaType.APPLICATION_JSON));

        KisOrderResult result = gateway.placeOrder(new KisOrderCommand(
                Symbol.of("001510"),
                OrderSide.BUY,
                OrderType.LIMIT,
                1L,
                Money.won(10_000L)));

        assertThat(result.status()).isEqualTo(KisOrderResultStatus.ACCEPTED);
        assertThat(result.kisOrderNumber().value()).isEqualTo("1234567890");
        server.verify();
    }

    private String tokenResponse(String accessToken) {
        return "{\"access_token\":\"" + accessToken + "\",\"expires_in\":\"86400\"}";
    }

    private KisQuantProperties properties() {
        return new KisQuantProperties(
                new KisQuantProperties.Trading(List.of("001510"), 1_000_000L, 1_000_000L, 1_000_000L),
                new KisQuantProperties.StartupSync(false),
                new KisQuantProperties.Polling(false, Duration.ofSeconds(10L), 10),
                new KisQuantProperties.OperationLogs(100),
                new KisQuantProperties.Kis(BASE_URL, "app-key", "app-secret", "12345678", "01", Duration.ofSeconds(3L)));
    }
}
