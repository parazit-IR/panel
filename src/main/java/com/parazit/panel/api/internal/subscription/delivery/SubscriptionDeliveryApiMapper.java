package com.parazit.panel.api.internal.subscription.delivery;

import com.parazit.panel.application.qrcode.InvalidQrOptionsException;
import com.parazit.panel.application.qrcode.model.QrErrorCorrection;
import com.parazit.panel.application.qrcode.model.QrImageFormat;
import com.parazit.panel.application.qrcode.model.QrRenderOptions;
import com.parazit.panel.application.subscription.delivery.BuildSubscriptionUrlCommand;
import com.parazit.panel.application.subscription.delivery.BuildSubscriptionUrlResult;
import com.parazit.panel.application.subscription.delivery.GenerateSubscriptionUrlQrCommand;
import com.parazit.panel.application.subscription.delivery.GenerateVlessConfigQrCommand;
import com.parazit.panel.application.subscription.delivery.SubscriptionConfigEntryResult;
import com.parazit.panel.application.subscription.delivery.SubscriptionDeliveryEntry;
import com.parazit.panel.application.subscription.delivery.SubscriptionDeliverySummary;
import com.parazit.panel.config.properties.QrCodeProperties;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionDeliveryApiMapper {

    private final QrCodeProperties qrProperties;

    public SubscriptionDeliveryApiMapper(QrCodeProperties qrProperties) {
        this.qrProperties = qrProperties;
    }

    public SubscriptionDeliverySummaryResponse toResponse(SubscriptionDeliverySummary summary) {
        return new SubscriptionDeliverySummaryResponse(
                summary.subscriptionId(),
                summary.planName(),
                summary.status(),
                summary.expiresAt(),
                summary.tokenVersion(),
                summary.accessTokenPrefix(),
                summary.configCount(),
                summary.entries().stream().map(this::toEntryResponse).toList(),
                summary.subscriptionUrlAvailable(),
                summary.subscriptionQrAvailable(),
                summary.configQrAvailable()
        );
    }

    public BuildSubscriptionUrlCommand toBuildCommand(
            Long telegramUserId,
            UUID subscriptionId,
            BuildSubscriptionUrlRequest request
    ) {
        return new BuildSubscriptionUrlCommand(telegramUserId, subscriptionId, request.accessToken());
    }

    public GenerateSubscriptionUrlQrCommand toSubscriptionQrCommand(
            Long telegramUserId,
            UUID subscriptionId,
            GenerateSubscriptionUrlQrRequest request
    ) {
        return new GenerateSubscriptionUrlQrCommand(
                telegramUserId,
                subscriptionId,
                request.accessToken(),
                options(request.format(), request.size(), request.marginModules(), request.errorCorrection(), request.transparentBackground()),
                Boolean.TRUE.equals(request.download())
        );
    }

    public GenerateVlessConfigQrCommand toVlessQrCommand(
            Long telegramUserId,
            UUID subscriptionId,
            int configIndex,
            QrImageFormat format,
            Integer size,
            Integer marginModules,
            QrErrorCorrection errorCorrection,
            Boolean transparentBackground,
            Boolean download
    ) {
        return new GenerateVlessConfigQrCommand(
                telegramUserId,
                subscriptionId,
                configIndex,
                options(format, size, marginModules, errorCorrection, transparentBackground),
                Boolean.TRUE.equals(download)
        );
    }

    public BuildSubscriptionUrlResponse toResponse(BuildSubscriptionUrlResult result) {
        return new BuildSubscriptionUrlResponse(result.subscriptionUrl());
    }

    public SubscriptionConfigEntryResponse toResponse(SubscriptionConfigEntryResult result) {
        return new SubscriptionConfigEntryResponse(
                result.subscriptionId(),
                result.index(),
                result.protocol(),
                result.displayName(),
                result.uri(),
                result.server(),
                result.port(),
                result.transport(),
                result.security(),
                result.expiresAt()
        );
    }

    private SubscriptionDeliveryEntryResponse toEntryResponse(SubscriptionDeliveryEntry entry) {
        return new SubscriptionDeliveryEntryResponse(
                entry.index(),
                entry.protocol(),
                entry.displayName(),
                entry.maskedServer(),
                entry.port(),
                entry.transport(),
                entry.security(),
                entry.qrAvailable()
        );
    }

    private QrRenderOptions options(
            QrImageFormat format,
            Integer size,
            Integer marginModules,
            QrErrorCorrection errorCorrection,
            Boolean transparentBackground
    ) {
        int requiredSize = size == null ? qrProperties.defaultSize() : size;
        int requiredMargin = marginModules == null ? qrProperties.defaultMarginModules() : marginModules;
        QrImageFormat requiredFormat = format == null ? qrProperties.defaultFormat() : format;
        QrErrorCorrection requiredCorrection = errorCorrection == null ? qrProperties.defaultErrorCorrection() : errorCorrection;
        boolean transparent = Boolean.TRUE.equals(transparentBackground);
        if (requiredSize < qrProperties.minSize() || requiredSize > qrProperties.maxSize()) {
            throw new InvalidQrOptionsException("QR size is outside configured bounds");
        }
        if (requiredMargin < 0 || requiredMargin > qrProperties.maxMarginModules()) {
            throw new InvalidQrOptionsException("QR margin is outside configured bounds");
        }
        if (requiredFormat == QrImageFormat.SVG && !qrProperties.allowSvg()) {
            throw new InvalidQrOptionsException("SVG QR output is not enabled");
        }
        if (transparent && !qrProperties.allowTransparentBackground()) {
            throw new InvalidQrOptionsException("Transparent QR background is not enabled");
        }
        return new QrRenderOptions(requiredSize, requiredSize, requiredMargin, requiredCorrection, requiredFormat, transparent);
    }
}

