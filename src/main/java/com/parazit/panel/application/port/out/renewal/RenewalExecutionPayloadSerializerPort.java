package com.parazit.panel.application.port.out.renewal;

import com.parazit.panel.domain.renewal.RenewalExecutionRequest;

public interface RenewalExecutionPayloadSerializerPort {

    String serialize(RenewalExecutionRequest request);

    RenewalExecutionRequest deserialize(String payload);
}
