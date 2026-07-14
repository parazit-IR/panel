package com.parazit.panel.application.telegram.renewal;

import com.parazit.panel.application.renewal.result.RenewalStatusResult;
import com.parazit.panel.application.telegram.TelegramMessageFormatter;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class TelegramRenewalStatusFormatter {

    private final TelegramRenewalStatusMapper statusMapper;
    private final TelegramMessageFormatter formatter;

    public TelegramRenewalStatusFormatter(
            TelegramRenewalStatusMapper statusMapper,
            TelegramMessageFormatter formatter
    ) {
        this.statusMapper = Objects.requireNonNull(statusMapper, "statusMapper must not be null");
        this.formatter = Objects.requireNonNull(formatter, "formatter must not be null");
    }

    public String format(String language, RenewalStatusResult result) {
        String service = result.serviceUsername() == null || result.serviceUsername().isBlank()
                ? result.serviceDisplayName()
                : result.serviceUsername();
        return """
                ♻️ وضعیت تمدید سرویس

                نام سرویس:
                %s

                وضعیت:
                %s

                آخرین بروزرسانی:
                %s
                """.formatted(
                formatter.html(service == null ? "-" : service),
                statusMapper.renewalLabel(result.renewalStatus(), result.paymentStatus(), result.outboxStatus(), language),
                formatter.formatDate(result.lastUpdatedAt())
        ).strip();
    }
}
