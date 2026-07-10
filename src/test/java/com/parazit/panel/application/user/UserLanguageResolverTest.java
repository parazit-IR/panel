package com.parazit.panel.application.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parazit.panel.domain.user.UserLanguage;
import org.junit.jupiter.api.Test;

class UserLanguageResolverTest {

    private final UserLanguageResolver resolver = new UserLanguageResolver();

    @Test
    void resolvesOrDefaultsRegistrationLanguageCodes() {
        assertThat(resolver.resolveOrDefault("fa")).isEqualTo(UserLanguage.FA);
        assertThat(resolver.resolveOrDefault("FA")).isEqualTo(UserLanguage.FA);
        assertThat(resolver.resolveOrDefault("fa-IR")).isEqualTo(UserLanguage.FA);
        assertThat(resolver.resolveOrDefault("farsi")).isEqualTo(UserLanguage.FA);
        assertThat(resolver.resolveOrDefault("en")).isEqualTo(UserLanguage.EN);
        assertThat(resolver.resolveOrDefault("EN")).isEqualTo(UserLanguage.EN);
        assertThat(resolver.resolveOrDefault("en-US")).isEqualTo(UserLanguage.EN);
        assertThat(resolver.resolveOrDefault("english")).isEqualTo(UserLanguage.EN);
        assertThat(resolver.resolveOrDefault(null)).isEqualTo(UserLanguage.FA);
        assertThat(resolver.resolveOrDefault("   ")).isEqualTo(UserLanguage.FA);
        assertThat(resolver.resolveOrDefault("de")).isEqualTo(UserLanguage.FA);
    }

    @Test
    void resolvesRequiredLanguageCodesStrictly() {
        assertThat(resolver.resolveRequired("fa")).isEqualTo(UserLanguage.FA);
        assertThat(resolver.resolveRequired("fa-IR")).isEqualTo(UserLanguage.FA);
        assertThat(resolver.resolveRequired("en")).isEqualTo(UserLanguage.EN);
        assertThat(resolver.resolveRequired("en-US")).isEqualTo(UserLanguage.EN);
        assertThat(resolver.resolveRequired(" en ")).isEqualTo(UserLanguage.EN);
        assertThat(resolver.resolveRequired("FA-ir")).isEqualTo(UserLanguage.FA);
        assertThat(resolver.resolveRequired("en_US")).isEqualTo(UserLanguage.EN);
    }

    @Test
    void rejectsMissingOrUnsupportedRequiredLanguageCodes() {
        assertThatThrownBy(() -> resolver.resolveRequired(null))
                .isInstanceOf(InvalidUserLanguageCommandException.class)
                .hasMessage("languageCode must not be blank");
        assertThatThrownBy(() -> resolver.resolveRequired("   "))
                .isInstanceOf(InvalidUserLanguageCommandException.class)
                .hasMessage("languageCode must not be blank");
        assertThatThrownBy(() -> resolver.resolveRequired("de"))
                .isInstanceOf(InvalidUserLanguageCommandException.class)
                .hasMessage("Unsupported languageCode: de");
    }
}
