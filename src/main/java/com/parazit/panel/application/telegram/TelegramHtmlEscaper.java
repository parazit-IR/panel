package com.parazit.panel.application.telegram;

import org.springframework.stereotype.Component;

@Component
public class TelegramHtmlEscaper {

    public String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
