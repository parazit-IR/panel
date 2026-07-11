package com.parazit.panel.application.xui.client;

import com.parazit.panel.application.port.in.xui.client.SynchronizeVpnClientUseCase;
import com.parazit.panel.application.xui.client.command.SynchronizeVpnClientCommand;
import com.parazit.panel.application.xui.client.result.XuiClientUpdateResult;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class SynchronizeVpnClientService implements SynchronizeVpnClientUseCase {

    private final XuiClientUpdateWorkflow workflow;

    public SynchronizeVpnClientService(XuiClientUpdateWorkflow workflow) {
        this.workflow = Objects.requireNonNull(workflow, "workflow must not be null");
    }

    @Override
    public XuiClientUpdateResult synchronize(SynchronizeVpnClientCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        return workflow.synchronize(command.operationId(), command.telegramUserId(), command.provisionId());
    }
}
