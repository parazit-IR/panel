package com.parazit.panel.infrastructure.security;

import com.parazit.panel.application.port.out.security.GeneratedSubscriptionToken;
import com.parazit.panel.application.port.out.security.SubscriptionTokenGenerator;
import com.parazit.panel.application.port.out.security.SubscriptionTokenHasher;
import com.parazit.panel.config.properties.SubscriptionProperties;
import com.parazit.panel.domain.subscription.SubscriptionAccessToken;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class SecureSubscriptionTokenGenerator implements SubscriptionTokenGenerator {

    private final SecureRandom secureRandom;
    private final SubscriptionTokenHasher hasher;
    private final SubscriptionProperties properties;

    public SecureSubscriptionTokenGenerator(
            SubscriptionTokenHasher hasher,
            SubscriptionProperties properties
    ) {
        this.secureRandom = new SecureRandom();
        this.hasher = Objects.requireNonNull(hasher, "hasher must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    @Override
    public GeneratedSubscriptionToken generate() {
        byte[] bytes = new byte[properties.tokenBytes()];
        secureRandom.nextBytes(bytes);
        String rawToken = SubscriptionAccessToken.PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String tokenHash = hasher.hash(rawToken);
        String safePrefix = SubscriptionAccessToken.parse(rawToken).safePrefix(properties.tokenPrefixLength());
        return new GeneratedSubscriptionToken(rawToken, tokenHash, safePrefix);
    }
}
