package com.parazit.panel.application.content.tutorial;

import java.net.URI;
import java.util.Objects;

public record TutorialDownloadLink(
        String label,
        URI url,
        TutorialDownloadSource source,
        boolean primary
) {

    public TutorialDownloadLink {
        label = requireBounded(label, "download label", 80);
        url = Objects.requireNonNull(url, "download url must not be null");
        source = source == null ? TutorialDownloadSource.OFFICIAL_WEBSITE : source;
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

    @Override
    public String toString() {
        return "TutorialDownloadLink[label=%s,source=%s,primary=%s]".formatted(label, source, primary);
    }
}
