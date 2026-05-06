package com.kisquant.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.kisquant.kis.KisProperties;
import com.kisquant.order.OrderGuard;
import com.kisquant.order.OrderProperties;
import com.kisquant.support.SensitiveMasker;

@Configuration
class ApiConfiguration {

	@Bean
	RestClient kisRestClient(KisProperties properties) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(properties.timeout());
		requestFactory.setReadTimeout(properties.timeout());
		return RestClient.builder()
				.baseUrl(properties.baseUrl())
				.requestFactory(requestFactory)
				.build();
	}

	@Bean
	OrderGuard orderGuard(OrderProperties properties) {
		return new OrderGuard(properties.allowedSymbol(), properties.maxOrderKrw(), properties.fixedOrderQuantity());
	}

	@Bean
	SensitiveMasker sensitiveMasker(KisProperties properties) {
		return new SensitiveMasker(properties.accountNumber(), properties.accountProductCode());
	}
}
