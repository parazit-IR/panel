package com.parazit.panel.application.xui.client;

import com.parazit.panel.domain.xui.operation.XuiClientOperation;
import com.parazit.panel.domain.xui.provisioning.XuiClientProvision;

public record PreparedXuiClientUpdateOperation(
        XuiClientProvision provision,
        XuiClientOperation operation,
        boolean claimed,
        boolean replay
) {
}
