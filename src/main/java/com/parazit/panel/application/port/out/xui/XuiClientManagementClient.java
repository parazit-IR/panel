package com.parazit.panel.application.port.out.xui;

import com.parazit.panel.application.xui.client.model.CreateXuiClientRequest;
import com.parazit.panel.application.xui.client.model.CreateXuiClientResponse;
import com.parazit.panel.application.xui.client.model.DeleteXuiClientRequest;
import com.parazit.panel.application.xui.client.model.DeleteXuiClientResponse;
import com.parazit.panel.application.xui.client.model.DisableXuiClientRequest;
import com.parazit.panel.application.xui.client.model.DisableXuiClientResponse;

public interface XuiClientManagementClient {

    CreateXuiClientResponse createClient(CreateXuiClientRequest request);

    DisableXuiClientResponse disableClient(DisableXuiClientRequest request);

    DeleteXuiClientResponse deleteClient(DeleteXuiClientRequest request);
}
