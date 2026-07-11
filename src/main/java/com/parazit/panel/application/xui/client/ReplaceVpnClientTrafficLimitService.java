package com.parazit.panel.application.xui.client;

import com.parazit.panel.application.port.in.xui.client.ChangeVpnClientTrafficLimitUseCase;
import com.parazit.panel.application.xui.client.command.ReplaceVpnClientTrafficLimitCommand;
import com.parazit.panel.application.xui.client.result.XuiClientUpdateResult;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ReplaceVpnClientTrafficLimitService implements ChangeVpnClientTrafficLimitUseCase {

    private final XuiClientUpdateWorkflow workflow;

    public ReplaceVpnClientTrafficLimitService(XuiClientUpdateWorkflow workflow) {
        this.workflow = Objects.requireNonNull(workflow, "workflow must not be null");
    }

    @Override
    public XuiClientUpdateResult replaceTrafficLimit(ReplaceVpnClientTrafficLimitCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        return workflow.replaceTrafficLimit(command.operationId(), command.telegramUserId(), command.provisionId(), command.trafficLimitBytes());
    }
}
