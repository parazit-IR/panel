package com.parazit.panel.application.telegram.faq;

import com.parazit.panel.application.content.faq.FaqItem;
import com.parazit.panel.application.telegram.TelegramHtmlEscaper;
import com.parazit.panel.application.telegram.TelegramMessageCatalog;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class TelegramFaqMessageFormatter {

    private final TelegramMessageCatalog catalog;
    private final TelegramHtmlEscaper escaper;

    public TelegramFaqMessageFormatter(TelegramMessageCatalog catalog, TelegramHtmlEscaper escaper) {
        this.catalog = Objects.requireNonNull(catalog, "catalog must not be null");
        this.escaper = Objects.requireNonNull(escaper, "escaper must not be null");
    }

    public String list(String language) {
        return catalog.text(language, "telegram.faq.title") + "\n\n" + catalog.text(language, "telegram.faq.choose");
    }

    public String empty(String language) {
        return catalog.text(language, "telegram.faq.empty");
    }

    public String detail(String language, FaqItem item) {
        Objects.requireNonNull(item, "item must not be null");
        return catalog.text(language, "telegram.faq.question")
                + ":\n"
                + escaper.escape(item.question())
                + "\n\n"
                + escaper.escape(item.answer());
    }
}
