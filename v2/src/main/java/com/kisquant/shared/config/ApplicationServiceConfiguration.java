package com.kisquant.shared.config;

import com.kisquant.shared.domain.Symbol;
import com.kisquant.shared.time.CurrentTimeProvider;
import java.time.Instant;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ApplicationServiceConfiguration {

    @Bean
    CurrentTimeProvider currentTimeProvider() {
        return Instant::now;
    }

    @Bean
    List<Symbol> allowedSymbols(KisQuantProperties properties) {
        return properties.trading().allowedSymbols().stream()
                .map(Symbol::of)
                .toList();
    }
}
