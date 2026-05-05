package com.kisquant.kis;

import java.math.BigDecimal;

import com.kisquant.web.CallResult;

public interface KisClient {

	CallResult issueToken();

	CallResult balance();

	CallResult holdings();

	CallResult quote(String symbol);

	CallResult buyable(String symbol, BigDecimal price);
}
