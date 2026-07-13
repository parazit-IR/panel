package com.parazit.panel.application.content.tutorial;

import java.util.List;
import java.util.Objects;

public record TutorialContent(
        TutorialPlatform platform,
        boolean enabled,
        String title,
        String appName,
        String shortDescription,
        List<String> steps,
        List<TutorialDownloadLink> downloadLinks,
        List<TutorialTroubleshootingItem> troubleshootingItems,
        String templateVersion,
        String notes
) {

    public TutorialContent {
        platform = Objects.requireNonNull(platform, "platform must not be null");
        title = normalize(title, 120);
        appName = normalize(appName, 80);
        shortDescription = normalize(shortDescription, 500);
        steps = steps == null ? List.of() : steps.stream().map(step -> normalizeRequired(step, "tutorial step", 700)).toList();
        downloadLinks = downloadLinks == null ? List.of() : List.copyOf(downloadLinks);
        troubleshootingItems = troubleshootingItems == null ? List.of() : List.copyOf(troubleshootingItems);
        templateVersion = templateVersion == null || templateVersion.isBlank() ? "1" : normalizeRequired(templateVersion, "template version", 20);
        notes = normalize(notes, 500);
        if (enabled && title.isBlank()) {
            throw new IllegalArgumentException("enabled tutorial title is required");
        }
        if (enabled && platform != TutorialPlatform.GENERAL_DOWNLOADS && steps.isEmpty()) {
            throw new IllegalArgumentException("enabled tutorial steps are required");
        }
        if (steps.size() > 12 || downloadLinks.size() > 8 || troubleshootingItems.size() > 8) {
            throw new IllegalArgumentException("tutorial content list is too large");
        }
        long primary = downloadLinks.stream().filter(TutorialDownloadLink::primary).count();
        if (primary > 1) {
            throw new IllegalArgumentException("tutorial must not have multiple primary download links");
        }
    }

    private static String normalize(String value, int max) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return normalizeRequired(value, "content", max);
    }

    private static String normalizeRequired(String value, String field, int max) {
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
        return "TutorialContent[platform=%s,enabled=%s,templateVersion=%s,steps=%d,downloadLinks=%d]"
                .formatted(platform, enabled, templateVersion, steps.size(), downloadLinks.size());
    }
}
