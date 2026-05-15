package com.kisquant.order.adapter.kis;

import static org.assertj.core.api.Assertions.assertThat;

import com.kisquant.order.application.KisOrderCommand;
import com.kisquant.order.domain.OrderSide;
import com.kisquant.order.domain.OrderType;
import com.kisquant.shared.domain.Money;
import com.kisquant.shared.domain.Symbol;
import org.junit.jupiter.api.Test;

class KisRequestFactoryTest {

    private final KisRequestFactory factory = new KisRequestFactory("12345678", "01");

    @Test
    void limitBuyUsesDomesticCashBuyTrIdAndLimitOrderCode() {
        KisPreparedRequest request = factory.orderCash(new KisOrderCommand(
                Symbol.of("001510"),
                OrderSide.BUY,
                OrderType.LIMIT,
                1L,
                Money.won(1_000L)));

        assertThat(request.path()).isEqualTo("/uapi/domestic-stock/v1/trading/order-cash");
        assertThat(request.trId()).isEqualTo("TTTC0012U");
        assertThat(request.body()).containsEntry("ORD_DVSN", "00");
        assertThat(request.body()).containsEntry("ORD_UNPR", "1000");
    }

    @Test
    void marketSellUsesDomesticCashSellTrIdAndZeroPrice() {
        KisPreparedRequest request = factory.orderCash(new KisOrderCommand(
                Symbol.of("001510"),
                OrderSide.SELL,
                OrderType.MARKET,
                1L,
                Money.ZERO));

        assertThat(request.trId()).isEqualTo("TTTC0011U");
        assertThat(request.body()).containsEntry("ORD_DVSN", "01");
        assertThat(request.body()).containsEntry("ORD_UNPR", "0");
    }
}
