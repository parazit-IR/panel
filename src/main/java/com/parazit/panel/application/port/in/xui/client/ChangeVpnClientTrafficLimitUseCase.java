package com.parazit.panel.application.port.in.xui.client;

import com.parazit.panel.application.xui.client.command.ReplaceVpnClientTrafficLimitCommand;
import com.parazit.panel.application.xui.client.result.XuiClientUpdateResult;

public interface ChangeVpnClientTrafficLimitUseCase {

    XuiClientUpdateResult replaceTrafficLimit(ReplaceVpnClientTrafficLimitCommand command);
}
