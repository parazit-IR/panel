package com.parazit.panel.infrastructure.persistence.renewal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parazit.panel.application.port.out.renewal.RenewalTargetPayloadSerializerPort;
import com.parazit.panel.application.renewal.result.RenewalApplicationTarget;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class RenewalTargetPayloadSerializer implements RenewalTargetPayloadSerializerPort {

    private final ObjectMapper objectMapper;

    public RenewalTargetPayloadSerializer(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    @Override
    public String serialize(RenewalApplicationTarget target) {
        try {
            return objectMapper.writeValueAsString(Objects.requireNonNull(target, "target must not be null"));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize renewal target", exception);
        }
    }

    @Override
    public RenewalApplicationTarget deserialize(String payload) {
        try {
            return objectMapper.readValue(Objects.requireNonNull(payload, "payload must not be null"), RenewalApplicationTarget.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid renewal target payload", exception);
        }
    }
}
