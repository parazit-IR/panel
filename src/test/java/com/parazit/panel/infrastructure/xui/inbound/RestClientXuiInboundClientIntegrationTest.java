package com.parazit.panel.infrastructure.xui.inbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parazit.panel.application.xui.model.XuiInboundSnapshot;
import com.parazit.panel.infrastructure.xui.XuiRequestExecutor;
import com.parazit.panel.infrastructure.xui.authentication.AuthenticatedRequestExecutor;
import com.parazit.panel.infrastructure.xui.authentication.XuiAuthenticationManager;
import com.parazit.panel.infrastructure.xui.config.XuiProperties;
import com.parazit.panel.infrastructure.xui.config.XuiRestClientConfiguration;
import com.parazit.panel.infrastructure.xui.exception.XuiExceptionMapper;
import com.parazit.panel.infrastructure.xui.exception.XuiInvalidResponseException;
import com.parazit.panel.infrastructure.xui.exception.XuiTimeoutException;
import com.parazit.panel.infrastructure.xui.retry.XuiRetryExecutor;
import com.parazit.panel.infrastructure.xui.session.XuiSessionStore;
import com.parazit.panel.test.xui.XuiMockServerSupport;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class RestClientXuiInboundClientIntegrationTest extends XuiMockServerSupport {

    @Test
    void retrievesAuthenticatedInboundListAndReusesCookie() throws Exception {
        server.enqueue(successfulLogin("xui_session=abc; Path=/; HttpOnly"));
        server.enqueue(jsonResponse(200, fixture("list-inbounds-success.json")));
        RestClientXuiInboundClient client = client(0, Duration.ofSeconds(2));

        List<XuiInboundSnapshot> inbounds = client.getInbounds();

        assertThat(inbounds).hasSize(2);
        assertThat(inbounds.getFirst().id()).isEqualTo(7);
        assertThat(inbounds.getFirst().protocol()).isEqualTo("VLESS");
        assertThat(inbounds.getFirst().securityType()).isEqualTo("REALITY");
        assertThat(inbounds.getFirst().publicKey()).isEqualTo("PUBLIC_KEY_TEST");
        assertThat(inbounds.getFirst().toString()).doesNotContain("PRIVATE_KEY");

        RecordedRequest login = server.takeRequest(1, TimeUnit.SECONDS);
        RecordedRequest list = server.takeRequest(1, TimeUnit.SECONDS);
        assertThat(login).isNotNull();
        assertThat(login.getPath()).isEqualTo("/login");
        assertThat(list).isNotNull();
        assertThat(list.getMethod()).isEqualTo("GET");
        assertThat(list.getPath()).isEqualTo("/panel/api/inbounds/list");
        assertThat(list.getHeader("Cookie")).isEqualTo("xui_session=abc");
    }

    @Test
    void returnsEmptyListForEmptySuccessfulEnvelope() {
        server.enqueue(successfulLogin("xui_session=abc; Path=/; HttpOnly"));
        server.enqueue(jsonResponse(200, fixture("list-inbounds-empty.json")));

        assertThat(client(0, Duration.ofSeconds(2)).getInbounds()).isEmpty();
    }

    @Test
    void mapsFailureEnvelopeAndMalformedPayloadToInvalidResponse() {
        server.enqueue(successfulLogin("xui_session=abc; Path=/; HttpOnly"));
        server.enqueue(jsonResponse(200, fixture("list-inbounds-failure.json")));

        assertThatThrownBy(() -> client(0, Duration.ofSeconds(2)).getInbounds())
                .isInstanceOf(XuiInvalidResponseException.class);

        server.enqueue(successfulLogin("xui_session=abc; Path=/; HttpOnly"));
        server.enqueue(jsonResponse(200, fixture("list-inbounds-malformed-settings.json")));

        assertThatThrownBy(() -> client(0, Duration.ofSeconds(2)).getInbounds())
                .isInstanceOf(XuiInvalidResponseException.class);
    }

    @Test
    void relogsInOnceWhenSessionExpires() throws Exception {
        server.enqueue(successfulLogin("xui_session=old; Path=/; HttpOnly"));
        server.enqueue(jsonResponse(401, "{\"success\":false}"));
        server.enqueue(successfulLogin("xui_session=new; Path=/; HttpOnly"));
        server.enqueue(jsonResponse(200, fixture("list-inbounds-vless-reality.json")));

        List<XuiInboundSnapshot> inbounds = client(0, Duration.ofSeconds(2)).getInbounds();

        assertThat(inbounds).hasSize(1);
        assertThat(server.getRequestCount()).isEqualTo(4);
        server.takeRequest(1, TimeUnit.SECONDS);
        server.takeRequest(1, TimeUnit.SECONDS);
        server.takeRequest(1, TimeUnit.SECONDS);
        RecordedRequest retriedList = server.takeRequest(1, TimeUnit.SECONDS);
        assertThat(retriedList).isNotNull();
        assertThat(retriedList.getHeader("Cookie")).isEqualTo("xui_session=new");
    }

    @Test
    void mapsTimeout() {
        server.enqueue(successfulLogin("xui_session=abc; Path=/; HttpOnly"));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(fixture("list-inbounds-empty.json"))
                .setBodyDelay(1, TimeUnit.SECONDS));

        assertThatThrownBy(() -> client(0, Duration.ofMillis(100)).getInbounds())
                .isInstanceOf(XuiTimeoutException.class);
    }

    private RestClientXuiInboundClient client(int maxRetries, Duration readTimeout) {
        try {
            XuiProperties properties = new XuiProperties(
                    baseUrl(),
                    "admin",
                    "secret",
                    Duration.ofSeconds(1),
                    readTimeout,
                    Duration.ofSeconds(1),
                    maxRetries,
                    Duration.ZERO,
                    true,
                    true,
                    Duration.ofMinutes(30),
                    "/panel/api/inbounds/list"
            );
            ObjectMapper objectMapper = new ObjectMapper();
            XuiRestClientConfiguration configuration = new XuiRestClientConfiguration();
            RestClient restClient = configuration.xuiRestClient(properties, objectMapper);
            RestClient loginRestClient = configuration.xuiLoginRestClient(properties, objectMapper);
            XuiSessionStore sessionStore = new XuiSessionStore();
            XuiRetryExecutor retryExecutor = new XuiRetryExecutor(maxRetries, Duration.ZERO);
            XuiExceptionMapper exceptionMapper = new XuiExceptionMapper();
            XuiRequestExecutor requestExecutor = new XuiRequestExecutor(
                    restClient,
                    sessionStore,
                    retryExecutor,
                    exceptionMapper
            );
            XuiAuthenticationManager authenticationManager = new XuiAuthenticationManager(
                    loginRestClient,
                    properties,
                    sessionStore,
                    retryExecutor,
                    exceptionMapper,
                    Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC)
            );
            AuthenticatedRequestExecutor authenticatedRequestExecutor = new AuthenticatedRequestExecutor(
                    authenticationManager,
                    requestExecutor,
                    properties
            );
            return new RestClientXuiInboundClient(
                    authenticatedRequestExecutor,
                    properties,
                    new XuiInboundMapper(new XuiInboundPayloadParser(objectMapper))
            );
        } catch (Exception exception) {
            throw new IllegalStateException("Could not create Xui inbound test client", exception);
        }
    }

    private static MockResponse successfulLogin(String setCookie) {
        return jsonResponse(200, "{\"success\":true,\"msg\":\"ok\"}")
                .setHeader("Set-Cookie", setCookie);
    }

    private static MockResponse jsonResponse(int status, String body) {
        return new MockResponse()
                .setResponseCode(status)
                .setHeader("Content-Type", "application/json")
                .setBody(body);
    }

    private static String fixture(String name) {
        try {
            return new String(
                    RestClientXuiInboundClientIntegrationTest.class
                            .getResourceAsStream("/xui/inbounds/" + name)
                            .readAllBytes(),
                    StandardCharsets.UTF_8
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read fixture " + name, exception);
        }
    }
}
