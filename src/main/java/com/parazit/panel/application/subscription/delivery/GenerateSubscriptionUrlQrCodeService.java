package com.parazit.panel.application.subscription.delivery;

import com.parazit.panel.application.port.in.subscription.delivery.GenerateSubscriptionUrlQrCodeUseCase;
import com.parazit.panel.application.port.out.qrcode.QrCodeGenerator;
import com.parazit.panel.application.qrcode.QrRenderingDisabledException;
import com.parazit.panel.application.qrcode.model.GeneratedQrCode;
import com.parazit.panel.application.qrcode.model.QrPayload;
import com.parazit.panel.application.qrcode.model.QrPayloadType;
import com.parazit.panel.config.properties.QrCodeProperties;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class GenerateSubscriptionUrlQrCodeService implements GenerateSubscriptionUrlQrCodeUseCase {

    private static final Logger log = LoggerFactory.getLogger(GenerateSubscriptionUrlQrCodeService.class);

    private final SubscriptionDeliveryContentResolver resolver;
    private final QrCodeGenerator qrCodeGenerator;
    private final QrCodeProperties qrProperties;

    public GenerateSubscriptionUrlQrCodeService(
            SubscriptionDeliveryContentResolver resolver,
            QrCodeGenerator qrCodeGenerator,
            QrCodeProperties qrProperties
    ) {
        this.resolver = Objects.requireNonNull(resolver, "resolver must not be null");
        this.qrCodeGenerator = Objects.requireNonNull(qrCodeGenerator, "qrCodeGenerator must not be null");
        this.qrProperties = Objects.requireNonNull(qrProperties, "qrProperties must not be null");
    }

    @Override
    public QrCodeImageResult generate(GenerateSubscriptionUrlQrCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        if (!qrProperties.enabled()) {
            throw new QrRenderingDisabledException();
        }
        String url = resolver.buildValidatedSubscriptionUrl(
                command.telegramUserId(),
                command.subscriptionId(),
                command.rawAccessToken()
        );
        GeneratedQrCode generated = qrCodeGenerator.generate(
                new QrPayload(QrPayloadType.SUBSCRIPTION_URL, url),
                command.options()
        );
        log.atInfo()
                .addKeyValue("subscriptionId", command.subscriptionId())
                .addKeyValue("payloadType", QrPayloadType.SUBSCRIPTION_URL)
                .addKeyValue("format", generated.format())
                .addKeyValue("size", generated.width())
                .log("Subscription URL QR generated");
        return QrImageResultFactory.subscription(command.subscriptionId(), generated, command.download());
    }
}

