package com.parazit.panel.application.telegram.menu;

import com.parazit.panel.application.telegram.TelegramMessageCatalog;
import com.parazit.panel.application.telegram.navigation.TelegramNavigationAction;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class TelegramMenuLabelProvider {

    private static final Map<TelegramMainMenuAction, String> MAIN_KEYS = new EnumMap<>(TelegramMainMenuAction.class);
    private static final Map<TelegramNavigationAction, String> NAVIGATION_KEYS = new EnumMap<>(TelegramNavigationAction.class);

    static {
        MAIN_KEYS.put(TelegramMainMenuAction.BUY_SUBSCRIPTION, "telegram.main.buy_subscription");
        MAIN_KEYS.put(TelegramMainMenuAction.RENEW_SERVICE, "telegram.main.renew_service");
        MAIN_KEYS.put(TelegramMainMenuAction.MY_SERVICES, "telegram.main.my_services");
        MAIN_KEYS.put(TelegramMainMenuAction.REQUEST_TRIAL, "telegram.main.trial_account");
        MAIN_KEYS.put(TelegramMainMenuAction.SHOW_TARIFFS, "telegram.main.tariffs");
        MAIN_KEYS.put(TelegramMainMenuAction.SHOW_WALLET, "telegram.main.wallet");
        MAIN_KEYS.put(TelegramMainMenuAction.SHOW_TUTORIALS, "telegram.main.tutorials");
        MAIN_KEYS.put(TelegramMainMenuAction.SHOW_SUPPORT, "telegram.main.support");

        NAVIGATION_KEYS.put(TelegramNavigationAction.BACK, "telegram.navigation.back");
        NAVIGATION_KEYS.put(TelegramNavigationAction.HOME, "telegram.navigation.home");
        NAVIGATION_KEYS.put(TelegramNavigationAction.CLOSE, "telegram.navigation.close");
    }

    private final TelegramMessageCatalog catalog;

    public TelegramMenuLabelProvider(TelegramMessageCatalog catalog) {
        this.catalog = Objects.requireNonNull(catalog, "catalog must not be null");
    }

    public String label(String language, TelegramMainMenuAction action) {
        Objects.requireNonNull(action, "action must not be null");
        return catalog.text(language, MAIN_KEYS.get(action));
    }

    public String label(String language, TelegramNavigationAction action) {
        Objects.requireNonNull(action, "action must not be null");
        return catalog.text(language, NAVIGATION_KEYS.get(action));
    }
}
