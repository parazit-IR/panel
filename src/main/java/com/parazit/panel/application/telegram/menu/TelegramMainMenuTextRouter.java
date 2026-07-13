package com.parazit.panel.application.telegram.menu;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TelegramMainMenuTextRouter {

    private static final Logger log = LoggerFactory.getLogger(TelegramMainMenuTextRouter.class);

    private final TelegramMenuLabelProvider labelProvider;
    private final TelegramButtonTextNormalizer normalizer;
    private final TelegramMenuTextAliasRegistry aliasRegistry;

    public TelegramMainMenuTextRouter(
            TelegramMenuLabelProvider labelProvider,
            TelegramButtonTextNormalizer normalizer,
            TelegramMenuTextAliasRegistry aliasRegistry
    ) {
        this.labelProvider = Objects.requireNonNull(labelProvider, "labelProvider must not be null");
        this.normalizer = Objects.requireNonNull(normalizer, "normalizer must not be null");
        this.aliasRegistry = Objects.requireNonNull(aliasRegistry, "aliasRegistry must not be null");
    }

    public Optional<TelegramMainMenuAction> route(String language, String text) {
        String normalized = normalizer.normalize(text);
        if (normalized.isBlank()) {
            return Optional.empty();
        }
        Map<String, TelegramMainMenuAction> labels = labels(language);
        TelegramMainMenuAction action = labels.get(normalized);
        if (action != null) {
            log.atDebug().addKeyValue("action", action).addKeyValue("locale", language).log("Telegram main menu label matched");
            return Optional.of(action);
        }
        for (Map.Entry<String, TelegramMainMenuAction> alias : aliasRegistry.aliases().entrySet()) {
            if (normalizer.exactNormalizedMatch(alias.getKey(), normalized)) {
                log.atDebug().addKeyValue("action", alias.getValue()).addKeyValue("locale", language).log("Telegram main menu alias matched");
                return Optional.of(alias.getValue());
            }
        }
        return Optional.empty();
    }

    private Map<String, TelegramMainMenuAction> labels(String language) {
        Map<String, TelegramMainMenuAction> labels = new java.util.LinkedHashMap<>();
        for (TelegramMainMenuAction action : TelegramMainMenuAction.values()) {
            labels.put(normalizer.normalize(labelProvider.label(language, action)), action);
        }
        return labels;
    }
}
