package com.parazit.panel.application.port.out.xui;

import java.util.UUID;

public interface XuiRemoteEmailGenerator {

    String generate(UUID userId, UUID provisionId);
}
