package com.parazit.panel.application.content.tutorial;

public record TutorialTroubleshootingItem(String title, String description) {

    public TutorialTroubleshootingItem {
        title = requireBounded(title, "troubleshooting title", 120);
        description = requireBounded(description, "troubleshooting description", 500);
    }

    private static String requireBounded(String value, String field, int max) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        String trimmed = value.trim();
        if (trimmed.length() > max) {
            throw new IllegalArgumentException(field + " is too long");
        }
        return trimmed;
    }
}
