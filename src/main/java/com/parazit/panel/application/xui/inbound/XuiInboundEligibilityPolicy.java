package com.parazit.panel.application.xui.inbound;

import com.parazit.panel.application.xui.model.XuiInboundSnapshot;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class XuiInboundEligibilityPolicy {

    public boolean isEligible(XuiInboundSnapshot inbound) {
        return inbound != null
                && inbound.enabled()
                && equalsIgnoreCase(inbound.protocol(), "VLESS")
                && equalsIgnoreCase(inbound.securityType(), "REALITY")
                && inbound.port() >= 1
                && inbound.port() <= 65_535
                && hasText(inbound.publicKey())
                && hasText(inbound.shortId())
                && hasText(inbound.serverName());
    }

    private static boolean equalsIgnoreCase(String actual, String expected) {
        return actual != null && actual.toUpperCase(Locale.ROOT).equals(expected);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
