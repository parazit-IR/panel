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
                        "app.xui.login-timeout=4s",
                        "app.xui.max-retries=2",
                        "app.xui.retry-delay=250ms",
                        "app.xui.verify-ssl=false",
                        "app.xui.auto-login=false",
                        "app.xui.session-timeout=PT10M",
                        "app.xui.inbound-list-path=/custom/api/inbounds",
                        "app.xui.client-create-path=/custom/api/addClient",
                        "app.xui.client-default-flow=",
                        "app.xui.subscription-id-length=24",
                        "app.xui.client-reconciliation-attempts=2"
                )
                .run(context -> {
                    XuiProperties properties = context.getBean(XuiProperties.class);

                    assertThat(properties.baseUrl()).isEqualTo("https://xui.example.test");
                    assertThat(properties.username()).isEqualTo("admin");
                    assertThat(properties.password()).isEqualTo("secret");
                    assertThat(properties.connectTimeout()).isEqualTo(Duration.ofSeconds(3));
                    assertThat(properties.readTimeout()).isEqualTo(Duration.ofSeconds(9));
                    assertThat(properties.loginTimeout()).isEqualTo(Duration.ofSeconds(4));
                    assertThat(properties.maxRetries()).isEqualTo(2);
                    assertThat(properties.retryDelay()).isEqualTo(Duration.ofMillis(250));
                    assertThat(properties.verifySsl()).isFalse();
                    assertThat(properties.autoLogin()).isFalse();
                    assertThat(properties.sessionTimeout()).isEqualTo(Duration.ofMinutes(10));
                    assertThat(properties.inboundListPath()).isEqualTo("/custom/api/inbounds");
                    assertThat(properties.clientCreatePath()).isEqualTo("/custom/api/addClient");
                    assertThat(properties.clientDefaultFlow()).isEqualTo("");
                    assertThat(properties.subscriptionIdLength()).isEqualTo(24);
                    assertThat(properties.clientReconciliationAttempts()).isEqualTo(2);
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
                    assertThat(properties.loginTimeout()).isEqualTo(Duration.ofSeconds(10));
                    assertThat(properties.retryDelay()).isEqualTo(Duration.ofSeconds(1));
                    assertThat(properties.autoLogin()).isTrue();
                    assertThat(properties.sessionTimeout()).isNull();
                    assertThat(properties.inboundListPath()).isEqualTo("/panel/api/inbounds/list");
                    assertThat(properties.clientCreatePath()).isEqualTo("/panel/api/inbounds/addClient");
                    assertThat(properties.clientDefaultFlow()).isEqualTo("xtls-rprx-vision");
                    assertThat(properties.subscriptionIdLength()).isEqualTo(16);
                    assertThat(properties.clientReconciliationAttempts()).isEqualTo(1);
                });
    }

    @Test
    void rejectsAbsoluteInboundListPath() {
        contextRunner
                .withPropertyValues(
                        "app.xui.base-url=http://localhost:2053",
                        "app.xui.inbound-list-path=http://localhost/panel/api/inbounds/list"
                )
                .run(context -> assertThat(context).hasFailed());
    }

    @EnableConfigurationProperties(XuiProperties.class)
    static class PropertiesConfiguration {
    }
}
