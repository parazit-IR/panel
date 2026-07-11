package com.parazit.panel.config.properties;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class XuiClientProvisioningPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class);

    @Test
    void bindsProvisioningPropertiesWithDefaults() {
        contextRunner.run(context -> {
            XuiClientProvisioningProperties properties = context.getBean(XuiClientProvisioningProperties.class);

            assertThat(properties.defaultFlow()).isEqualTo("xtls-rprx-vision");
            assertThat(properties.reconciliationAttempts()).isEqualTo(1);
        });
    }

    @Test
    void validatesReconciliationAttempts() {
        contextRunner
                .withPropertyValues("app.xui-client-provisioning.reconciliation-attempts=4")
                .run(context -> assertThat(context).hasFailed());
    }

    @EnableConfigurationProperties(XuiClientProvisioningProperties.class)
    static class TestConfiguration {
    }
}
