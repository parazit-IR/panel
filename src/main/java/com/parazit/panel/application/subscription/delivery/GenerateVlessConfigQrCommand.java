package com.parazit.panel.application.subscription.delivery;

import com.parazit.panel.application.qrcode.model.QrRenderOptions;
import java.util.UUID;

public record GenerateVlessConfigQrCommand(
        Long telegramUserId,
        UUID subscriptionId,
        int configIndex,
        QrRenderOptions options,
        boolean download
) {
}

