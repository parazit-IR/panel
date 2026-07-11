package com.parazit.panel.infrastructure.xui;

import com.parazit.panel.infrastructure.xui.exception.XuiExceptionMapper;
import com.parazit.panel.infrastructure.xui.retry.XuiRetryExecutor;
import com.parazit.panel.infrastructure.xui.session.XuiSessionStore;
import java.util.Objects;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class XuiRequestExecutor {

    private final RestClient restClient;
    private final XuiSessionStore sessionStore;
    private final XuiRetryExecutor retryExecutor;
    private final XuiExceptionMapper exceptionMapper;

    public XuiRequestExecutor(
            RestClient xuiRestClient,
            XuiSessionStore sessionStore,
            XuiRetryExecutor retryExecutor,
            XuiExceptionMapper exceptionMapper
    ) {
        this.restClient = Objects.requireNonNull(xuiRestClient, "xuiRestClient must not be null");
        this.sessionStore = Objects.requireNonNull(sessionStore, "sessionStore must not be null");
        this.retryExecutor = Objects.requireNonNull(retryExecutor, "retryExecutor must not be null");
        this.exceptionMapper = Objects.requireNonNull(exceptionMapper, "exceptionMapper must not be null");
    }

    public <T> T get(String path, Class<T> responseType) {
        return retryExecutor.execute(() -> {
            try {
                ResponseEntity<T> response = restClient.get()
                        .uri(path)
                        .headers(this::applySessionCookie)
                        .retrieve()
                        .toEntity(responseType);
                storeSessionCookies(response.getHeaders());
                return response.getBody();
            } catch (RuntimeException exception) {
                throw exceptionMapper.map(exception);
            }
        });
    }

    public <T> T post(String path, Object body, Class<T> responseType) {
        return retryExecutor.execute(() -> {
            try {
                ResponseEntity<T> response = restClient.post()
                        .uri(path)
                        .headers(this::applySessionCookie)
                        .body(body)
                        .retrieve()
                        .toEntity(responseType);
                storeSessionCookies(response.getHeaders());
                return response.getBody();
            } catch (RuntimeException exception) {
                throw exceptionMapper.map(exception);
            }
        });
    }

    private void applySessionCookie(HttpHeaders headers) {
        sessionStore.cookieHeader().ifPresent(value -> headers.set(HttpHeaders.COOKIE, value));
    }

    private void storeSessionCookies(HttpHeaders headers) {
        sessionStore.storeFromSetCookieHeaders(headers.get(HttpHeaders.SET_COOKIE));
    }
}
