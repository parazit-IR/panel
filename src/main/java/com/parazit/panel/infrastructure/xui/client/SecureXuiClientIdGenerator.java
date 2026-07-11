package com.parazit.panel.infrastructure.xui.client;

import com.parazit.panel.application.port.out.xui.XuiClientIdGenerator;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class SecureXuiClientIdGenerator implements XuiClientIdGenerator {

    @Override
    public String generateClientId() {
        return UUID.randomUUID().toString();
    }
}
