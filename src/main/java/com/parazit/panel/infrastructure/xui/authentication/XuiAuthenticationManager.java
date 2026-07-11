package com.parazit.panel.infrastructure.xui.authentication;

import com.parazit.panel.infrastructure.xui.config.XuiProperties;
import com.parazit.panel.infrastructure.xui.exception.XuiAuthenticationException;
import com.parazit.panel.infrastructure.xui.exception.XuiException;
import com.parazit.panel.infrastructure.xui.exception.XuiExceptionMapper;
import com.parazit.panel.infrastructure.xui.retry.XuiRetryExecutor;
import com.parazit.panel.infrastructure.xui.session.XuiSessionStore;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class XuiAuthenticationManager {

    private static final Logger log = LoggerFactory.getLogger(XuiAuthenticationManager.class);

    private final RestClient loginRestClient;
    private final XuiProperties properties;
    private final XuiSessionStore sessionStore;
    private final XuiRetryExecutor retryExecutor;
    private final XuiExceptionMapper exceptionMapper;
    private final Clock clock;
    private final ReentrantLock loginLock = new ReentrantLock();

    public XuiAuthenticationManager(
            @Qualifier("xuiLoginRestClient") RestClient loginRestClient,
            XuiProperties properties,
            XuiSessionStore sessionStore,
            XuiRetryExecutor retryExecutor,
            XuiExceptionMapper exceptionMapper,
            Clock clock
    ) {
        this.loginRestClient = Objects.requireNonNull(loginRestClient, "loginRestClient must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.sessionStore = Objects.requireNonNull(sessionStore, "sessionStore must not be null");
        this.retryExecutor = Objects.requireNonNull(retryExecutor, "retryExecutor must not be null");
        this.exceptionMapper = Objects.requireNonNull(exceptionMapper, "exceptionMapper must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public void ensureAuthenticated() {
        if (isAuthenticated()) {
            return;
        }
        if (!Boolean.TRUE.equals(properties.autoLogin())) {
            throw new XuiAuthenticationException("Xui auto-login is disabled");
        }
        login();
    }

    public void login() {
        if (isAuthenticated()) {
            return;
        }
        loginLock.lock();
        try {
            if (isAuthenticated()) {
                return;
            }
            loginLocked();
        } finally {
            loginLock.unlock();
        }
    }

    public void refreshSession() {
        loginLock.lock();
        try {
            sessionStore.clear();
            log.info("Refreshing Xui session");
            loginLocked();
        } finally {
            loginLock.unlock();
        }
    }

    public void logout() {
        sessionStore.clear();
        log.info("Xui session cleared");
    }

    public boolean isAuthenticated() {
        if (!sessionStore.isPresent()) {
            return false;
        }
        Duration timeout = properties.sessionTimeout();
        if (timeout == null) {
            return true;
        }
        return sessionStore.lastLoginTime()
                .map(lastLogin -> lastLogin.plus(timeout).isAfter(clock.instant()))
                .orElse(false);
    }

    private void loginLocked() {
        validateCredentials();
        long startedAt = System.nanoTime();
        try {
            ResponseEntity<XuiLoginResponse> response = retryExecutor.execute(() -> {
                try {
                    return loginRestClient.post()
                            .uri("/login")
                            .body(new XuiLoginRequest(properties.username(), properties.password()))
                            .retrieve()
                            .toEntity(XuiLoginResponse.class);
                } catch (RuntimeException exception) {
                    throw exceptionMapper.map(exception);
                }
            });

            XuiLoginResponse body = response.getBody();
            if (body == null || !Boolean.TRUE.equals(body.success())) {
                sessionStore.clear();
                log.info("Xui login failed durationMs={}", elapsedMillis(startedAt));
                throw new XuiAuthenticationException("Xui login failed");
            }

            List<String> setCookieHeaders = response.getHeaders().get(HttpHeaders.SET_COOKIE);
            if (setCookieHeaders == null || setCookieHeaders.isEmpty()) {
                sessionStore.clear();
                log.info("Xui login failed durationMs={}", elapsedMillis(startedAt));
                throw new XuiAuthenticationException("Xui login did not return a session");
            }

            sessionStore.clear();
            sessionStore.storeFromSetCookieHeaders(setCookieHeaders);
            sessionStore.markLoggedIn(Instant.now(clock));
            log.info("Xui login succeeded durationMs={}", elapsedMillis(startedAt));
        } catch (XuiException exception) {
            sessionStore.clear();
            if (exception instanceof XuiAuthenticationException) {
                throw exception;
            }
            throw exception;
        }
    }

    private void validateCredentials() {
        if (properties.username() == null || properties.username().isBlank()
                || properties.password() == null || properties.password().isBlank()) {
            throw new XuiAuthenticationException("Xui credentials are not configured");
        }
    }

    private static long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
