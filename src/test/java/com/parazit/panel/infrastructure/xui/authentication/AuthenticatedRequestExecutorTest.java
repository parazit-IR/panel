package com.parazit.panel.infrastructure.xui.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parazit.panel.infrastructure.xui.XuiRequestExecutor;
import com.parazit.panel.infrastructure.xui.config.XuiProperties;
import com.parazit.panel.infrastructure.xui.config.XuiRestClientConfiguration;
import com.parazit.panel.infrastructure.xui.exception.XuiAuthenticationException;
import com.parazit.panel.infrastructure.xui.exception.XuiExceptionMapper;
import com.parazit.panel.infrastructure.xui.retry.XuiRetryExecutor;
import com.parazit.panel.infrastructure.xui.session.XuiSessionStore;
import com.parazit.panel.test.support.MutableTestClock;
import com.parazit.panel.test.xui.XuiMockServerSupport;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class AuthenticatedRequestExecutorTest extends XuiMockServerSupport {

    private final MutableTestClock clock = new MutableTestClock(Instant.parse("2026-01-01T00:00:00Z"));

    @Test
    void authenticatedRequestLogsInAndAttachesCookies() throws Exception {
        server.enqueue(successfulLogin("JSESSIONID=abc; Path=/"));
        server.enqueue(textResponse(200, "ok"));
        AuthenticatedRequestExecutor executor = executor(properties(true));

        String response = executor.get("/", String.class);

        assertThat(response).isEqualTo("ok");
        RecordedRequest login = server.takeRequest(1, TimeUnit.SECONDS);
        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
        assertThat(login.getPath()).isEqualTo("/login");
        assertThat(request.getPath()).isEqualTo("/");
        assertThat(request.getHeader("Cookie")).isEqualTo("JSESSIONID=abc");
    }

    @Test
    void expiredSessionTriggersOneAutomaticReloginAndRetry() throws Exception {
        server.enqueue(successfulLogin("JSESSIONID=old; Path=/"));
        server.enqueue(textResponse(401, "expired"));
        server.enqueue(successfulLogin("JSESSIONID=new; Path=/"));
        server.enqueue(textResponse(200, "ok"));
        AuthenticatedRequestExecutor executor = executor(properties(true));

        String response = executor.get("/", String.class);

        assertThat(response).isEqualTo("ok");
        RecordedRequest initialLogin = server.takeRequest(1, TimeUnit.SECONDS);
        RecordedRequest expiredRequest = server.takeRequest(1, TimeUnit.SECONDS);
        RecordedRequest refreshLogin = server.takeRequest(1, TimeUnit.SECONDS);
        RecordedRequest retriedRequest = server.takeRequest(1, TimeUnit.SECONDS);
        assertThat(initialLogin.getPath()).isEqualTo("/login");
        assertThat(expiredRequest.getHeader("Cookie")).isEqualTo("JSESSIONID=old");
        assertThat(refreshLogin.getPath()).isEqualTo("/login");
        assertThat(retriedRequest.getHeader("Cookie")).isEqualTo("JSESSIONID=new");
        assertThat(server.getRequestCount()).isEqualTo(4);
    }

    @Test
    void loginRedirectTriggersAutomaticReloginAndRetry() throws Exception {
        server.enqueue(successfulLogin("JSESSIONID=old; Path=/"));
        server.enqueue(new MockResponse()
                .setResponseCode(302)
                .setHeader("Location", "/login")
                .setBody("redirect"));
        server.enqueue(successfulLogin("JSESSIONID=new; Path=/"));
        server.enqueue(textResponse(200, "ok"));
        AuthenticatedRequestExecutor executor = executor(properties(true));

        assertThat(executor.get("/", String.class)).isEqualTo("ok");

        assertThat(server.getRequestCount()).isEqualTo(4);
    }

    @Test
    void automaticReloginRetriesOnlyOnce() {
        server.enqueue(successfulLogin("JSESSIONID=old; Path=/"));
        server.enqueue(textResponse(401, "expired"));
        server.enqueue(successfulLogin("JSESSIONID=new; Path=/"));
        server.enqueue(textResponse(401, "still expired"));
        AuthenticatedRequestExecutor executor = executor(properties(true));

        assertThatThrownBy(() -> executor.get("/", String.class))
                .isInstanceOf(XuiAuthenticationException.class);
        assertThat(server.getRequestCount()).isEqualTo(4);
    }

    @Test
    void invalidCredentialsAreNotRetried() {
        server.enqueue(jsonResponse(200, "{\"success\":false,\"msg\":\"bad\"}"));
        AuthenticatedRequestExecutor executor = executor(properties(true));

        assertThatThrownBy(() -> executor.get("/", String.class))
                .isInstanceOf(XuiAuthenticationException.class);
        assertThat(server.getRequestCount()).isEqualTo(1);
    }

    @Test
    void autoLoginDisabledRejectsAuthenticatedRequest() {
        AuthenticatedRequestExecutor executor = executor(properties(false));

        assertThatThrownBy(() -> executor.get("/", String.class))
                .isInstanceOf(XuiAuthenticationException.class);
        assertThat(server.getRequestCount()).isZero();
    }

    private AuthenticatedRequestExecutor executor(XuiProperties properties) {
        try {
            XuiRestClientConfiguration configuration = new XuiRestClientConfiguration();
            ObjectMapper objectMapper = new ObjectMapper();
            RestClient restClient = configuration.xuiRestClient(properties, objectMapper);
            RestClient loginRestClient = configuration.xuiLoginRestClient(properties, objectMapper);
            XuiSessionStore sessionStore = new XuiSessionStore();
            XuiRetryExecutor retryExecutor = new XuiRetryExecutor(0, Duration.ZERO);
            XuiExceptionMapper exceptionMapper = new XuiExceptionMapper();
            XuiAuthenticationManager authenticationManager = new XuiAuthenticationManager(
                    loginRestClient,
                    properties,
                    sessionStore,
                    retryExecutor,
                    exceptionMapper,
                    clock
            );
            XuiRequestExecutor requestExecutor = new XuiRequestExecutor(
                    restClient,
                    sessionStore,
                    retryExecutor,
                    exceptionMapper
            );
            return new AuthenticatedRequestExecutor(authenticationManager, requestExecutor, properties);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not create authenticated executor", exception);
        }
    }

    private XuiProperties properties(boolean autoLogin) {
        return new XuiProperties(
                baseUrl(),
                "admin",
                "secret",
                Duration.ofSeconds(1),
                Duration.ofSeconds(2),
                Duration.ofSeconds(1),
                0,
                Duration.ZERO,
                true,
                autoLogin,
                Duration.ofMinutes(30)
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

    private static MockResponse textResponse(int status, String body) {
        return new MockResponse()
                .setResponseCode(status)
                .setHeader("Content-Type", "text/plain")
                .setBody(body);
    }
}
