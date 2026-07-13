package com.parazit.panel.application.telegram.menu;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class TelegramMenuTextAliasRegistry {

    public Map<String, TelegramMainMenuAction> aliases() {
        Map<String, TelegramMainMenuAction> aliases = new LinkedHashMap<>();
        aliases.put("🛒 Buy VPN", TelegramMainMenuAction.BUY_SUBSCRIPTION);
        aliases.put("Buy VPN", TelegramMainMenuAction.BUY_SUBSCRIPTION);
        aliases.put("خرید VPN", TelegramMainMenuAction.BUY_SUBSCRIPTION);
        aliases.put("📦 My subscriptions", TelegramMainMenuAction.MY_SERVICES);
        aliases.put("My subscriptions", TelegramMainMenuAction.MY_SERVICES);
        aliases.put("اشتراک‌های من", TelegramMainMenuAction.MY_SERVICES);
        aliases.put("اشتراک های من", TelegramMainMenuAction.MY_SERVICES);
        aliases.put("💳 My payments", TelegramMainMenuAction.SHOW_WALLET);
        aliases.put("⚙️ Settings", TelegramMainMenuAction.SHOW_SUPPORT);
        aliases.put("Help", TelegramMainMenuAction.SHOW_SUPPORT);
        aliases.put("راهنما", TelegramMainMenuAction.SHOW_SUPPORT);
        return Map.copyOf(aliases);
    }
}
