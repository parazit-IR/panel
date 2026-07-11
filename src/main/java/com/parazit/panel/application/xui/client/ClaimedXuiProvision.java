package com.parazit.panel.application.xui.client;

import com.parazit.panel.domain.xui.provisioning.XuiClientProvision;

record ClaimedXuiProvision(
        XuiClientProvision provision,
        boolean claimed
) {
}
