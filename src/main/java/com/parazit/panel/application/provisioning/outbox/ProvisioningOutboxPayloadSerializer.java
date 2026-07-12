package com.parazit.panel.application.provisioning.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class ProvisioningOutboxPayloadSerializer {

    public static final String CREATE_VPN_CLIENT_V1 = "create-vpn-client.v1";

    private final ObjectMapper objectMapper;

    public ProvisioningOutboxPayloadSerializer(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    public String serialize(CreateVpnProvisioningPayloadV1 payload) {
        try {
            return objectMapper.writeValueAsString(Objects.requireNonNull(payload, "payload must not be null"));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize provisioning payload", exception);
        }
    }

    public CreateVpnProvisioningPayloadV1 deserializeCreateVpnClient(String payload) {
        try {
            return objectMapper.readValue(payload, CreateVpnProvisioningPayloadV1.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid provisioning payload", exception);
        }
    }
}
