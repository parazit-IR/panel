package com.parazit.panel.application.port.in.xui.client;

import com.parazit.panel.application.xui.client.command.DisableVpnClientCommand;
import com.parazit.panel.application.xui.client.result.XuiClientLifecycleResult;

public interface DisableVpnClientUseCase {

    XuiClientLifecycleResult disable(DisableVpnClientCommand command);
}
