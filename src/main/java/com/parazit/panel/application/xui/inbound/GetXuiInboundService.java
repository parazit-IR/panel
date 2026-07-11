package com.parazit.panel.application.xui.inbound;

import com.parazit.panel.application.port.in.xui.inbound.GetXuiInboundUseCase;
import com.parazit.panel.application.port.out.xui.XuiInboundClient;
import com.parazit.panel.application.xui.inbound.query.GetXuiInboundByIdQuery;
import com.parazit.panel.application.xui.inbound.query.GetXuiInboundByRemarkQuery;
import com.parazit.panel.application.xui.inbound.result.XuiInboundResult;
import com.parazit.panel.application.xui.model.XuiInboundSnapshot;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class GetXuiInboundService implements GetXuiInboundUseCase {

    private final XuiInboundClient inboundClient;
    private final XuiInboundResultMapper mapper;

    public GetXuiInboundService(XuiInboundClient inboundClient, XuiInboundResultMapper mapper) {
        this.inboundClient = Objects.requireNonNull(inboundClient, "inboundClient must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public XuiInboundResult getById(GetXuiInboundByIdQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        return inboundClient.getInboundById(query.inboundId())
                .map(mapper::toResult)
                .orElseThrow(() -> new XuiInboundNotFoundException(query.inboundId()));
    }

    @Override
    public XuiInboundResult getByRemark(GetXuiInboundByRemarkQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        List<XuiInboundSnapshot> matches = inboundClient.getInbounds()
                .stream()
                .filter(inbound -> inbound.remark() != null && inbound.remark().equalsIgnoreCase(query.remark()))
                .toList();
        if (matches.isEmpty()) {
            throw new XuiInboundNotFoundException(query.remark());
        }
        if (matches.size() > 1) {
            throw new XuiInboundAmbiguousException(query.remark());
        }
        return mapper.toResult(matches.getFirst());
    }
}
