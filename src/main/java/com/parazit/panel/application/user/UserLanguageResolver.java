package com.parazit.panel.application.user;

import com.parazit.panel.domain.user.UserLanguage;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class UserLanguageResolver {

    /**
     * Telegram may omit or send unsupported language codes. The temporary product
     * default is Persian until explicit user settings are introduced.
     */
    private static final UserLanguage DEFAULT_LANGUAGE = UserLanguage.FA;

    public UserLanguage resolve(String languageCode) {
        if (languageCode == null || languageCode.isBlank()) {
            return DEFAULT_LANGUAGE;
        }

        String normalized = languageCode.trim()
                .replace('_', '-')
                .toLowerCase(Locale.ROOT);

        if (normalized.equals("en") || normalized.startsWith("en-")) {
            return UserLanguage.EN;
        }
        if (normalized.equals("fa") || normalized.startsWith("fa-")) {
            return UserLanguage.FA;
        }

        return DEFAULT_LANGUAGE;
    }
}
