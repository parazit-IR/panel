package com.parazit.panel.infrastructure.payment.zarinpal;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parazit.panel.application.payment.zarinpal.model.ZarinpalCreateRequest;
import com.parazit.panel.application.payment.zarinpal.model.ZarinpalVerifyRequest;
import com.parazit.panel.config.properties.ZarinpalProperties;
import com.parazit.panel.test.xui.XuiMockServerSupport;
import java.net.URI;
import java.time.Duration;
import okhttp3.mockwebserver.MockResponse;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class RestClientZarinpalGatewayClientIntegrationTest extends XuiMockServerSupport {

    @Test
    void createsPaymentAndBuildsStartPayUrl() throws Exception {
        server.enqueue(json(fixture("request-success.json")));
        RestClientZarinpalGatewayClient client = client();

        var response = client.createPayment(new ZarinpalCreateRequest(
                "merchant-id",
                100_000L,
                "IRT",
                "https://example.test/callback",
                "Payment",
                null,
                null
        ));

        assertThat(response.successful()).isTrue();
        assertThat(response.authority()).isEqualTo("A000000000000000000000000000123456");
        assertThat(response.paymentUrl()).endsWith("/A000000000000000000000000000123456");
        assertThat(server.takeRequest().getPath()).isEqualTo("/pg/v4/payment/request.json");
    }

    @Test
    void verifiesSuccessAndAlreadyVerifiedCodes() throws Exception {
        server.enqueue(json(fixture("verify-success.json")));
        RestClientZarinpalGatewayClient client = client();

        var response = client.verifyPayment(new ZarinpalVerifyRequest(
                "merchant-id",
                100_000L,
                "A000000000000000000000000000123456"
        ));

        assertThat(response.successful()).isTrue();
        assertThat(response.referenceId()).isEqualTo("987654321");
        assertThat(response.cardPanMasked()).isEqualTo("502229******5995");
        assertThat(server.takeRequest().getPath()).isEqualTo("/pg/v4/payment/verify.json");
    }

    private RestClientZarinpalGatewayClient client() {
        ZarinpalProperties properties = properties();
        RestClient restClient = RestClient.builder()
                .baseUrl(properties.apiBaseUrl().toString())
                .messageConverters(converters -> converters.add(new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(new ObjectMapper())))
                .build();
        return new RestClientZarinpalGatewayClient(restClient, properties);
    }

    private ZarinpalProperties properties() {
        return new ZarinpalProperties(
                true,
                "merchant-id",
                URI.create(baseUrl()),
                URI.create(baseUrl() + "pg/StartPay"),
                "/pg/v4/payment/request.json",
                "/pg/v4/payment/verify.json",
                URI.create("https://example.test/callback"),
                URI.create("https://example.test/success"),
                URI.create("https://example.test/failed"),
                URI.create("https://example.test/cancelled"),
                Duration.ofSeconds(1),
                Duration.ofSeconds(1),
                0,
                Duration.ofMillis(10),
                false,
                true
        );
    }

    private MockResponse json(String body) {
        return new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(body);
    }

    private String fixture(String name) throws Exception {
        return new String(java.nio.file.Files.readAllBytes(
                java.nio.file.Path.of("src/test/resources/payment/zarinpal/" + name)
        ));
    }
}
