package com.parazit.panel.application.subscription.model;

import java.util.UUID;

public record VlessSubscriptionConfig(
        UUID clientId,
        String address,
        int port,
        String encryption,
        String security,
        String sni,
        String publicKey,
        String shortId,
        String fingerprint,
        String flow,
        String transportType,
        String path,
        String host,
        String mode,
        String serviceName,
        String remark
) {
}
