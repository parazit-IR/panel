package com.parazit.panel.infrastructure.xui.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.xui")
public record XuiClientLifecycleProperties(
        String clientUpdatePathTemplate,
        String clientDeletePathTemplate,
        String clientResetTrafficPathTemplate,
        String clientTrafficPathTemplate,
        String clientClearIpsPathTemplate
) {

    public XuiClientLifecycleProperties {
        if (clientUpdatePathTemplate == null || clientUpdatePathTemplate.isBlank()) {
            clientUpdatePathTemplate = "/panel/api/inbounds/updateClient/{clientId}";
        }
        clientUpdatePathTemplate = normalizeRelativePath(clientUpdatePathTemplate, "app.xui.client-update-path-template");
        if (!clientUpdatePathTemplate.contains("{clientId}") && !clientUpdatePathTemplate.contains("{email}")) {
            throw new IllegalArgumentException("app.xui.client-update-path-template must contain {clientId} or {email}");
        }
        if (clientDeletePathTemplate == null || clientDeletePathTemplate.isBlank()) {
            clientDeletePathTemplate = "/panel/api/inbounds/{inboundId}/delClient/{clientId}";
        }
        clientDeletePathTemplate = normalizeRelativePath(clientDeletePathTemplate, "app.xui.client-delete-path-template");
        if (!clientDeletePathTemplate.contains("{inboundId}") || !clientDeletePathTemplate.contains("{clientId}")) {
            throw new IllegalArgumentException("app.xui.client-delete-path-template must contain {inboundId} and {clientId}");
        }
        if (clientResetTrafficPathTemplate == null || clientResetTrafficPathTemplate.isBlank()) {
            clientResetTrafficPathTemplate = "/panel/api/clients/resetTraffic/{email}";
        }
        clientResetTrafficPathTemplate = normalizeRelativePath(clientResetTrafficPathTemplate, "app.xui.client-reset-traffic-path-template");
        if (!clientResetTrafficPathTemplate.contains("{email}")) {
            throw new IllegalArgumentException("app.xui.client-reset-traffic-path-template must contain {email}");
        }
        if (clientTrafficPathTemplate == null || clientTrafficPathTemplate.isBlank()) {
            clientTrafficPathTemplate = "/panel/api/clients/traffic/{email}";
        }
        clientTrafficPathTemplate = normalizeRelativePath(clientTrafficPathTemplate, "app.xui.client-traffic-path-template");
        if (!clientTrafficPathTemplate.contains("{email}")) {
            throw new IllegalArgumentException("app.xui.client-traffic-path-template must contain {email}");
        }
        if (clientClearIpsPathTemplate == null || clientClearIpsPathTemplate.isBlank()) {
            clientClearIpsPathTemplate = "/panel/api/clients/clearIps/{email}";
        }
        clientClearIpsPathTemplate = normalizeRelativePath(clientClearIpsPathTemplate, "app.xui.client-clear-ips-path-template");
        if (!clientClearIpsPathTemplate.contains("{email}")) {
            throw new IllegalArgumentException("app.xui.client-clear-ips-path-template must contain {email}");
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
