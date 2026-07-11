package com.parazit.panel.application.port.in.xui.client;

import com.parazit.panel.application.xui.client.command.DeleteVpnClientCommand;
import com.parazit.panel.application.xui.client.result.XuiClientLifecycleResult;

public interface DeleteVpnClientUseCase {

    XuiClientLifecycleResult delete(DeleteVpnClientCommand command);
}
