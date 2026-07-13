package com.parazit.panel.application.telegram.menu;

import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class TelegramButtonTextNormalizer {

    public String normalize(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text
                .replace('\u064A', '\u06CC')
                .replace('\u0643', '\u06A9')
                .replace("\u200C", " ")
                .replace("\u200D", "")
                .replace("\uFE0E", "")
                .replace("\uFE0F", "")
                .trim();
        StringBuilder result = new StringBuilder(normalized.length());
        boolean previousWhitespace = false;
        for (int i = 0; i < normalized.length(); i++) {
            char current = normalized.charAt(i);
            if (Character.isWhitespace(current)) {
                if (!previousWhitespace) {
                    result.append(' ');
                }
                previousWhitespace = true;
            } else {
                result.append(current);
                previousWhitespace = false;
            }
        }
        return result.toString();
    }

    public boolean exactNormalizedMatch(String first, String second) {
        return Objects.equals(normalize(first), normalize(second));
    }
}
