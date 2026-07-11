package com.parazit.panel.application.xui.client;

import com.parazit.panel.application.port.in.xui.client.ChangeVpnClientIpLimitUseCase;
import com.parazit.panel.application.xui.client.command.ChangeVpnClientIpLimitCommand;
import com.parazit.panel.application.xui.client.result.XuiClientUpdateResult;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ChangeVpnClientIpLimitService implements ChangeVpnClientIpLimitUseCase {

    private final XuiClientUpdateWorkflow workflow;

    public ChangeVpnClientIpLimitService(XuiClientUpdateWorkflow workflow) {
        this.workflow = Objects.requireNonNull(workflow, "workflow must not be null");
    }

    @Override
    public XuiClientUpdateResult changeIpLimit(ChangeVpnClientIpLimitCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        return workflow.changeIpLimit(command.operationId(), command.telegramUserId(), command.provisionId(), command.ipLimit());
    }
}
