package com.parazit.panel.application.port.in.xui.inbound;

import com.parazit.panel.application.xui.inbound.query.GetXuiInboundByIdQuery;
import com.parazit.panel.application.xui.inbound.query.GetXuiInboundByRemarkQuery;
import com.parazit.panel.application.xui.inbound.result.XuiInboundResult;

public interface GetXuiInboundUseCase {

    XuiInboundResult getById(GetXuiInboundByIdQuery query);

    XuiInboundResult getByRemark(GetXuiInboundByRemarkQuery query);
}
