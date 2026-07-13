package com.parazit.panel.config.properties;

import com.parazit.panel.application.content.faq.FaqItem;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.telegram.faq")
public record FaqProperties(
        boolean enabled,
        int pageSize,
        List<FaqItem> items
) {

    public FaqProperties {
        pageSize = pageSize <= 0 ? 6 : pageSize;
        if (pageSize < 1 || pageSize > 10) {
            throw new IllegalArgumentException("app.telegram.faq.page-size must be between 1 and 10");
        }
        items = items == null || items.isEmpty() ? defaultItems() : List.copyOf(items);
        if (items.size() > 50) {
            throw new IllegalArgumentException("app.telegram.faq.items is too large");
        }
        Set<String> ids = new HashSet<>();
        for (FaqItem item : items) {
            if (!ids.add(item.id())) {
                throw new IllegalArgumentException("duplicate FAQ id");
            }
        }
        boolean hasEnabledItem = items.stream().anyMatch(FaqItem::enabled);
        if (enabled && !hasEnabledItem) {
            throw new IllegalArgumentException("app.telegram.faq must contain at least one enabled item");
        }
    }

    public List<FaqItem> enabledItems() {
        return items.stream()
                .filter(FaqItem::enabled)
                .sorted(Comparator.comparingInt(FaqItem::displayOrder).thenComparing(FaqItem::id))
                .toList();
    }

    private static List<FaqItem> defaultItems() {
        return List.of(
                new FaqItem("how-to-buy", true, 10, "چطور اشتراک بخرم؟", "از منوی پایین گزینه خرید اشتراک را انتخاب کنید و مراحل نمایش‌داده‌شده را دنبال کنید.", List.of("purchase"), "1"),
                new FaqItem("how-to-connect", true, 20, "چطور وصل شوم؟", "پس از فعال شدن سرویس، از بخش سرویس‌های من لینک اشتراک یا QR را دریافت و در برنامه مناسب وارد کنید.", List.of("connect"), "1"),
                new FaqItem("update-subscription", true, 30, "چطور لیست سرورها را به‌روزرسانی کنم؟", "در برنامه اتصال، گزینه Update یا به‌روزرسانی Subscription را بزنید.", List.of("update"), "1"),
                new FaqItem("connection-fails", true, 40, "اگر اتصال برقرار نشد چه کنم؟", "ابتدا اینترنت دستگاه، به‌روزرسانی اشتراک و انتخاب سرور دیگر را بررسی کنید. سپس از بخش پشتیبانی کمک بگیرید.", List.of("support"), "1"),
                new FaqItem("devices", true, 50, "تعداد دستگاه‌ها چقدر است؟", "تعداد دستگاه‌های مجاز به پلن خریداری‌شده بستگی دارد و در توضیحات پلن نمایش داده می‌شود.", List.of("devices"), "1")
        );
    }

    @Override
    public String toString() {
        return "FaqProperties[enabled=%s,pageSize=%d,items=%d]".formatted(enabled, pageSize, items.size());
    }
}
