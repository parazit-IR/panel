package com.parazit.panel.application.xui.client;

import com.parazit.panel.application.port.in.xui.client.EnableVpnClientUseCase;
import com.parazit.panel.application.xui.client.command.EnableVpnClientCommand;
import com.parazit.panel.application.xui.client.result.XuiClientUpdateResult;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class EnableVpnClientService implements EnableVpnClientUseCase {

    private final XuiClientUpdateWorkflow workflow;

    public EnableVpnClientService(XuiClientUpdateWorkflow workflow) {
        this.workflow = Objects.requireNonNull(workflow, "workflow must not be null");
    }

    @Override
    public XuiClientUpdateResult enable(EnableVpnClientCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        return workflow.enable(command.operationId(), command.telegramUserId(), command.provisionId());
    }
}
