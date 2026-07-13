package com.parazit.panel.api.internal.subscription.delivery;

import com.parazit.panel.application.qrcode.model.QrErrorCorrection;
import com.parazit.panel.application.qrcode.model.QrImageFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GenerateSubscriptionUrlQrRequest(
        @NotBlank
        @Size(max = 512)
        String accessToken,
        QrImageFormat format,
        Integer size,
        Integer marginModules,
        QrErrorCorrection errorCorrection,
        Boolean transparentBackground,
        Boolean download
) {
}

