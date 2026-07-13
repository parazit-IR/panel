package com.parazit.panel.application.content.tutorial;

import com.parazit.panel.config.properties.TutorialProperties;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class TutorialCatalog {

    private final TutorialProperties properties;

    public TutorialCatalog(TutorialProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    public boolean enabled() {
        return properties.enabled();
    }

    public List<TutorialContent> enabledPlatforms() {
        if (!enabled()) {
            return List.of();
        }
        return properties.enabledContents()
                .stream()
                .filter(content -> content.platform() != TutorialPlatform.GENERAL_DOWNLOADS)
                .sorted(Comparator.comparingInt(content -> order(content.platform())))
                .toList();
    }

    public Optional<TutorialContent> findEnabled(TutorialPlatform platform) {
        Objects.requireNonNull(platform, "platform must not be null");
        if (!enabled()) {
            return Optional.empty();
        }
        return properties.allContents()
                .stream()
                .filter(TutorialContent::enabled)
                .filter(content -> content.platform() == platform)
                .findFirst();
    }

    public Optional<TutorialContent> downloads() {
        return findEnabled(TutorialPlatform.GENERAL_DOWNLOADS);
    }

    private static int order(TutorialPlatform platform) {
        return switch (platform) {
            case ANDROID -> 10;
            case IOS -> 20;
            case WINDOWS -> 30;
            case LINUX -> 40;
            case MACOS -> 50;
            case GENERAL_DOWNLOADS -> 60;
        };
    }
}
