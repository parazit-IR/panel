package com.parazit.panel.application.user;

import com.parazit.panel.domain.user.UserLanguage;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class UserLanguageResolver {

    /**
     * Telegram may omit or send unsupported language codes during registration.
     * The product default is Persian.
     */
    private static final UserLanguage DEFAULT_LANGUAGE = UserLanguage.FA;

    public UserLanguage resolveOrDefault(String languageCode) {
        if (languageCode == null || languageCode.isBlank()) {
            return DEFAULT_LANGUAGE;
        }

        return resolveSupported(languageCode.trim()).orElse(DEFAULT_LANGUAGE);
    }

    public UserLanguage resolveRequired(String languageCode) {
        if (languageCode == null || languageCode.isBlank()) {
            throw new InvalidUserLanguageCommandException("languageCode must not be blank");
        }

        return resolveSupported(languageCode.trim())
                .orElseThrow(() -> new InvalidUserLanguageCommandException("Unsupported languageCode: " + languageCode.trim()));
    }

    private Optional<UserLanguage> resolveSupported(String languageCode) {
        String normalized = languageCode
                .replace('_', '-')
                .toLowerCase(Locale.ROOT);

        if (normalized.startsWith("en")) {
            return Optional.of(UserLanguage.EN);
        }
        if (normalized.startsWith("fa")) {
            return Optional.of(UserLanguage.FA);
        }

        return Optional.empty();
    }
}
