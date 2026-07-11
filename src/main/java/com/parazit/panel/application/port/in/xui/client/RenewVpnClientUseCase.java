package com.parazit.panel.application.port.in.xui.client;

import com.parazit.panel.application.xui.client.command.RenewVpnClientCommand;
import com.parazit.panel.application.xui.client.result.XuiClientUpdateResult;

public interface RenewVpnClientUseCase {

    XuiClientUpdateResult renew(RenewVpnClientCommand command);
}
