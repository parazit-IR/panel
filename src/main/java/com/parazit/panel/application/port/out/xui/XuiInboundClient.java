package com.parazit.panel.application.port.out.xui;

import com.parazit.panel.application.xui.model.XuiInboundSnapshot;
import java.util.List;
import java.util.Optional;

public interface XuiInboundClient {

    List<XuiInboundSnapshot> getInbounds();

    Optional<XuiInboundSnapshot> getInboundById(long inboundId);
}
