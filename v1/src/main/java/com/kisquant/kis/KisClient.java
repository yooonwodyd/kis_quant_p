package com.kisquant.kis;

import java.math.BigDecimal;

import com.kisquant.web.CallResult;

public interface KisClient {

	CallResult issueToken();

	CallResult balance();

	CallResult holdings();

	CallResult quote(String symbol);

	CallResult buyable(String symbol, BigDecimal price);

	CallResult limitBuy(BigDecimal price);

	CallResult limitSell(BigDecimal price);

	CallResult marketBuy();

	CallResult marketSell();

	CallResult orderStatus(String kisOrderNo, String krxOrderOrgNo);

	CallResult cancel(String kisOrderNo, String krxOrderOrgNo);

	CallResult todayExecutions();
}
