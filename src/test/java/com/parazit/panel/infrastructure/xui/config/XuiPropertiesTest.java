package com.parazit.panel.infrastructure.xui.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class XuiPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfiguration.class);

    @Test
    void bindsXuiPropertiesFromConfiguration() {
        contextRunner
                .withPropertyValues(
                        "app.xui.base-url=https://xui.example.test/",
                        "app.xui.username=admin",
                        "app.xui.password=secret",
                        "app.xui.connect-timeout=3s",
                        "app.xui.read-timeout=9s",
                        "app.xui.max-retries=2",
                        "app.xui.retry-delay=250ms",
                        "app.xui.verify-ssl=false"
                )
                .run(context -> {
                    XuiProperties properties = context.getBean(XuiProperties.class);

                    assertThat(properties.baseUrl()).isEqualTo("https://xui.example.test");
                    assertThat(properties.username()).isEqualTo("admin");
                    assertThat(properties.password()).isEqualTo("secret");
                    assertThat(properties.connectTimeout()).isEqualTo(Duration.ofSeconds(3));
                    assertThat(properties.readTimeout()).isEqualTo(Duration.ofSeconds(9));
                    assertThat(properties.maxRetries()).isEqualTo(2);
                    assertThat(properties.retryDelay()).isEqualTo(Duration.ofMillis(250));
                    assertThat(properties.verifySsl()).isFalse();
                });
    }

    @Test
    void rejectsInvalidBaseUrl() {
        contextRunner
                .withPropertyValues("app.xui.base-url=localhost:2053")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void rejectsNegativeRetryConfiguration() {
        contextRunner
                .withPropertyValues(
                        "app.xui.base-url=http://localhost:2053",
                        "app.xui.max-retries=-1"
                )
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void appliesSafeDefaultsWhenOptionalValuesAreAbsent() {
        contextRunner
                .withPropertyValues("app.xui.base-url=http://localhost:2053")
                .run(context -> {
                    XuiProperties properties = context.getBean(XuiProperties.class);

                    assertThat(properties.connectTimeout()).isEqualTo(Duration.ofSeconds(5));
                    assertThat(properties.readTimeout()).isEqualTo(Duration.ofSeconds(20));
                    assertThat(properties.retryDelay()).isEqualTo(Duration.ofSeconds(1));
                });
    }

    @EnableConfigurationProperties(XuiProperties.class)
    static class PropertiesConfiguration {
    }
}
