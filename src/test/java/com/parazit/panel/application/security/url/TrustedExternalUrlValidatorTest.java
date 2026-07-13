package com.parazit.panel.application.security.url;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TrustedExternalUrlValidatorTest {

    private final TrustedExternalUrlValidator validator = new TrustedExternalUrlValidator();

    @Test
    void acceptsHttpsAllowlistedHosts() {
        URI uri = validator.validate("https://github.com/2dust/v2rayNG/releases", Set.of("github.com"));

        assertThat(uri).isEqualTo(URI.create("https://github.com/2dust/v2rayNG/releases"));
    }

    @Test
    void rejectsUnsafeSchemesAndCredentials() {
        assertThatThrownBy(() -> validator.validate("http://github.com/app", Set.of("github.com")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> validator.validate("ftp://github.com/app", Set.of("github.com")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> validator.validate("javascript:alert(1)", Set.of("github.com")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> validator.validate("https://user:pass@github.com/app", Set.of("github.com")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsLocalAndPrivateDestinations() {
        assertThatThrownBy(() -> validator.validate("https://localhost/app", Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> validator.validate("https://127.0.0.1/app", Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> validator.validate("https://10.1.2.3/app", Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> validator.validate("https://[::1]/app", Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNonAllowlistedHostsWhenPolicyIsSet() {
        assertThatThrownBy(() -> validator.validate("https://example.com/app", Set.of("github.com")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
