package com.parazit.panel.application.customer;

import com.parazit.panel.domain.subscription.Subscription;
import com.parazit.panel.domain.xui.provisioning.XuiClientProvision;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CustomerServiceDisplayNamePolicy {

    private static final int MAX_LENGTH = 48;

    public String displayName(Subscription subscription, XuiClientProvision provision, String planName) {
        String candidate = safeRemoteEmail(provision);
        if (candidate == null && subscription != null) {
            candidate = normalize(subscription.getDisplayName());
        }
        if (candidate == null) {
            String base = normalize(planName);
            String suffix = subscription == null || subscription.getId() == null ? "" : "_" + shortSuffix(subscription.getId());
            candidate = (base == null ? "service" : base) + suffix;
        }
        return bound(candidate);
    }

    public String serviceUsername(Subscription subscription, XuiClientProvision provision, String planName) {
        return displayName(subscription, provision, planName);
    }

    private static String safeRemoteEmail(XuiClientProvision provision) {
        if (provision == null) {
            return null;
        }
        if (provision.getRemoteEmail() != null && provision.getRemoteEmail().contains("@")) {
            return null;
        }
        String value = normalize(provision.getRemoteEmail());
        if (value == null) {
            return null;
        }
        if (value.contains("@")) {
            return null;
        }
        return value;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().replaceAll("\\s+", "_");
        if (normalized.isBlank()) {
            return null;
        }
        return normalized.replaceAll("[^\\p{L}\\p{N}._-]", "_");
    }

    private static String bound(String value) {
        if (value.length() <= MAX_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_LENGTH);
    }

    private static String shortSuffix(UUID id) {
        String text = id.toString().replace("-", "");
        return text.substring(0, Math.min(6, text.length()));
    }
}
