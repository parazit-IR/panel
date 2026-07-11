package com.parazit.panel.application.xui.client;

import com.parazit.panel.domain.xui.provisioning.XuiClientProvision;

record PreparedXuiClientLifecycleOperation(
        XuiClientProvision provision,
        boolean claimed,
        boolean idempotent
) {
}
