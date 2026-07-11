package com.parazit.panel.application.port.out.xui;

import com.parazit.panel.application.xui.client.model.GetXuiClientTrafficRequest;
import com.parazit.panel.application.xui.client.model.XuiClientTrafficSnapshot;
import com.parazit.panel.application.xui.model.XuiClientSnapshot;
import java.util.Optional;

public interface XuiClientStateClient {

    Optional<XuiClientSnapshot> findClient(long inboundId, String clientId);

    XuiClientTrafficSnapshot getTraffic(GetXuiClientTrafficRequest request);
}
