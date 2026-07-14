package com.parazit.panel.application.telegram.promotion;

import com.parazit.panel.application.promotion.result.DiscountApplicationResult;
import com.parazit.panel.application.promotion.result.GiftCodeRedemptionResult;
import com.parazit.panel.application.telegram.TelegramMessageCatalog;
import com.parazit.panel.application.telegram.TelegramPersianTextFormatter;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class TelegramPromotionFormatter {

    private final TelegramMessageCatalog catalog;
    private final TelegramPersianTextFormatter text;

    public TelegramPromotionFormatter(TelegramMessageCatalog catalog, TelegramPersianTextFormatter text) {
        this.catalog = Objects.requireNonNull(catalog, "catalog must not be null");
        this.text = Objects.requireNonNull(text, "text must not be null");
    }

    public String discountPrompt(String language) {
        return catalog.text(language, "telegram.promotion.discount_prompt");
    }

    public String giftPrompt(String language) {
        return catalog.text(language, "telegram.promotion.gift_prompt");
    }

    public String discountSuccess(String language, DiscountApplicationResult result) {
        return catalog.text(language, "telegram.promotion.discount_success")
                .replace("{originalAmount}", text.formatAmount(result.originalAmount().amount(), result.originalAmount().currency().name(), language))
                .replace("{discountAmount}", text.formatAmount(result.discountAmount().amount(), result.discountAmount().currency().name(), language))
                .replace("{finalAmount}", text.formatAmount(result.finalAmount().amount(), result.finalAmount().currency().name(), language));
    }

    public String giftSuccess(String language, GiftCodeRedemptionResult result) {
        String balance = result.balanceAfter() == null
                ? "-"
                : text.formatAmount(result.balanceAfter().amount(), result.balanceAfter().currency().name(), language);
        return catalog.text(language, "telegram.promotion.gift_success")
                .replace("{creditedAmount}", text.formatAmount(result.creditedAmount().amount(), result.creditedAmount().currency().name(), language))
                .replace("{balanceAfter}", balance);
    }
}
