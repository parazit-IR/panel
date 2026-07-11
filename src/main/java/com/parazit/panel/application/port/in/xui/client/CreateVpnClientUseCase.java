package com.parazit.panel.application.port.in.xui.client;

import com.parazit.panel.application.xui.client.command.CreateVpnClientCommand;
import com.parazit.panel.application.xui.client.result.CreateVpnClientResult;

public interface CreateVpnClientUseCase {

    CreateVpnClientResult create(CreateVpnClientCommand command);
}
