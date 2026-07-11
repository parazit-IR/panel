package com.parazit.panel.application.port.in.xui.client;

import com.parazit.panel.application.xui.client.command.ChangeVpnClientIpLimitCommand;
import com.parazit.panel.application.xui.client.result.XuiClientUpdateResult;

public interface ChangeVpnClientIpLimitUseCase {

    XuiClientUpdateResult changeIpLimit(ChangeVpnClientIpLimitCommand command);
}
