package com.parazit.panel.api.internal.xui.inbound;

import com.parazit.panel.application.xui.inbound.query.FindEligibleXuiInboundQuery;
import com.parazit.panel.application.xui.inbound.query.GetXuiInboundByIdQuery;
import com.parazit.panel.application.xui.inbound.query.GetXuiInboundByRemarkQuery;
import com.parazit.panel.application.xui.inbound.result.XuiInboundResult;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class XuiInboundApiMapper {

    public GetXuiInboundByIdQuery toGetByIdQuery(long inboundId) {
        return new GetXuiInboundByIdQuery(inboundId);
    }

    public GetXuiInboundByRemarkQuery toGetByRemarkQuery(String remark) {
        return new GetXuiInboundByRemarkQuery(remark);
    }

    public FindEligibleXuiInboundQuery toFindEligibleQuery(
            String protocol,
            String security,
            String network
    ) {
        return new FindEligibleXuiInboundQuery(protocol, security, network, true);
    }

    public List<XuiInboundResponse> toResponse(List<XuiInboundResult> results) {
        return results.stream()
                .map(this::toResponse)
                .toList();
    }

    public XuiInboundResponse toResponse(XuiInboundResult result) {
        return new XuiInboundResponse(
                result.id(),
                result.remark(),
                result.protocol(),
                result.port(),
                result.enabled(),
                result.listenAddress(),
                result.totalTrafficLimitBytes(),
                result.uploadBytes(),
                result.downloadBytes(),
                result.expiryTime(),
                result.clientCount(),
                result.streamNetwork(),
                result.securityType(),
                result.serverName(),
                result.publicKey(),
                result.shortIds()
        );
    }
}
