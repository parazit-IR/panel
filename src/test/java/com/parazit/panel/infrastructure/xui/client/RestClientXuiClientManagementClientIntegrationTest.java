package com.parazit.panel.infrastructure.xui.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parazit.panel.application.xui.client.XuiClientCreateRejectedException;
import com.parazit.panel.application.xui.client.XuiClientCreateTimeoutException;
import com.parazit.panel.application.xui.client.model.CreateXuiClientRequest;
import com.parazit.panel.infrastructure.xui.XuiRequestExecutor;
import com.parazit.panel.infrastructure.xui.authentication.AuthenticatedRequestExecutor;
import com.parazit.panel.infrastructure.xui.authentication.XuiAuthenticationManager;
import com.parazit.panel.infrastructure.xui.config.XuiProperties;
import com.parazit.panel.infrastructure.xui.config.XuiRestClientConfiguration;
import com.parazit.panel.infrastructure.xui.exception.XuiExceptionMapper;
import com.parazit.panel.infrastructure.xui.retry.XuiRetryExecutor;
import com.parazit.panel.infrastructure.xui.session.XuiSessionStore;
import com.parazit.panel.test.xui.XuiMockServerSupport;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class RestClientXuiClientManagementClientIntegrationTest extends XuiMockServerSupport {

    @Test
    void postsAuthenticatedCreateClientPayload() throws Exception {
        server.enqueue(successfulLogin("xui_session=abc; Path=/; HttpOnly"));
        server.enqueue(jsonResponse(200, fixture("create-client-success.json")));

        assertThat(client(Duration.ofSeconds(2)).createClient(request()).created()).isTrue();

        RecordedRequest login = server.takeRequest(1, TimeUnit.SECONDS);
        RecordedRequest create = server.takeRequest(1, TimeUnit.SECONDS);
        assertThat(login.getPath()).isEqualTo("/login");
        assertThat(create.getMethod()).isEqualTo("POST");
        assertThat(create.getPath()).isEqualTo("/panel/api/inbounds/addClient");
        assertThat(create.getHeader("Cookie")).isEqualTo("xui_session=abc");
        String body = create.getBody().readUtf8();
        assertThat(body).contains("\"id\":7");
        assertThat(body).contains("\"email\":\"vpn_abc_def\"");
        assertThat(body).doesNotContain("password");
    }

    @Test
    void mapsFailureEnvelopeAndTimeout() {
        server.enqueue(successfulLogin("xui_session=abc; Path=/; HttpOnly"));
        server.enqueue(jsonResponse(200, fixture("create-client-failure.json")));

        assertThatThrownBy(() -> client(Duration.ofSeconds(2)).createClient(request()))
                .isInstanceOf(XuiClientCreateRejectedException.class);

        server.enqueue(successfulLogin("xui_session=abc; Path=/; HttpOnly"));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(fixture("create-client-success.json"))
                .setBodyDelay(1, TimeUnit.SECONDS));

        assertThatThrownBy(() -> client(Duration.ofMillis(100)).createClient(request()))
                .isInstanceOf(XuiClientCreateTimeoutException.class);
    }

    @Test
    void createPostIsNotBlindlyRetriedAfterTimeout() {
        server.enqueue(successfulLogin("xui_session=abc; Path=/; HttpOnly"));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(fixture("create-client-success.json"))
                .setBodyDelay(1, TimeUnit.SECONDS));

        assertThatThrownBy(() -> client(Duration.ofMillis(100)).createClient(request()))
                .isInstanceOf(XuiClientCreateTimeoutException.class);
        assertThat(server.getRequestCount()).isEqualTo(2);
    }

    private RestClientXuiClientManagementClient client(Duration readTimeout) {
        try {
            XuiProperties properties = new XuiProperties(
                    baseUrl(),
                    "admin",
                    "secret",
                    Duration.ofSeconds(1),
                    readTimeout,
                    Duration.ofSeconds(1),
                    3,
                    Duration.ZERO,
                    true,
                    true,
                    Duration.ofMinutes(30),
                    "/panel/api/inbounds/list",
                    "/panel/api/inbounds/addClient",
                    "xtls-rprx-vision",
                    16,
                    1
            );
            ObjectMapper objectMapper = new ObjectMapper();
            XuiRestClientConfiguration configuration = new XuiRestClientConfiguration();
            RestClient restClient = configuration.xuiRestClient(properties, objectMapper);
            RestClient loginRestClient = configuration.xuiLoginRestClient(properties, objectMapper);
            XuiSessionStore sessionStore = new XuiSessionStore();
            XuiRetryExecutor retryExecutor = new XuiRetryExecutor(3, Duration.ZERO);
            XuiExceptionMapper exceptionMapper = new XuiExceptionMapper();
            XuiRequestExecutor requestExecutor = new XuiRequestExecutor(restClient, sessionStore, retryExecutor, exceptionMapper);
            XuiAuthenticationManager authenticationManager = new XuiAuthenticationManager(
                    loginRestClient,
                    properties,
                    sessionStore,
                    retryExecutor,
                    exceptionMapper,
                    Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC)
            );
            return new RestClientXuiClientManagementClient(
                    new AuthenticatedRequestExecutor(authenticationManager, requestExecutor, properties),
                    properties,
                    new XuiCreateClientPayloadBuilder()
            );
        } catch (Exception exception) {
            throw new IllegalStateException("Could not create management client", exception);
        }
    }

    private static CreateXuiClientRequest request() {
        return new CreateXuiClientRequest(
                7,
                "11111111-1111-1111-1111-111111111111",
                "vpn_abc_def",
                "sub123",
                true,
                1024,
                Instant.ofEpochMilli(1893456000000L),
                2,
                "xtls-rprx-vision"
        );
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
                    RestClientXuiClientManagementClientIntegrationTest.class
                            .getResourceAsStream("/xui/clients/" + name)
                            .readAllBytes(),
                    StandardCharsets.UTF_8
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read fixture " + name, exception);
        }
    }
}
