package com.parazit.panel.application.xui.client;

import com.parazit.panel.application.port.in.xui.client.AddVpnClientTrafficUseCase;
import com.parazit.panel.application.xui.client.command.AddVpnClientTrafficCommand;
import com.parazit.panel.application.xui.client.result.XuiClientUpdateResult;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class AddVpnClientTrafficService implements AddVpnClientTrafficUseCase {

    private final XuiClientUpdateWorkflow workflow;

    public AddVpnClientTrafficService(XuiClientUpdateWorkflow workflow) {
        this.workflow = Objects.requireNonNull(workflow, "workflow must not be null");
    }

    @Override
    public XuiClientUpdateResult addTraffic(AddVpnClientTrafficCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        return workflow.addTraffic(command.operationId(), command.telegramUserId(), command.provisionId(), command.additionalTrafficBytes());
    }
}
