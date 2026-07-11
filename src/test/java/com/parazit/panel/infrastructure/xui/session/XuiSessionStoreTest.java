package com.parazit.panel.infrastructure.xui.session;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
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
        sessionStore.markLoggedIn(Instant.parse("2026-01-01T00:00:00Z"));

        sessionStore.clear();

        assertThat(sessionStore.hasSession()).isFalse();
        assertThat(sessionStore.isPresent()).isFalse();
        assertThat(sessionStore.cookieHeader()).isEmpty();
        assertThat(sessionStore.lastLoginTime()).isEmpty();
        assertThat(sessionStore.snapshot()).isEmpty();
    }

    @Test
    void exposesCookieAndLoginTimestamp() {
        Instant loggedInAt = Instant.parse("2026-01-01T00:00:00Z");

        sessionStore.store("JSESSIONID", "abc");
        sessionStore.markLoggedIn(loggedInAt);

        assertThat(sessionStore.get("JSESSIONID")).contains("abc");
        assertThat(sessionStore.get("missing")).isEmpty();
        assertThat(sessionStore.isPresent()).isTrue();
        assertThat(sessionStore.lastLoginTime()).contains(loggedInAt);
    }
}
