package com.kisquant.order.adapter.kis;

import com.kisquant.shared.config.KisQuantProperties;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
class KisRestClientConfiguration {

    @Bean
    RestClient kisRestClient(KisQuantProperties properties) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
        Duration timeout = properties.kis().timeout();
        requestFactory.setReadTimeout(timeout);
        RestClient.Builder builder = RestClient.builder().requestFactory(requestFactory);
        if (properties.kis().baseUrl() != null && !properties.kis().baseUrl().isBlank()) {
            builder.baseUrl(properties.kis().baseUrl());
        }
        return builder.build();
    }
}
