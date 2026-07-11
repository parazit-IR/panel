package com.parazit.panel.application.port.out.xui;

import com.parazit.panel.application.xui.client.model.CreateXuiClientRequest;
import com.parazit.panel.application.xui.client.model.CreateXuiClientResponse;

public interface XuiClientManagementClient {

    CreateXuiClientResponse createClient(CreateXuiClientRequest request);
}
