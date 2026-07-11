package com.parazit.panel.infrastructure.xui.authentication;

import com.parazit.panel.infrastructure.xui.XuiRequestExecutor;
import com.parazit.panel.infrastructure.xui.config.XuiProperties;
import com.parazit.panel.infrastructure.xui.exception.XuiAuthenticationException;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedRequestExecutor {

    private static final Logger log = LoggerFactory.getLogger(AuthenticatedRequestExecutor.class);

    private final XuiAuthenticationManager authenticationManager;
    private final XuiRequestExecutor requestExecutor;
    private final XuiProperties properties;

    public AuthenticatedRequestExecutor(
            XuiAuthenticationManager authenticationManager,
            XuiRequestExecutor requestExecutor,
            XuiProperties properties
    ) {
        this.authenticationManager = Objects.requireNonNull(
                authenticationManager,
                "authenticationManager must not be null"
        );
        this.requestExecutor = Objects.requireNonNull(requestExecutor, "requestExecutor must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    public <T> T get(String path, Class<T> responseType) {
        return getEntity(path, responseType).getBody();
    }

    public <T> ResponseEntity<T> getEntity(String path, Class<T> responseType) {
        authenticationManager.ensureAuthenticated();
        try {
            return requestExecutor.getEntity(path, responseType);
        } catch (XuiAuthenticationException exception) {
            return retryAfterRelogin(() -> requestExecutor.getEntity(path, responseType));
        }
    }

    public <T> T post(String path, Object body, Class<T> responseType) {
        return postEntity(path, body, responseType).getBody();
    }

    public <T> ResponseEntity<T> postEntity(String path, Object body, Class<T> responseType) {
        authenticationManager.ensureAuthenticated();
        try {
            return requestExecutor.postEntity(path, body, responseType);
        } catch (XuiAuthenticationException exception) {
            return retryAfterRelogin(() -> requestExecutor.postEntity(path, body, responseType));
        }
    }

    private <T> T retryAfterRelogin(RequestSupplier<T> supplier) {
        if (!Boolean.TRUE.equals(properties.autoLogin())) {
            throw new XuiAuthenticationException("Xui session is not authenticated");
        }
        log.info("Xui session expired; attempting automatic re-login");
        authenticationManager.refreshSession();
        return supplier.get();
    }

    @FunctionalInterface
    private interface RequestSupplier<T> {

        T get();
    }
}
