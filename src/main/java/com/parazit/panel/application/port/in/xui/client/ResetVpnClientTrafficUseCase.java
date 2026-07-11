package com.parazit.panel.application.port.in.xui.client;

import com.parazit.panel.application.xui.client.command.ResetVpnClientTrafficCommand;
import com.parazit.panel.application.xui.client.result.XuiClientUpdateResult;

public interface ResetVpnClientTrafficUseCase {

    XuiClientUpdateResult resetTraffic(ResetVpnClientTrafficCommand command);
}
