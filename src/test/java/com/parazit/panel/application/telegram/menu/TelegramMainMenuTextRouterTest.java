package com.parazit.panel.application.telegram.menu;

import static org.assertj.core.api.Assertions.assertThat;

import com.parazit.panel.application.telegram.TelegramMessageCatalog;
import org.junit.jupiter.api.Test;

class TelegramMainMenuTextRouterTest {

    private final TelegramMainMenuTextRouter router = new TelegramMainMenuTextRouter(
            new TelegramMenuLabelProvider(new TelegramMessageCatalog()),
            new TelegramButtonTextNormalizer(),
            new TelegramMenuTextAliasRegistry()
    );

    @Test
    void mapsLocalizedPersianLabelsExactly() {
        assertThat(router.route("FA", "🔐 خرید اشتراک")).contains(TelegramMainMenuAction.BUY_SUBSCRIPTION);
        assertThat(router.route("FA", "🛍 سرویس‌های من")).contains(TelegramMainMenuAction.MY_SERVICES);
        assertThat(router.route("FA", "💰 كيف\u200Cپول + شارژ")).contains(TelegramMainMenuAction.SHOW_WALLET);
    }

    @Test
    void mapsExplicitAliasesButRejectsArbitraryOrPartialText() {
        assertThat(router.route("EN", "🛒 Buy VPN")).contains(TelegramMainMenuAction.BUY_SUBSCRIPTION);
        assertThat(router.route("FA", "خرید")).isEmpty();
        assertThat(router.route("FA", "سلام، خرید اشتراک می‌خواهم")).isEmpty();
    }
}
