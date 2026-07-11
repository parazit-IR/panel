package com.parazit.panel.application.port.in.xui.inbound;

import com.parazit.panel.application.xui.inbound.query.FindEligibleXuiInboundQuery;
import com.parazit.panel.application.xui.inbound.result.XuiInboundResult;

public interface FindEligibleXuiInboundUseCase {

    XuiInboundResult findEligible(FindEligibleXuiInboundQuery query);
}
