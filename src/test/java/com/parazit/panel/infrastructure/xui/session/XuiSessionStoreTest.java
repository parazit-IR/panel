package com.parazit.panel.infrastructure.xui.session;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class XuiSessionStoreTest {

    private final XuiSessionStore sessionStore = new XuiSessionStore();

    @Test
    void storesAndBuildsDeterministicCookieHeader() {
        sessionStore.store("zeta", "last");
        sessionStore.store("alpha", "first");

        assertThat(sessionStore.hasSession()).isTrue();
        assertThat(sessionStore.cookieHeader()).contains("alpha=first; zeta=last");
        assertThat(sessionStore.snapshot())
                .containsEntry("alpha", "first")
                .containsEntry("zeta", "last");
    }

    @Test
    void storesCookiesFromSetCookieHeaders() {
        sessionStore.storeFromSetCookieHeaders(List.of(
                "session=abc; Path=/; HttpOnly",
                "csrf=token; Path=/"
        ));

        assertThat(sessionStore.cookieHeader()).contains("csrf=token; session=abc");
    }

    @Test
    void clearRemovesSessionState() {
        sessionStore.store("session", "abc");

        sessionStore.clear();

        assertThat(sessionStore.hasSession()).isFalse();
        assertThat(sessionStore.cookieHeader()).isEmpty();
        assertThat(sessionStore.snapshot()).isEmpty();
    }
}
