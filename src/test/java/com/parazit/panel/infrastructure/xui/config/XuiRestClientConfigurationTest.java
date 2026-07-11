package com.parazit.panel.infrastructure.xui.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parazit.panel.infrastructure.xui.exception.XuiExceptionMapper;
import com.parazit.panel.infrastructure.xui.retry.XuiRetryExecutor;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class XuiRestClientConfigurationTest {

    private final XuiRestClientConfiguration configuration = new XuiRestClientConfiguration();

    @Test
    void createsRestClientAndSupportBeans() throws Exception {
        XuiProperties properties = new XuiProperties(
                "http://localhost:2053",
                "",
                "",
                Duration.ofSeconds(1),
                Duration.ofSeconds(2),
                2,
                Duration.ZERO,
                true
        );

        RestClient restClient = configuration.xuiRestClient(properties, new ObjectMapper());
        XuiRetryExecutor retryExecutor = configuration.xuiRetryExecutor(properties);
        XuiExceptionMapper exceptionMapper = configuration.xuiExceptionMapper();

        assertThat(restClient).isNotNull();
        assertThat(retryExecutor).isNotNull();
        assertThat(exceptionMapper).isNotNull();
    }

    @Test
    void createsRestClientWhenSslVerificationIsDisabled() throws Exception {
        XuiProperties properties = new XuiProperties(
                "https://localhost:2053",
                "",
                "",
                Duration.ofSeconds(1),
                Duration.ofSeconds(2),
                0,
                Duration.ZERO,
                false
        );

        RestClient restClient = configuration.xuiRestClient(properties, new ObjectMapper());

        assertThat(restClient).isNotNull();
    }
}
