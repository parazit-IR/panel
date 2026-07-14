package com.parazit.panel.application.port.out.renewal;

import com.parazit.panel.application.renewal.result.RenewalApplicationTarget;

public interface RenewalTargetPayloadSerializerPort {

    String serialize(RenewalApplicationTarget target);

    RenewalApplicationTarget deserialize(String payload);
}
