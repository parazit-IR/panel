package com.parazit.panel.application.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.parazit.panel.domain.user.UserLanguage;
import org.junit.jupiter.api.Test;

class UserLanguageResolverTest {

    private final UserLanguageResolver resolver = new UserLanguageResolver();

    @Test
    void resolvesPersianLanguageCodes() {
        assertThat(resolver.resolve("fa")).isEqualTo(UserLanguage.FA);
        assertThat(resolver.resolve("FA")).isEqualTo(UserLanguage.FA);
        assertThat(resolver.resolve("fa-IR")).isEqualTo(UserLanguage.FA);
        assertThat(resolver.resolve("fa_ir")).isEqualTo(UserLanguage.FA);
        assertThat(resolver.resolve(" fa ")).isEqualTo(UserLanguage.FA);
    }

    @Test
    void resolvesEnglishLanguageCodes() {
        assertThat(resolver.resolve("en")).isEqualTo(UserLanguage.EN);
        assertThat(resolver.resolve("en-US")).isEqualTo(UserLanguage.EN);
    }

    @Test
    void defaultsToPersianForMissingOrUnsupportedLanguageCodes() {
        assertThat(resolver.resolve(null)).isEqualTo(UserLanguage.FA);
        assertThat(resolver.resolve("   ")).isEqualTo(UserLanguage.FA);
        assertThat(resolver.resolve("de")).isEqualTo(UserLanguage.FA);
    }
}
