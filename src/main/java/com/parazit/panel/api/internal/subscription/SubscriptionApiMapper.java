package com.parazit.panel.api.internal.subscription;

import com.parazit.panel.application.subscription.command.CreateSubscriptionCommand;
import com.parazit.panel.application.subscription.command.RevokeSubscriptionCommand;
import com.parazit.panel.application.subscription.command.RotateSubscriptionTokenCommand;
import com.parazit.panel.application.subscription.result.CreateSubscriptionResult;
import com.parazit.panel.application.subscription.result.SubscriptionResult;
import com.parazit.panel.config.properties.SubscriptionProperties;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionApiMapper {

    private final SubscriptionProperties properties;

    public SubscriptionApiMapper(SubscriptionProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    public CreateSubscriptionCommand toCreateCommand(Long telegramUserId, CreateSubscriptionRequest request) {
        return new CreateSubscriptionCommand(telegramUserId, request.xuiClientProvisionId());
    }

    public RotateSubscriptionTokenCommand toRotateCommand(
            Long telegramUserId,
            UUID subscriptionId,
            RotateSubscriptionTokenRequest request
    ) {
        return new RotateSubscriptionTokenCommand(
                telegramUserId,
                subscriptionId,
                request == null ? null : request.reason()
        );
    }

    public RevokeSubscriptionCommand toRevokeCommand(
            Long telegramUserId,
            UUID subscriptionId,
            RevokeSubscriptionRequest request
    ) {
        return new RevokeSubscriptionCommand(
                telegramUserId,
                subscriptionId,
                request == null ? null : request.reason()
        );
    }

    public CreateSubscriptionResponse toCreateResponse(CreateSubscriptionResult result) {
        String subscriptionUrl = result.rawAccessToken() == null
                ? null
                : properties.subscriptionUrl(result.rawAccessToken()).toString();
        return new CreateSubscriptionResponse(
                result.subscriptionId(),
                result.userId(),
                result.orderId(),
                result.xuiClientProvisionId(),
                result.status(),
                result.rawAccessToken(),
                subscriptionUrl,
                result.accessTokenPrefix(),
                result.tokenVersion(),
                result.activatedAt(),
                result.expiresAt(),
                result.newlyCreated(),
                deliveryLinks(result)
        );
    }

    public RotateSubscriptionTokenResponse toRotateResponse(CreateSubscriptionResult result) {
        return new RotateSubscriptionTokenResponse(
                result.subscriptionId(),
                result.rawAccessToken(),
                properties.subscriptionUrl(result.rawAccessToken()).toString(),
                result.accessTokenPrefix(),
                result.tokenVersion(),
                result.expiresAt()
        );
    }

    public SubscriptionResponse toResponse(SubscriptionResult result) {
        return new SubscriptionResponse(
                result.subscriptionId(),
                result.orderId(),
                result.xuiClientProvisionId(),
                result.planName(),
                result.status(),
                result.accessTokenPrefix(),
                result.tokenVersion(),
                result.activatedAt(),
                result.expiresAt(),
                result.revokedAt(),
                result.lastAccessedAt(),
                result.accessCount(),
                result.accessible()
        );
    }

    private static SubscriptionDeliveryLinksResponse deliveryLinks(CreateSubscriptionResult result) {
        String base = "/internal/users/{telegramUserId}/subscriptions/" + result.subscriptionId() + "/delivery";
        return new SubscriptionDeliveryLinksResponse(base, base + "/subscription-url/qr", 1);
    }
}
