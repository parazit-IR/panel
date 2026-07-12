package com.parazit.panel.infrastructure.payment.zarinpal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parazit.panel.config.properties.ZarinpalProperties;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;

@Configuration
public class ZarinpalRestClientConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "app.payment.zarinpal", name = "enabled", havingValue = "true")
    public RestClient zarinpalRestClient(ZarinpalProperties properties, ObjectMapper objectMapper)
            throws GeneralSecurityException {
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(
                httpClient(properties)
        );
        return RestClient.builder()
                .baseUrl(properties.apiBaseUrl().toString())
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.USER_AGENT, "panel-zarinpal-client")
                .messageConverters(converters -> {
                    converters.removeIf(MappingJackson2HttpMessageConverter.class::isInstance);
                    converters.add(new MappingJackson2HttpMessageConverter(objectMapper));
                })
                .build();
    }

    private CloseableHttpClient httpClient(ZarinpalProperties properties) throws GeneralSecurityException {
        PoolingHttpClientConnectionManagerBuilder builder = PoolingHttpClientConnectionManagerBuilder.create()
                .setDefaultConnectionConfig(ConnectionConfig.custom()
                        .setConnectTimeout(Timeout.of(properties.connectTimeout()))
                        .setSocketTimeout(Timeout.of(properties.readTimeout()))
                        .build());
        if (!properties.verifySsl()) {
            builder.setSSLSocketFactory(SSLConnectionSocketFactoryBuilder.create()
                    .setSslContext(SSLContexts.custom()
                            .loadTrustMaterial(TrustAllStrategy.INSTANCE)
                            .build())
                    .setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                    .build());
        }
        return HttpClients.custom()
                .setConnectionManager(builder.build())
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setRedirectsEnabled(false)
                        .setResponseTimeout(Timeout.of(properties.readTimeout()))
                        .build())
                .disableAutomaticRetries()
                .build();
    }
}
