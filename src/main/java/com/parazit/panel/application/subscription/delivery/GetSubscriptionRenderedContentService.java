package com.parazit.panel.application.subscription.delivery;

import com.parazit.panel.application.port.in.subscription.delivery.GetSubscriptionRenderedContentUseCase;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class GetSubscriptionRenderedContentService implements GetSubscriptionRenderedContentUseCase {

    private static final String CONTENT_TYPE = "text/plain; charset=utf-8";

    private final SubscriptionDeliveryContentResolver resolver;

    public GetSubscriptionRenderedContentService(SubscriptionDeliveryContentResolver resolver) {
        this.resolver = Objects.requireNonNull(resolver, "resolver must not be null");
    }

    @Override
    public SubscriptionRenderedContentResult get(Long telegramUserId, UUID subscriptionId, String format) {
        String selected = format == null || format.isBlank() ? "base64" : format.trim().toLowerCase(Locale.ROOT);
        if (!"plain".equals(selected) && !"base64".equals(selected)) {
            throw new IllegalArgumentException("format must be plain or base64");
        }
        SubscriptionDeliveryContent content = resolver.resolveContent(telegramUserId, subscriptionId);
        StringBuilder plain = new StringBuilder();
        for (ResolvedSubscriptionConfigEntry entry : content.entries()) {
            plain.append(entry.uri()).append('\n');
        }
        byte[] body = "plain".equals(selected)
                ? plain.toString().getBytes(StandardCharsets.UTF_8)
                : Base64.getEncoder().encode(plain.toString().getBytes(StandardCharsets.UTF_8));
        return new SubscriptionRenderedContentResult(subscriptionId, selected, CONTENT_TYPE, body);
    }
}

