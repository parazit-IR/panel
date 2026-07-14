package com.parazit.panel.infrastructure.persistence.renewal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parazit.panel.application.port.out.renewal.RenewalExecutionPayloadSerializerPort;
import com.parazit.panel.domain.renewal.RenewalExecutionRequest;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class RenewalExecutionPayloadSerializer implements RenewalExecutionPayloadSerializerPort {

    private final ObjectMapper objectMapper;

    public RenewalExecutionPayloadSerializer(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    @Override
    public String serialize(RenewalExecutionRequest request) {
        try {
            return objectMapper.writeValueAsString(Objects.requireNonNull(request, "request must not be null"));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize renewal execution request", exception);
        }
    }

    @Override
    public RenewalExecutionRequest deserialize(String payload) {
        try {
            return objectMapper.readValue(Objects.requireNonNull(payload, "payload must not be null"), RenewalExecutionRequest.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid renewal execution request payload", exception);
        }
    }
}
