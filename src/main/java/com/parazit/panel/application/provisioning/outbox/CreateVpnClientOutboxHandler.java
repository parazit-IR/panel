package com.parazit.panel.application.provisioning.outbox;

import com.parazit.panel.application.port.in.xui.client.CreateVpnClientUseCase;
import com.parazit.panel.application.xui.client.command.CreateVpnClientCommand;
import com.parazit.panel.application.xui.client.result.CreateVpnClientResult;
import com.parazit.panel.domain.provisioning.outbox.ProvisioningOutbox;
import com.parazit.panel.domain.provisioning.outbox.ProvisioningOutboxType;
import com.parazit.panel.domain.xui.provisioning.XuiProvisionStatus;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class CreateVpnClientOutboxHandler {

    private final ProvisioningOutboxPayloadSerializer serializer;
    private final CreateVpnClientUseCase createVpnClientUseCase;

    public CreateVpnClientOutboxHandler(
            ProvisioningOutboxPayloadSerializer serializer,
            CreateVpnClientUseCase createVpnClientUseCase
    ) {
        this.serializer = Objects.requireNonNull(serializer, "serializer must not be null");
        this.createVpnClientUseCase = Objects.requireNonNull(createVpnClientUseCase, "createVpnClientUseCase must not be null");
    }

    public void handle(ProvisioningOutbox outbox) {
        Objects.requireNonNull(outbox, "outbox must not be null");
        if (outbox.getType() != ProvisioningOutboxType.CREATE_VPN_CLIENT) {
            throw new IllegalArgumentException("Unsupported provisioning outbox type");
        }
        if (!ProvisioningOutboxPayloadSerializer.CREATE_VPN_CLIENT_V1.equals(outbox.getPayloadVersion())) {
            throw new IllegalArgumentException("Unsupported provisioning payload version");
        }
        CreateVpnProvisioningPayloadV1 payload = serializer.deserializeCreateVpnClient(outbox.getPayload());
        CreateVpnClientResult result = createVpnClientUseCase.create(new CreateVpnClientCommand(
                payload.telegramUserId(),
                payload.planSelectionId(),
                payload.preferredInboundId()
        ));
        if (result.status() != XuiProvisionStatus.ACTIVE) {
            throw new IllegalStateException("Provisioning did not reach active state");
        }
    }
}
