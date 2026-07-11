package com.parazit.panel.infrastructure.xui.inbound;

import com.parazit.panel.application.xui.model.XuiInboundSnapshot;
import com.parazit.panel.infrastructure.xui.dto.inbound.XuiInboundRemoteDto;
import com.parazit.panel.infrastructure.xui.exception.XuiInvalidResponseException;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class XuiInboundMapper {

    private final XuiInboundPayloadParser payloadParser;

    public XuiInboundMapper(XuiInboundPayloadParser payloadParser) {
        this.payloadParser = Objects.requireNonNull(payloadParser, "payloadParser must not be null");
    }

    public XuiInboundSnapshot toSnapshot(XuiInboundRemoteDto remote) {
        Objects.requireNonNull(remote, "remote must not be null");
        XuiInboundPayload payload = payloadParser.parse(remote.settings(), remote.streamSettings());
        return new XuiInboundSnapshot(
                requiredPositive(remote.id(), "id"),
                normalizeText(remote.remark()),
                normalizeProtocol(remote.protocol()),
                requiredPort(remote.port()),
                Boolean.TRUE.equals(remote.enabled()),
                normalizeText(remote.listen()),
                nonNegative(remote.total(), "total"),
                nonNegative(remote.up(), "up"),
                nonNegative(remote.down(), "down"),
                toInstant(remote.expiryTime()),
                payload.clients(),
                normalizeText(payload.streamNetwork()),
                normalizeText(payload.securityType()),
                normalizeText(payload.serverName()),
                normalizeText(payload.publicKey()),
                normalizeText(payload.shortId())
        );
    }

    private static long requiredPositive(Long value, String field) {
        if (value == null || value <= 0) {
            throw new XuiInvalidResponseException("Xui inbound field must be positive: " + field);
        }
        return value;
    }

    private static int requiredPort(Integer value) {
        if (value == null || value < 1 || value > 65_535) {
            throw new XuiInvalidResponseException("Xui inbound port is invalid");
        }
        return value;
    }

    private static long nonNegative(Long value, String field) {
        if (value == null) {
            return 0;
        }
        if (value < 0) {
            throw new XuiInvalidResponseException("Xui inbound traffic field must not be negative: " + field);
        }
        return value;
    }

    private static Instant toInstant(Long epochMillis) {
        if (epochMillis == null || epochMillis <= 0) {
            return null;
        }
        return Instant.ofEpochMilli(epochMillis);
    }

    private static String normalizeProtocol(String protocol) {
        if (protocol == null || protocol.isBlank()) {
            throw new XuiInvalidResponseException("Xui inbound protocol must not be blank");
        }
        return protocol.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
