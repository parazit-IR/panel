package com.parazit.panel.application.subscription.delivery;

import com.parazit.panel.application.port.in.subscription.delivery.GenerateVlessConfigQrCodeUseCase;
import com.parazit.panel.application.port.out.qrcode.QrCodeGenerator;
import com.parazit.panel.application.qrcode.QrRenderingDisabledException;
import com.parazit.panel.application.qrcode.model.GeneratedQrCode;
import com.parazit.panel.application.qrcode.model.QrPayload;
import com.parazit.panel.application.qrcode.model.QrPayloadType;
import com.parazit.panel.application.subscription.SubscriptionNotFoundException;
import com.parazit.panel.config.properties.QrCodeProperties;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class GenerateVlessConfigQrCodeService implements GenerateVlessConfigQrCodeUseCase {

    private static final Logger log = LoggerFactory.getLogger(GenerateVlessConfigQrCodeService.class);

    private final SubscriptionDeliveryContentResolver resolver;
    private final QrCodeGenerator qrCodeGenerator;
    private final QrCodeProperties qrProperties;

    public GenerateVlessConfigQrCodeService(
            SubscriptionDeliveryContentResolver resolver,
            QrCodeGenerator qrCodeGenerator,
            QrCodeProperties qrProperties
    ) {
        this.resolver = Objects.requireNonNull(resolver, "resolver must not be null");
        this.qrCodeGenerator = Objects.requireNonNull(qrCodeGenerator, "qrCodeGenerator must not be null");
        this.qrProperties = Objects.requireNonNull(qrProperties, "qrProperties must not be null");
    }

    @Override
    public QrCodeImageResult generate(GenerateVlessConfigQrCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        if (!qrProperties.enabled()) {
            throw new QrRenderingDisabledException();
        }
        if (command.configIndex() < 1) {
            throw new IllegalArgumentException("configIndex must be one-based and positive");
        }
        SubscriptionDeliveryContent content = resolver.resolveContent(command.telegramUserId(), command.subscriptionId());
        ResolvedSubscriptionConfigEntry entry = content.entries()
                .stream()
                .filter(candidate -> candidate.index() == command.configIndex())
                .findFirst()
                .orElseThrow(() -> new SubscriptionNotFoundException(command.subscriptionId()));
        GeneratedQrCode generated = qrCodeGenerator.generate(
                new QrPayload(QrPayloadType.VLESS_URI, entry.uri()),
                command.options()
        );
        log.atInfo()
                .addKeyValue("subscriptionId", command.subscriptionId())
                .addKeyValue("payloadType", QrPayloadType.VLESS_URI)
                .addKeyValue("format", generated.format())
                .addKeyValue("size", generated.width())
                .addKeyValue("configIndex", command.configIndex())
                .log("VLESS QR generated");
        return QrImageResultFactory.vless(command.configIndex(), generated, command.download());
    }
}
