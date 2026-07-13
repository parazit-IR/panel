package com.parazit.panel.application.telegram.tutorial;

import com.parazit.panel.application.content.tutorial.TutorialContent;
import com.parazit.panel.application.content.tutorial.TutorialTroubleshootingItem;
import com.parazit.panel.application.telegram.TelegramHtmlEscaper;
import com.parazit.panel.application.telegram.TelegramMessageCatalog;
import com.parazit.panel.application.telegram.TelegramPersianTextFormatter;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class TelegramTutorialMessageFormatter {

    private final TelegramMessageCatalog catalog;
    private final TelegramHtmlEscaper escaper;
    private final TelegramPersianTextFormatter textFormatter;

    public TelegramTutorialMessageFormatter(
            TelegramMessageCatalog catalog,
            TelegramHtmlEscaper escaper,
            TelegramPersianTextFormatter textFormatter
    ) {
        this.catalog = Objects.requireNonNull(catalog, "catalog must not be null");
        this.escaper = Objects.requireNonNull(escaper, "escaper must not be null");
        this.textFormatter = Objects.requireNonNull(textFormatter, "textFormatter must not be null");
    }

    public String menu(String language) {
        return catalog.text(language, "telegram.tutorials.title") + "\n\n" + catalog.text(language, "telegram.tutorials.choose");
    }

    public String empty(String language) {
        return catalog.text(language, "telegram.tutorials.empty");
    }

    public String detail(String language, TutorialContent content) {
        Objects.requireNonNull(content, "content must not be null");
        StringBuilder builder = new StringBuilder("📚 ").append(escaper.escape(content.title()));
        if (!content.appName().isBlank()) {
            builder.append("\n\n")
                    .append(catalog.text(language, "telegram.tutorials.app_name"))
                    .append(":\n")
                    .append(escaper.escape(content.appName()));
        }
        if (!content.shortDescription().isBlank()) {
            builder.append("\n\n").append(escaper.escape(content.shortDescription()));
        }
        if (!content.steps().isEmpty()) {
            builder.append("\n\n").append(catalog.text(language, "telegram.tutorials.steps")).append(":\n");
            int index = 1;
            for (String step : content.steps()) {
                builder.append("\n")
                        .append(textFormatter.formatNumber(index++, language))
                        .append(". ")
                        .append(escaper.escape(step));
            }
        }
        if (!content.troubleshootingItems().isEmpty()) {
            builder.append("\n\n").append(catalog.text(language, "telegram.tutorials.troubleshooting")).append(":");
            for (TutorialTroubleshootingItem item : content.troubleshootingItems()) {
                builder.append("\n\n")
                        .append(escaper.escape(item.title()))
                        .append("\n")
                        .append(escaper.escape(item.description()));
            }
        }
        if (!content.notes().isBlank()) {
            builder.append("\n\n").append(escaper.escape(content.notes()));
        }
        return builder.toString();
    }

    public String downloads(String language, TutorialContent content) {
        Objects.requireNonNull(content, "content must not be null");
        StringBuilder builder = new StringBuilder(catalog.text(language, "telegram.downloads.title"));
        if (!content.shortDescription().isBlank()) {
            builder.append("\n\n").append(escaper.escape(content.shortDescription()));
        }
        if (content.downloadLinks().isEmpty()) {
            builder.append("\n\n").append(catalog.text(language, "telegram.downloads.empty"));
        } else {
            builder.append("\n\n").append(catalog.text(language, "telegram.downloads.official_source"));
        }
        return builder.toString();
    }
}
