package com.parazit.panel.application.xui.inbound;

import com.parazit.panel.application.xui.inbound.result.XuiInboundResult;
import com.parazit.panel.application.xui.model.XuiInboundSnapshot;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class XuiInboundResultMapper {

    public XuiInboundResult toResult(XuiInboundSnapshot inbound) {
        List<String> shortIds = inbound.shortId() == null || inbound.shortId().isBlank()
                ? List.of()
                : List.of(inbound.shortId());
        return new XuiInboundResult(
                inbound.id(),
                inbound.remark(),
                inbound.protocol(),
                inbound.port(),
                inbound.enabled(),
                inbound.listenAddress(),
                inbound.totalTrafficLimitBytes(),
                inbound.uploadBytes(),
                inbound.downloadBytes(),
                inbound.expiryTime(),
                inbound.clients().size(),
                inbound.streamNetwork(),
                inbound.securityType(),
                inbound.serverName(),
                inbound.publicKey(),
                shortIds
        );
    }
}
