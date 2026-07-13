package com.parazit.panel.application.subscription.delivery;

import com.parazit.panel.application.qrcode.model.GeneratedQrCode;
import com.parazit.panel.application.qrcode.model.QrPayloadType;
import java.util.UUID;

final class QrImageResultFactory {

    private QrImageResultFactory() {
    }

    static QrCodeImageResult subscription(UUID subscriptionId, GeneratedQrCode generated, boolean download) {
        return result(
                generated,
                "subscription-" + subscriptionId.toString().substring(0, 8) + extension(generated),
                QrPayloadType.SUBSCRIPTION_URL,
                download
        );
    }

    static QrCodeImageResult vless(int configIndex, GeneratedQrCode generated, boolean download) {
        return result(generated, "vless-config-" + configIndex + extension(generated), QrPayloadType.VLESS_URI, download);
    }

    private static QrCodeImageResult result(
            GeneratedQrCode generated,
            String filename,
            QrPayloadType payloadType,
            boolean download
    ) {
        return new QrCodeImageResult(
                generated.bytes(),
                generated.contentType(),
                filename,
                "\"" + generated.contentHash() + "\"",
                generated.width(),
                generated.height(),
                payloadType,
                generated.format(),
                download
        );
    }

    private static String extension(GeneratedQrCode generated) {
        return switch (generated.format()) {
            case PNG -> ".png";
            case SVG -> ".svg";
        };
    }
}

