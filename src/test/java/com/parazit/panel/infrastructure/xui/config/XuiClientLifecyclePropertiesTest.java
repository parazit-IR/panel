package com.parazit.panel.infrastructure.xui.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class XuiClientLifecyclePropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class);

    @Test
    void bindsDefaultsAndCustomTemplates() {
        contextRunner.run(context -> {
            XuiClientLifecycleProperties properties = context.getBean(XuiClientLifecycleProperties.class);
            assertThat(properties.clientUpdatePathTemplate()).isEqualTo("/panel/api/inbounds/updateClient/{clientId}");
            assertThat(properties.clientDeletePathTemplate()).isEqualTo("/panel/api/inbounds/{inboundId}/delClient/{clientId}");
        });

        contextRunner.withPropertyValues(
                "app.xui.client-update-path-template=/custom/update/{clientId}",
                "app.xui.client-delete-path-template=/custom/{inboundId}/delete/{clientId}"
        ).run(context -> {
            XuiClientLifecycleProperties properties = context.getBean(XuiClientLifecycleProperties.class);
            assertThat(properties.clientUpdatePathTemplate()).isEqualTo("/custom/update/{clientId}");
            assertThat(properties.clientDeletePathTemplate()).isEqualTo("/custom/{inboundId}/delete/{clientId}");
        });
    }

    @Test
    void rejectsMissingRequiredTemplateVariables() {
        contextRunner.withPropertyValues("app.xui.client-delete-path-template=/custom/delete/{clientId}")
                .run(context -> assertThat(context).hasFailed());
    }

    @EnableConfigurationProperties(XuiClientLifecycleProperties.class)
    static class TestConfiguration {
    }
}
