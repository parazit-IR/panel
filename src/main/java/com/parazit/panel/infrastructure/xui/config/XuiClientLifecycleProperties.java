package com.parazit.panel.infrastructure.xui.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.xui")
public record XuiClientLifecycleProperties(
        String clientUpdatePathTemplate,
        String clientDeletePathTemplate
) {

    public XuiClientLifecycleProperties {
        if (clientUpdatePathTemplate == null || clientUpdatePathTemplate.isBlank()) {
            clientUpdatePathTemplate = "/panel/api/inbounds/updateClient/{clientId}";
        }
        clientUpdatePathTemplate = normalizeRelativePath(clientUpdatePathTemplate, "app.xui.client-update-path-template");
        if (!clientUpdatePathTemplate.contains("{clientId}")) {
            throw new IllegalArgumentException("app.xui.client-update-path-template must contain {clientId}");
        }
        if (clientDeletePathTemplate == null || clientDeletePathTemplate.isBlank()) {
            clientDeletePathTemplate = "/panel/api/inbounds/{inboundId}/delClient/{clientId}";
        }
        clientDeletePathTemplate = normalizeRelativePath(clientDeletePathTemplate, "app.xui.client-delete-path-template");
        if (!clientDeletePathTemplate.contains("{inboundId}") || !clientDeletePathTemplate.contains("{clientId}")) {
            throw new IllegalArgumentException("app.xui.client-delete-path-template must contain {inboundId} and {clientId}");
        }
    }

    private static String normalizeRelativePath(String value, String propertyName) {
        String trimmed = value.trim();
        if (trimmed.contains("://")) {
            throw new IllegalArgumentException(propertyName + " must be relative to app.xui.base-url");
        }
        if (!trimmed.startsWith("/")) {
            throw new IllegalArgumentException(propertyName + " must start with /");
        }
        return trimmed;
    }
}
