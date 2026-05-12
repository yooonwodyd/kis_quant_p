package com.kisquant.shared.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class KisQuantPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfig.class)
            .withPropertyValues(
                    "kis-quant.trading.allowed-symbols=001510,005930",
                    "kis-quant.trading.strategy-initial-budget-krw=30000",
                    "kis-quant.trading.max-order-krw=30000",
                    "kis-quant.trading.max-daily-order-krw=30000",
                    "kis-quant.startup-sync.enabled=true",
                    "kis-quant.polling.enabled=true",
                    "kis-quant.polling.interval=10s",
                    "kis-quant.polling.batch-size=20",
                    "kis-quant.operation-logs.capacity=1000",
                    "kis-quant.kis.base-url=https://openapi.koreainvestment.com:9443",
                    "kis-quant.kis.timeout=5s");

    @Test
    void bindsTradingAndKisConfiguration() {
        contextRunner.run(context -> {
            KisQuantProperties properties = context.getBean(KisQuantProperties.class);

            assertThat(properties.trading().allowedSymbols()).containsExactly("001510", "005930");
            assertThat(properties.trading().strategyInitialBudgetKrw()).isEqualTo(30_000L);
            assertThat(properties.startupSync().enabled()).isTrue();
            assertThat(properties.polling().batchSize()).isEqualTo(20);
            assertThat(properties.operationLogs().capacity()).isEqualTo(1_000);
            assertThat(properties.kis().timeout()).isEqualTo(Duration.ofSeconds(5));
        });
    }

    @Test
    void applicationYamlUsesRequiredExternalConfigurationWithoutInlineDefaults() throws Exception {
        String applicationYaml = Files.readString(Path.of("src/main/resources/application.yml"));

        assertThat(applicationYaml).contains("import: optional:file:./src/main/resources/p_env.properties");
        assertThat(applicationYaml).contains("url: ${QUANT_DB_URL}");
        assertThat(applicationYaml).contains("enabled: ${KIS_QUANT_STARTUP_SYNC_ENABLED}");
        assertThat(applicationYaml).contains("enabled: ${KIS_QUANT_POLLING_ENABLED}");
        assertThat(applicationYaml).contains("capacity: ${KIS_QUANT_OPERATION_LOG_CAPACITY}");
        assertThat(applicationYaml).doesNotContain(":jdbc:mysql://localhost");
        assertThat(applicationYaml).doesNotContain(":30000}");
        assertThat(applicationYaml).doesNotContain(":true}");
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(KisQuantProperties.class)
    static class PropertiesConfig {
    }
}
