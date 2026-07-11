package com.parazit.panel.application.port.in.xui.client;

import com.parazit.panel.application.xui.client.command.SynchronizeVpnClientCommand;
import com.parazit.panel.application.xui.client.result.XuiClientUpdateResult;

public interface SynchronizeVpnClientUseCase {

    XuiClientUpdateResult synchronize(SynchronizeVpnClientCommand command);
}
