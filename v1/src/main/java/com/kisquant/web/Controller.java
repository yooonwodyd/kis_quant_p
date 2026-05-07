package com.kisquant.web;

import java.math.BigDecimal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kisquant.kis.KisClient;
import com.kisquant.order.OrderGuard;

/**
 * 브라우저에서 누르는 테스트 버튼의 진입점.
 * 처음에는 token과 잔고부터 확인한다.
 */
@RestController
@RequestMapping("/api/kis")
final class Controller {

	private final KisClient kisClient;
	private final OrderGuard orderGuard;

	Controller(KisClient kisClient, OrderGuard orderGuard) {
		this.kisClient = kisClient;
		this.orderGuard = orderGuard;
	}

	@GetMapping("/token")
	CallResult token() {
		return this.kisClient.issueToken();
	}

	@GetMapping("/account/balance")
	CallResult balance() {
		return this.kisClient.balance();
	}

	@GetMapping("/account/holdings")
	CallResult holdings() {
		return this.kisClient.holdings();
	}

	@GetMapping("/quotes/{symbol}")
	CallResult quote(@PathVariable String symbol) {
		this.orderGuard.validateSymbol(symbol);
		return this.kisClient.quote(symbol);
	}

	@GetMapping("/orders/buyable")
	CallResult buyable(@RequestParam String symbol, @RequestParam BigDecimal price) {
		this.orderGuard.validateLimitOrder(symbol, this.orderGuard.fixedOrderQuantity(), price);
		return this.kisClient.buyable(symbol, price);
	}
}
