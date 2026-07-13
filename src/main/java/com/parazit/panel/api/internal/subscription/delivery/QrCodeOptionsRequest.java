package com.parazit.panel.api.internal.subscription.delivery;

import com.parazit.panel.application.qrcode.model.QrErrorCorrection;
import com.parazit.panel.application.qrcode.model.QrImageFormat;

public record QrCodeOptionsRequest(
        QrImageFormat format,
        Integer size,
        Integer marginModules,
        QrErrorCorrection errorCorrection,
        Boolean transparentBackground
) {
}

