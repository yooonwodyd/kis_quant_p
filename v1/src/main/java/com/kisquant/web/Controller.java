package com.kisquant.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kisquant.kis.KisClient;

/**
 * 브라우저에서 누르는 테스트 버튼의 진입점.
 * 처음에는 token과 잔고부터 확인한다.
 */
@RestController
@RequestMapping("/api/kis")
final class Controller {

	private final KisClient kisClient;

	Controller(KisClient kisClient) {
		this.kisClient = kisClient;
	}

	@GetMapping("/token")
	CallResult token() {
		return this.kisClient.issueToken();
	}

	@GetMapping("/account/balance")
	CallResult balance() {
		return this.kisClient.balance();
	}
}
