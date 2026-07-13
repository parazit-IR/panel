package com.parazit.panel.application.telegram.menu;

import com.parazit.panel.application.telegram.TelegramMessageCatalog;
import com.parazit.panel.application.telegram.keyboard.TelegramReplyKeyboard;
import com.parazit.panel.application.telegram.keyboard.TelegramReplyKeyboardButton;
import com.parazit.panel.application.telegram.keyboard.TelegramReplyKeyboardRow;
import com.parazit.panel.config.properties.TelegramMenuProperties;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TelegramMainReplyKeyboardFactory {

    private static final Logger log = LoggerFactory.getLogger(TelegramMainReplyKeyboardFactory.class);

    private static final List<List<TelegramMainMenuAction>> LAYOUT = List.of(
            List.of(TelegramMainMenuAction.BUY_SUBSCRIPTION, TelegramMainMenuAction.RENEW_SERVICE),
            List.of(TelegramMainMenuAction.MY_SERVICES, TelegramMainMenuAction.REQUEST_TRIAL),
            List.of(TelegramMainMenuAction.SHOW_TARIFFS, TelegramMainMenuAction.SHOW_WALLET),
            List.of(TelegramMainMenuAction.SHOW_TUTORIALS),
            List.of(TelegramMainMenuAction.SHOW_SUPPORT)
    );

    private final TelegramMenuLabelProvider labelProvider;
    private final TelegramMenuFeatureAvailabilityService availabilityService;
    private final TelegramMessageCatalog catalog;
    private final TelegramMenuProperties properties;

    public TelegramMainReplyKeyboardFactory(
            TelegramMenuLabelProvider labelProvider,
            TelegramMenuFeatureAvailabilityService availabilityService,
            TelegramMessageCatalog catalog,
            TelegramMenuProperties properties
    ) {
        this.labelProvider = Objects.requireNonNull(labelProvider, "labelProvider must not be null");
        this.availabilityService = Objects.requireNonNull(availabilityService, "availabilityService must not be null");
        this.catalog = Objects.requireNonNull(catalog, "catalog must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    public TelegramReplyKeyboard mainKeyboard(String language) {
        List<TelegramReplyKeyboardRow> rows = new ArrayList<>();
        Set<TelegramMainMenuAction> seen = new LinkedHashSet<>();
        for (List<TelegramMainMenuAction> rowActions : LAYOUT) {
            List<TelegramReplyKeyboardButton> buttons = new ArrayList<>();
            for (TelegramMainMenuAction action : rowActions) {
                TelegramMenuFeatureAvailability availability = availabilityService.availability(action);
                if (availability.visible() && seen.add(action)) {
                    buttons.add(new TelegramReplyKeyboardButton(labelProvider.label(language, action)));
                }
            }
            if (!buttons.isEmpty()) {
                rows.add(new TelegramReplyKeyboardRow(buttons));
            }
        }
        log.atDebug()
                .addKeyValue("locale", language)
                .addKeyValue("rows", rows.size())
                .log("Generated Telegram main reply keyboard layout");
        return new TelegramReplyKeyboard(
                rows,
                true,
                false,
                properties.persistent(),
                false,
                catalog.text(language, "telegram.input.placeholder")
        );
    }
}
