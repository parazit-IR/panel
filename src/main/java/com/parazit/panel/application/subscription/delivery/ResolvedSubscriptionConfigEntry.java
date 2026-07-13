package com.parazit.panel.application.subscription.delivery;

record ResolvedSubscriptionConfigEntry(
        int index,
        String protocol,
        String displayName,
        String uri,
        String server,
        int port,
        String transport,
        String security
) {
}

