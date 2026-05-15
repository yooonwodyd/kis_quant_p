package com.kisquant.order.adapter.kis;

import com.kisquant.order.application.CancelOrderCommand;
import com.kisquant.order.application.KisOrderCommand;
import com.kisquant.order.domain.OrderSide;
import com.kisquant.order.domain.OrderType;
import java.util.LinkedHashMap;
import java.util.Map;

final class KisRequestFactory {

    private static final String LIMIT_ORDER = "00";
    private static final String MARKET_ORDER = "01";
    private static final String EXCHANGE_KRX = "KRX";

    private final String accountNumber;
    private final String accountProductCode;

    KisRequestFactory(String accountNumber, String accountProductCode) {
        this.accountNumber = accountNumber;
        this.accountProductCode = accountProductCode;
    }

    KisPreparedRequest orderCash(KisOrderCommand command) {
        Map<String, Object> body = accountBody();
        body.put("PDNO", command.symbol().value());
        body.put("ORD_DVSN", command.orderType() == OrderType.MARKET ? MARKET_ORDER : LIMIT_ORDER);
        body.put("ORD_QTY", String.valueOf(command.quantity()));
        body.put("ORD_UNPR", command.orderType() == OrderType.MARKET ? "0" : String.valueOf(command.orderPrice().amount()));
        body.put("EXCG_ID_DVSN_CD", EXCHANGE_KRX);
        body.put("SLL_TYPE", "");
        body.put("CNDT_PRIC", "");
        return new KisPreparedRequest(
                "/uapi/domestic-stock/v1/trading/order-cash",
                command.side() == OrderSide.BUY ? "TTTC0012U" : "TTTC0011U",
                body);
    }

    KisPreparedRequest cancel(CancelOrderCommand command) {
        Map<String, Object> body = accountBody();
        body.put("KRX_FWDG_ORD_ORGNO", command.kisOrderOrgNumber().value());
        body.put("ORGN_ODNO", command.kisOrderNumber().value());
        body.put("ORD_DVSN", LIMIT_ORDER);
        body.put("RVSE_CNCL_DVSN_CD", "02");
        body.put("ORD_QTY", "0");
        body.put("ORD_UNPR", "0");
        body.put("QTY_ALL_ORD_YN", "Y");
        body.put("EXCG_ID_DVSN_CD", EXCHANGE_KRX);
        return new KisPreparedRequest("/uapi/domestic-stock/v1/trading/order-rvsecncl", "TTTC0013U", body);
    }

    private Map<String, Object> accountBody() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("CANO", accountNumber);
        body.put("ACNT_PRDT_CD", accountProductCode);
        return body;
    }
}
