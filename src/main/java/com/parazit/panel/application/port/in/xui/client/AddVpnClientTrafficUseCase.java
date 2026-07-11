package com.parazit.panel.application.port.in.xui.client;

import com.parazit.panel.application.xui.client.command.AddVpnClientTrafficCommand;
import com.parazit.panel.application.xui.client.result.XuiClientUpdateResult;

public interface AddVpnClientTrafficUseCase {

    XuiClientUpdateResult addTraffic(AddVpnClientTrafficCommand command);
}
