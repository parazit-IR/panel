package com.parazit.panel.application.telegram;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TelegramMessageChunker {

    private static final int DEFAULT_LIMIT = 3500;

    public List<String> split(String text) {
        return split(text, DEFAULT_LIMIT);
    }

    public List<String> split(String text, int maxLength) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        int limit = Math.max(512, Math.min(maxLength, DEFAULT_LIMIT));
        if (text.length() <= limit) {
            return List.of(text);
        }
        List<String> chunks = new ArrayList<>();
        String remaining = text;
        while (!remaining.isBlank()) {
            if (remaining.length() <= limit) {
                chunks.add(remaining.strip());
                break;
            }
            int split = remaining.lastIndexOf("\n\n", limit);
            if (split < limit / 2) {
                split = remaining.lastIndexOf('\n', limit);
            }
            if (split < limit / 2) {
                split = remaining.lastIndexOf(' ', limit);
            }
            if (split < limit / 2) {
                split = limit;
            }
            chunks.add(remaining.substring(0, split).strip());
            remaining = remaining.substring(split).strip();
        }
        return chunks;
    }
}
