package com.parazit.panel.application.xui.client;

import com.parazit.panel.application.port.in.xui.client.ResetVpnClientTrafficUseCase;
import com.parazit.panel.application.xui.client.command.ResetVpnClientTrafficCommand;
import com.parazit.panel.application.xui.client.result.XuiClientUpdateResult;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ResetVpnClientTrafficService implements ResetVpnClientTrafficUseCase {

    private final XuiClientUpdateWorkflow workflow;

    public ResetVpnClientTrafficService(XuiClientUpdateWorkflow workflow) {
        this.workflow = Objects.requireNonNull(workflow, "workflow must not be null");
    }

    @Override
    public XuiClientUpdateResult resetTraffic(ResetVpnClientTrafficCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        return workflow.resetTraffic(command.operationId(), command.telegramUserId(), command.provisionId());
    }
}
