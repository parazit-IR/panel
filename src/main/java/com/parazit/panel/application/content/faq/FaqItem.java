package com.parazit.panel.application.content.faq;

import java.util.List;

public record FaqItem(
        String id,
        boolean enabled,
        int displayOrder,
        String question,
        String answer,
        List<String> keywords,
        String templateVersion
) {

    public FaqItem {
        id = requireId(id);
        question = normalize(question, 180);
        answer = normalize(answer, 1200);
        keywords = keywords == null ? List.of() : keywords.stream().map(keyword -> normalize(keyword, 40)).toList();
        templateVersion = templateVersion == null || templateVersion.isBlank() ? "1" : normalize(templateVersion, 20);
        if (enabled && (question.isBlank() || answer.isBlank())) {
            throw new IllegalArgumentException("enabled FAQ item requires question and answer");
        }
        if (keywords.size() > 10) {
            throw new IllegalArgumentException("FAQ keywords list is too large");
        }
    }

    private static String requireId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("FAQ id is required");
        }
        String trimmed = value.trim();
        if (trimmed.length() > 32 || !trimmed.matches("[a-z0-9][a-z0-9_-]*")) {
            throw new IllegalArgumentException("FAQ id is invalid");
        }
        return trimmed;
    }

    private static String normalize(String value, int max) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() > max) {
            throw new IllegalArgumentException("FAQ content is too long");
        }
        return trimmed;
    }

    @Override
    public String toString() {
        return "FaqItem[id=%s,enabled=%s,displayOrder=%d,templateVersion=%s]".formatted(id, enabled, displayOrder, templateVersion);
    }
}
