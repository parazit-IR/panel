package com.parazit.panel.application.subscription.render;

import com.parazit.panel.application.subscription.UnsupportedInboundConfigurationException;
import com.parazit.panel.application.subscription.model.VlessSubscriptionConfig;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class VlessUriBuilder {

    public String build(VlessSubscriptionConfig config) {
        Objects.requireNonNull(config, "config must not be null");
        validate(config);
        Map<String, String> query = new LinkedHashMap<>();
        put(query, "encryption", defaultValue(config.encryption(), "none"));
        put(query, "security", config.security());
        put(query, "sni", config.sni());
        put(query, "fp", config.fingerprint());
        put(query, "pbk", config.publicKey());
        put(query, "sid", config.shortId());
        put(query, "type", config.transportType());
        put(query, "flow", config.flow());
        put(query, "path", config.path());
        put(query, "host", config.host());
        put(query, "mode", config.mode());
        put(query, "serviceName", config.serviceName());

        StringBuilder builder = new StringBuilder();
        builder.append("vless://")
                .append(config.clientId())
                .append('@')
                .append(formatHost(config.address()))
                .append(':')
                .append(config.port());
        if (!query.isEmpty()) {
            builder.append('?');
            boolean first = true;
            for (Map.Entry<String, String> entry : query.entrySet()) {
                if (!first) {
                    builder.append('&');
                }
                first = false;
                builder.append(encode(entry.getKey())).append('=').append(encode(entry.getValue()));
            }
        }
        if (config.remark() != null && !config.remark().isBlank()) {
            builder.append('#').append(encode(config.remark().trim()));
        }
        return builder.toString();
    }

    private static void validate(VlessSubscriptionConfig config) {
        Objects.requireNonNull(config.clientId(), "clientId must not be null");
        requireText(config.address(), "address");
        if (config.port() < 1 || config.port() > 65_535) {
            throw new UnsupportedInboundConfigurationException("VLESS port is invalid");
        }
        if (!"none".equalsIgnoreCase(defaultValue(config.encryption(), "none"))) {
            throw new UnsupportedInboundConfigurationException("Only VLESS encryption=none is supported");
        }
        if (!"reality".equalsIgnoreCase(config.security())) {
            throw new UnsupportedInboundConfigurationException("Only VLESS REALITY is supported");
        }
        String transport = defaultValue(config.transportType(), "tcp");
        if (!"tcp".equalsIgnoreCase(transport) && !"xhttp".equalsIgnoreCase(transport)) {
            throw new UnsupportedInboundConfigurationException("Unsupported VLESS transport");
        }
        requireText(config.sni(), "sni");
        requireText(config.publicKey(), "publicKey");
        requireText(config.shortId(), "shortId");
        requireText(config.fingerprint(), "fingerprint");
    }

    private static void put(Map<String, String> values, String name, String value) {
        if (value != null && !value.isBlank()) {
            values.put(name, value.trim());
        }
    }

    private static String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new UnsupportedInboundConfigurationException("VLESS " + fieldName + " is required");
        }
    }

    private static String formatHost(String host) {
        String trimmed = host.trim();
        if (trimmed.contains(":") && !trimmed.startsWith("[") && !trimmed.endsWith("]")) {
            return "[" + trimmed + "]";
        }
        return trimmed;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
