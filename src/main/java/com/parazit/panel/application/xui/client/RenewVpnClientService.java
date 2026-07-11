package com.parazit.panel.application.xui.client;

import com.parazit.panel.application.port.in.xui.client.RenewVpnClientUseCase;
import com.parazit.panel.application.xui.client.command.RenewVpnClientCommand;
import com.parazit.panel.application.xui.client.result.XuiClientUpdateResult;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class RenewVpnClientService implements RenewVpnClientUseCase {

    private final XuiClientUpdateWorkflow workflow;

    public RenewVpnClientService(XuiClientUpdateWorkflow workflow) {
        this.workflow = Objects.requireNonNull(workflow, "workflow must not be null");
    }

    @Override
    public XuiClientUpdateResult renew(RenewVpnClientCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        return workflow.renew(command.operationId(), command.telegramUserId(), command.provisionId(), command.durationDays(), command.renewalMode());
    }
}
