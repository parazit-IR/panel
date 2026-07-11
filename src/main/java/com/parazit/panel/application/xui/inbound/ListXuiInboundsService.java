package com.parazit.panel.application.xui.inbound;

import com.parazit.panel.application.port.in.xui.inbound.ListXuiInboundsUseCase;
import com.parazit.panel.application.port.out.xui.XuiInboundClient;
import com.parazit.panel.application.xui.inbound.result.XuiInboundResult;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ListXuiInboundsService implements ListXuiInboundsUseCase {

    private final XuiInboundClient inboundClient;
    private final XuiInboundResultMapper mapper;

    public ListXuiInboundsService(XuiInboundClient inboundClient, XuiInboundResultMapper mapper) {
        this.inboundClient = Objects.requireNonNull(inboundClient, "inboundClient must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public List<XuiInboundResult> list() {
        return inboundClient.getInbounds()
                .stream()
                .map(mapper::toResult)
                .toList();
    }
}
