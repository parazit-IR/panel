package com.parazit.panel.application.subscription.delivery;

import com.parazit.panel.application.qrcode.model.QrRenderOptions;
import java.util.UUID;

public record GenerateSubscriptionUrlQrCommand(
        Long telegramUserId,
        UUID subscriptionId,
        String rawAccessToken,
        QrRenderOptions options,
        boolean download
) {
}

