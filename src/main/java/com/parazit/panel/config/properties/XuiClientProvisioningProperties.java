package com.parazit.panel.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.xui-client-provisioning")
public record XuiClientProvisioningProperties(
        String defaultFlow,
        Integer reconciliationAttempts
) {

    public XuiClientProvisioningProperties {
        if (defaultFlow == null) {
            defaultFlow = "xtls-rprx-vision";
        }
        defaultFlow = defaultFlow.trim();
        if (reconciliationAttempts == null) {
            reconciliationAttempts = 1;
        }
        if (reconciliationAttempts < 0 || reconciliationAttempts > 3) {
            throw new IllegalArgumentException("app.xui-client-provisioning.reconciliation-attempts must be between 0 and 3");
        }
    }
}
