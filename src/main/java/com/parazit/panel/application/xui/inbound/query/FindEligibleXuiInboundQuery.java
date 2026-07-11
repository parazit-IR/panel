package com.parazit.panel.application.xui.inbound.query;

public record FindEligibleXuiInboundQuery(
        String protocol,
        String securityType,
        String streamNetwork,
        Boolean requireEnabled
) {

    public FindEligibleXuiInboundQuery {
        protocol = normalize(protocol);
        securityType = normalize(securityType);
        streamNetwork = normalize(streamNetwork);
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase();
    }
}
