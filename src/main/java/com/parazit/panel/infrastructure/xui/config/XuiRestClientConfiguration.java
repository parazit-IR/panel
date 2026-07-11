package com.parazit.panel.infrastructure.xui.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parazit.panel.infrastructure.xui.XuiLoggingInterceptor;
import com.parazit.panel.infrastructure.xui.exception.XuiExceptionMapper;
import com.parazit.panel.infrastructure.xui.retry.XuiRetryExecutor;
import java.security.GeneralSecurityException;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;

@Configuration
public class XuiRestClientConfiguration {

    @Bean
    public RestClient xuiRestClient(XuiProperties properties, ObjectMapper objectMapper)
            throws GeneralSecurityException {
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(
                httpClient(properties)
        );

        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.USER_AGENT, "panel-xui-client")
                .messageConverters(converters -> {
                    converters.removeIf(MappingJackson2HttpMessageConverter.class::isInstance);
                    converters.add(new MappingJackson2HttpMessageConverter(objectMapper));
                })
                .requestInterceptor(new XuiLoggingInterceptor())
                .build();
    }

    @Bean
    public XuiRetryExecutor xuiRetryExecutor(XuiProperties properties) {
        return new XuiRetryExecutor(properties.maxRetries(), properties.retryDelay());
    }

    @Bean
    public XuiExceptionMapper xuiExceptionMapper() {
        return new XuiExceptionMapper();
    }

    private CloseableHttpClient httpClient(XuiProperties properties) throws GeneralSecurityException {
        PoolingHttpClientConnectionManagerBuilder connectionManagerBuilder =
                PoolingHttpClientConnectionManagerBuilder.create()
                        .setDefaultConnectionConfig(ConnectionConfig.custom()
                                .setConnectTimeout(Timeout.of(properties.connectTimeout()))
                                .setSocketTimeout(Timeout.of(properties.readTimeout()))
                                .build());

        if (!properties.verifySsl()) {
            connectionManagerBuilder.setSSLSocketFactory(SSLConnectionSocketFactoryBuilder.create()
                    .setSslContext(SSLContexts.custom()
                            .loadTrustMaterial(TrustAllStrategy.INSTANCE)
                            .build())
                    .setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                    .build());
        }

        return HttpClients.custom()
                .setConnectionManager(connectionManagerBuilder.build())
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setResponseTimeout(Timeout.of(properties.readTimeout()))
                        .build())
                .disableAutomaticRetries()
                .build();
    }
}
