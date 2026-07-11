package com.parazit.panel.infrastructure.xui.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class XuiAuthenticationManagerTest extends XuiMockServerSupport {

    private final MutableTestClock clock = new MutableTestClock(Instant.parse("2026-01-01T00:00:00Z"));
    private XuiSessionStore sessionStore;

    @Test
    void successfulLoginStoresSessionCookiesAndTimestamp() throws Exception {
        server.enqueue(successfulLogin("JSESSIONID=abc; Path=/; HttpOnly"));
        XuiAuthenticationManager manager = manager(properties(true, Duration.ofMinutes(30), "admin", "secret"));

        manager.login();

        assertThat(manager.isAuthenticated()).isTrue();
        assertThat(sessionStore.get("JSESSIONID")).contains("abc");
        assertThat(sessionStore.lastLoginTime()).contains(clock.instant());
        RecordedRequest request = server.takeRequest(1, TimeUnit.SECONDS);
        assertThat(request.getPath()).isEqualTo("/login");
        assertThat(request.getBody().readUtf8()).isEqualTo("{\"username\":\"admin\",\"password\":\"secret\"}");
    }

    @Test
    void failedLoginClearsExistingSession() {
        sessionStore = new XuiSessionStore();
        sessionStore.store("JSESSIONID", "old");
        server.enqueue(jsonResponse(200, "{\"success\":false,\"msg\":\"bad\"}"));
        XuiAuthenticationManager manager = manager(
                properties(true, Duration.ofMinutes(30), "admin", "wrong"),
                sessionStore
        );

        assertThatThrownBy(manager::login).isInstanceOf(XuiAuthenticationException.class);

        assertThat(sessionStore.isPresent()).isFalse();
        assertThat(server.getRequestCount()).isEqualTo(1);
    }

    @Test
    void invalidCredentialsFailWithoutHttpRequest() {
        XuiAuthenticationManager manager = manager(properties(true, Duration.ofMinutes(30), "", ""));

        assertThatThrownBy(manager::login).isInstanceOf(XuiAuthenticationException.class);

        assertThat(server.getRequestCount()).isZero();
    }

    @Test
    void logoutClearsSessionState() {
        server.enqueue(successfulLogin("JSESSIONID=abc; Path=/"));
        XuiAuthenticationManager manager = manager(properties(true, Duration.ofMinutes(30), "admin", "secret"));
        manager.login();

        manager.logout();

        assertThat(manager.isAuthenticated()).isFalse();
        assertThat(sessionStore.isPresent()).isFalse();
        assertThat(sessionStore.lastLoginTime()).isEmpty();
    }

    @Test
    void ensureAuthenticatedReusesExistingValidSession() {
        sessionStore = new XuiSessionStore();
        sessionStore.store("JSESSIONID", "abc");
        sessionStore.markLoggedIn(clock.instant());
        XuiAuthenticationManager manager = manager(
                properties(true, Duration.ofMinutes(30), "admin", "secret"),
                sessionStore
        );

        manager.ensureAuthenticated();

        assertThat(server.getRequestCount()).isZero();
    }

    @Test
    void ensureAuthenticatedLogsInWhenSessionTimedOut() {
        sessionStore = new XuiSessionStore();
        sessionStore.store("JSESSIONID", "old");
        sessionStore.markLoggedIn(clock.instant());
        clock.setInstant(Instant.parse("2026-01-01T00:31:00Z"));
        server.enqueue(successfulLogin("JSESSIONID=new; Path=/"));
        XuiAuthenticationManager manager = manager(
                properties(true, Duration.ofMinutes(30), "admin", "secret"),
                sessionStore
        );

        manager.ensureAuthenticated();

        assertThat(sessionStore.get("JSESSIONID")).contains("new");
        assertThat(server.getRequestCount()).isEqualTo(1);
    }

    @Test
    void autoLoginDisabledRejectsEnsureButAllowsExplicitLogin() {
        server.enqueue(successfulLogin("JSESSIONID=abc; Path=/"));
        XuiAuthenticationManager manager = manager(properties(false, Duration.ofMinutes(30), "admin", "secret"));

        assertThatThrownBy(manager::ensureAuthenticated).isInstanceOf(XuiAuthenticationException.class);
        assertThat(server.getRequestCount()).isZero();

        manager.login();

        assertThat(manager.isAuthenticated()).isTrue();
        assertThat(server.getRequestCount()).isEqualTo(1);
    }

    @Test
    void concurrentEnsureAuthenticatedPerformsExactlyOneLogin() throws Exception {
        for (int index = 0; index < 50; index++) {
            server.enqueue(successfulLogin("JSESSIONID=abc; Path=/"));
        }
        XuiAuthenticationManager manager = manager(properties(true, Duration.ofMinutes(30), "admin", "secret"));
        ExecutorService executor = Executors.newFixedThreadPool(50);
        CountDownLatch ready = new CountDownLatch(50);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        for (int index = 0; index < 50; index++) {
            futures.add(executor.submit(() -> {
                ready.countDown();
                start.await(5, TimeUnit.SECONDS);
                manager.ensureAuthenticated();
                return null;
            }));
        }

        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        for (Future<?> future : futures) {
            future.get(5, TimeUnit.SECONDS);
        }
        executor.shutdown();
        assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

        assertThat(server.getRequestCount()).isEqualTo(1);
        assertThat(sessionStore.get("JSESSIONID")).contains("abc");
    }

    private XuiAuthenticationManager manager(XuiProperties properties) {
        return manager(properties, new XuiSessionStore());
    }

    private XuiAuthenticationManager manager(XuiProperties properties, XuiSessionStore sessionStore) {
        try {
            this.sessionStore = sessionStore;
            RestClient loginRestClient = new XuiRestClientConfiguration()
                    .xuiLoginRestClient(properties, new ObjectMapper());
            return new XuiAuthenticationManager(
                    loginRestClient,
                    properties,
                    sessionStore,
                    new XuiRetryExecutor(0, Duration.ZERO),
                    new XuiExceptionMapper(),
                    clock
            );
        } catch (Exception exception) {
            throw new IllegalStateException("Could not create Xui authentication manager", exception);
        }
    }

    private XuiProperties properties(
            boolean autoLogin,
            Duration sessionTimeout,
            String username,
            String password
    ) {
        return new XuiProperties(
                baseUrl(),
                username,
                password,
                Duration.ofSeconds(1),
                Duration.ofSeconds(2),
                Duration.ofSeconds(1),
                0,
                Duration.ZERO,
                true,
                autoLogin,
                sessionTimeout
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
}
