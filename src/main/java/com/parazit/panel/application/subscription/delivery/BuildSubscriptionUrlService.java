package com.parazit.panel.application.subscription.delivery;

import com.parazit.panel.application.port.in.subscription.delivery.BuildSubscriptionUrlUseCase;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BuildSubscriptionUrlService implements BuildSubscriptionUrlUseCase {

    private static final Logger log = LoggerFactory.getLogger(BuildSubscriptionUrlService.class);

    private final SubscriptionDeliveryContentResolver resolver;

    public BuildSubscriptionUrlService(SubscriptionDeliveryContentResolver resolver) {
        this.resolver = Objects.requireNonNull(resolver, "resolver must not be null");
    }

    @Override
    public BuildSubscriptionUrlResult build(BuildSubscriptionUrlCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        String url = resolver.buildValidatedSubscriptionUrl(
                command.telegramUserId(),
                command.subscriptionId(),
                command.rawAccessToken()
        );
        log.atInfo()
                .addKeyValue("subscriptionId", command.subscriptionId())
                .log("Subscription URL revealed through internal delivery use case");
        return new BuildSubscriptionUrlResult(command.subscriptionId(), url);
    }
}

