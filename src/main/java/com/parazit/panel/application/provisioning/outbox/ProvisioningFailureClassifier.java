package com.parazit.panel.application.provisioning.outbox;

import com.parazit.panel.application.xui.client.XuiClientProvisionFailedException;
import com.parazit.panel.application.xui.client.XuiClientProvisionNotAllowedException;
import com.parazit.panel.application.xui.client.XuiClientProvisionNotFoundException;
import com.parazit.panel.application.xui.client.XuiClientProvisionUnknownException;
import org.springframework.stereotype.Component;

@Component
public class ProvisioningFailureClassifier {

    public ProvisioningFailure classify(RuntimeException exception) {
        if (exception instanceof XuiClientProvisionUnknownException) {
            return new ProvisioningFailure(true, true, "XUI_UNKNOWN", "Provisioning result is unknown");
        }
        String simpleName = exception.getClass().getSimpleName();
        if (simpleName.contains("Connection")
                || simpleName.contains("Timeout")
                || simpleName.contains("Authentication")
                || simpleName.contains("Server")) {
            return new ProvisioningFailure(true, false, "XUI_TRANSIENT", "Provisioning gateway is temporarily unavailable");
        }
        if (exception instanceof XuiClientProvisionFailedException) {
            return new ProvisioningFailure(true, false, "XUI_FAILED", exception.getMessage());
        }
        if (exception instanceof XuiClientProvisionNotAllowedException
                || exception instanceof XuiClientProvisionNotFoundException
                || exception instanceof IllegalArgumentException) {
            return new ProvisioningFailure(false, false, "PROVISIONING_INVALID", exception.getMessage());
        }
        return new ProvisioningFailure(true, false, "PROVISIONING_ERROR", "Provisioning failed");
    }
}
