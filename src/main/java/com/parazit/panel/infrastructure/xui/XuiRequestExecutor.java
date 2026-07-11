package com.parazit.panel.infrastructure.xui;

import com.parazit.panel.infrastructure.xui.exception.XuiExceptionMapper;
import com.parazit.panel.infrastructure.xui.retry.XuiRetryExecutor;
import com.parazit.panel.infrastructure.xui.session.XuiSessionStore;
import java.util.Objects;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import com.parazit.panel.infrastructure.xui.exception.XuiAuthenticationException;
import com.parazit.panel.infrastructure.xui.exception.XuiException;

@Component
public class XuiRequestExecutor {

    private final RestClient restClient;
    private final XuiSessionStore sessionStore;
    private final XuiRetryExecutor retryExecutor;
    private final XuiExceptionMapper exceptionMapper;

    public XuiRequestExecutor(
            @Qualifier("xuiRestClient") RestClient xuiRestClient,
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
        return getEntity(path, responseType).getBody();
    }

    public <T> ResponseEntity<T> getEntity(String path, Class<T> responseType) {
        return retryExecutor.execute(() -> {
            try {
                ResponseEntity<T> response = restClient.get()
                        .uri(path)
                        .headers(this::applySessionCookie)
                        .retrieve()
                        .toEntity(responseType);
                storeSessionCookies(response.getHeaders());
                rejectExpiredSession(response);
                return response;
            } catch (RuntimeException exception) {
                throw mapAndClearExpiredSession(exception);
            }
        });
    }

    public <T> T post(String path, Object body, Class<T> responseType) {
        return postEntity(path, body, responseType).getBody();
    }

    public <T> ResponseEntity<T> postEntity(String path, Object body, Class<T> responseType) {
        return retryExecutor.execute(() -> {
            try {
                ResponseEntity<T> response = restClient.post()
                        .uri(path)
                        .headers(this::applySessionCookie)
                        .body(body)
                        .retrieve()
                        .toEntity(responseType);
                storeSessionCookies(response.getHeaders());
                rejectExpiredSession(response);
                return response;
            } catch (RuntimeException exception) {
                throw mapAndClearExpiredSession(exception);
            }
        });
    }

    private void applySessionCookie(HttpHeaders headers) {
        sessionStore.cookieHeader().ifPresent(value -> headers.set(HttpHeaders.COOKIE, value));
    }

    private void storeSessionCookies(HttpHeaders headers) {
        sessionStore.storeFromSetCookieHeaders(headers.get(HttpHeaders.SET_COOKIE));
    }

    private void rejectExpiredSession(ResponseEntity<?> response) {
        HttpStatusCode statusCode = response.getStatusCode();
        if (isLoginRedirect(statusCode, response.getHeaders())) {
            sessionStore.clear();
            throw new XuiAuthenticationException("Xui session expired");
        }
    }

    private XuiException mapAndClearExpiredSession(RuntimeException exception) {
        XuiException mapped = exceptionMapper.map(exception);
        if (mapped instanceof XuiAuthenticationException) {
            sessionStore.clear();
        }
        return mapped;
    }

    private static boolean isLoginRedirect(HttpStatusCode statusCode, HttpHeaders headers) {
        if (!statusCode.is3xxRedirection()) {
            return false;
        }
        String location = headers.getFirst(HttpHeaders.LOCATION);
        return location != null && location.toLowerCase().contains("login");
    }
}
