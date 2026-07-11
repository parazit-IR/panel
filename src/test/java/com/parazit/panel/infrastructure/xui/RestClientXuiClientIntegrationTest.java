package com.parazit.panel.infrastructure.xui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parazit.panel.infrastructure.xui.config.XuiProperties;
import com.parazit.panel.infrastructure.xui.config.XuiRestClientConfiguration;
import com.parazit.panel.infrastructure.xui.exception.XuiExceptionMapper;
import com.parazit.panel.infrastructure.xui.exception.XuiServerException;
import com.parazit.panel.infrastructure.xui.exception.XuiTimeoutException;
import com.parazit.panel.infrastructure.xui.retry.XuiRetryExecutor;
import com.parazit.panel.infrastructure.xui.session.XuiSessionStore;
import com.parazit.panel.test.xui.XuiMockServerSupport;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.tls.HandshakeCertificates;
import okhttp3.tls.HeldCertificate;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class RestClientXuiClientIntegrationTest extends XuiMockServerSupport {

    private XuiSessionStore sessionStore;
    private XuiRequestExecutor requestExecutor;

    @Test
    void pingUsesConfiguredHeaders() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/plain")
                .setBody("ok"));
        RestClientXuiClient client = client(0, Duration.ofSeconds(2), true);

        assertThat(client.ping()).isTrue();

        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath()).isEqualTo("/");
        assertThat(request.getHeader("Accept")).contains("application/json");
        assertThat(request.getHeader("User-Agent")).isEqualTo("panel-xui-client");
    }

    @Test
    void storesAndReusesCookiesWithoutInterceptors() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Set-Cookie", "xui_session=abc; Path=/; HttpOnly")
                .setBody("ok"));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("ok"));
        RestClientXuiClient client = client(0, Duration.ofSeconds(2), true);

        assertThat(client.ping()).isTrue();
        assertThat(sessionStore.hasSession()).isTrue();
        assertThat(client.ping()).isTrue();

        server.takeRequest(1, TimeUnit.SECONDS);
        RecordedRequest secondRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertThat(secondRequest).isNotNull();
        assertThat(secondRequest.getHeader("Cookie")).isEqualTo("xui_session=abc");
    }

    @Test
    void serializesJsonThroughRequestExecutor() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"ok\":true}"));
        client(0, Duration.ofSeconds(2), true);

        String response = requestExecutor.post("/json", new SamplePayload("alpha"), String.class);

        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
        assertThat(response).isEqualTo("{\"ok\":true}");
        assertThat(request).isNotNull();
        assertThat(request.getHeader("Content-Type")).contains("application/json");
        assertThat(request.getBody().readUtf8()).isEqualTo("{\"name\":\"alpha\"}");
    }

    @Test
    void retriesTransientServerFailures() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(503).setBody("unavailable"));
        server.enqueue(new MockResponse().setResponseCode(200).setBody("ok"));
        RestClientXuiClient client = client(1, Duration.ofSeconds(2), true);

        assertThat(client.ping()).isTrue();

        assertThat(server.getRequestCount()).isEqualTo(2);
    }

    @Test
    void doesNotRetryNonTransientServerFailures() {
        server.enqueue(new MockResponse().setResponseCode(500).setBody("failure"));
        RestClientXuiClient client = client(3, Duration.ofSeconds(2), true);

        assertThatThrownBy(client::ping).isInstanceOf(XuiServerException.class);
        assertThat(server.getRequestCount()).isEqualTo(1);
    }

    @Test
    void mapsReadTimeouts() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/plain")
                .setBody("slow")
                .setBodyDelay(1, TimeUnit.SECONDS));
        RestClientXuiClient client = client(0, Duration.ofMillis(100), true);

        assertThatThrownBy(client::ping).isInstanceOf(XuiTimeoutException.class);
    }

    @Test
    void supportsSelfSignedSslWhenVerificationIsDisabled() throws Exception {
        server.shutdown();
        HeldCertificate certificate = new HeldCertificate.Builder()
                .commonName("localhost")
                .build();
        HandshakeCertificates serverCertificates = new HandshakeCertificates.Builder()
                .heldCertificate(certificate)
                .build();
        server = new okhttp3.mockwebserver.MockWebServer();
        server.useHttps(serverCertificates.sslSocketFactory(), false);
        server.start();
        server.enqueue(new MockResponse().setResponseCode(200).setBody("ok"));
        RestClientXuiClient client = client(0, Duration.ofSeconds(2), false);

        assertThat(client.ping()).isTrue();
    }

    @Test
    void unsupportedOperationsRemainOutOfScope() {
        RestClientXuiClient client = client(0, Duration.ofSeconds(2), true);

        assertThatThrownBy(client::login).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(client::getInbounds).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(client::createClient).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(client::updateClient).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(client::deleteClient).isInstanceOf(UnsupportedOperationException.class);
    }

    private RestClientXuiClient client(int maxRetries, Duration readTimeout, boolean verifySsl) {
        try {
            XuiProperties properties = new XuiProperties(
                    baseUrl(),
                    "",
                    "",
                    Duration.ofSeconds(1),
                    readTimeout,
                    maxRetries,
                    Duration.ZERO,
                    verifySsl
            );
            RestClient restClient = new XuiRestClientConfiguration()
                    .xuiRestClient(properties, new ObjectMapper());
            sessionStore = new XuiSessionStore();
            requestExecutor = new XuiRequestExecutor(
                    restClient,
                    sessionStore,
                    new XuiRetryExecutor(maxRetries, Duration.ZERO),
                    new XuiExceptionMapper()
            );
            return new RestClientXuiClient(requestExecutor);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not create Xui test client", exception);
        }
    }

    private record SamplePayload(String name) {
    }
}
