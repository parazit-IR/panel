package com.parazit.panel.application.port.in.xui.client;

import com.parazit.panel.application.xui.client.command.EnableVpnClientCommand;
import com.parazit.panel.application.xui.client.result.XuiClientUpdateResult;

public interface EnableVpnClientUseCase {

    XuiClientUpdateResult enable(EnableVpnClientCommand command);
}
