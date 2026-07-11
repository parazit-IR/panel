package com.parazit.panel.infrastructure.xui.inbound;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parazit.panel.application.xui.model.XuiClientSnapshot;
import com.parazit.panel.infrastructure.xui.exception.XuiInvalidResponseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class XuiInboundPayloadParser {

    private final ObjectMapper objectMapper;

    public XuiInboundPayloadParser(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    XuiInboundPayload parse(String settings, String streamSettings) {
        JsonNode settingsRoot = parseObject(settings, "settings");
        JsonNode streamRoot = parseObject(streamSettings, "streamSettings");
        return new XuiInboundPayload(
                parseClients(settingsRoot),
                textAt(streamRoot, "network"),
                uppercase(textAt(streamRoot, "security")),
                firstText(path(streamRoot, "realitySettings", "serverNames")),
                firstPresentText(
                        path(streamRoot, "realitySettings", "settings", "publicKey"),
                        path(streamRoot, "realitySettings", "publicKey")
                ),
                firstText(path(streamRoot, "realitySettings", "shortIds"))
        );
    }

    List<XuiClientSnapshot> parseClients(String settings) {
        return parseClients(parseObject(settings, "settings"));
    }

    Instant toInstant(Long epochMillis) {
        if (epochMillis == null || epochMillis <= 0) {
            return null;
        }
        return Instant.ofEpochMilli(epochMillis);
    }

    private List<XuiClientSnapshot> parseClients(JsonNode root) {
        JsonNode clients = root.path("clients");
        if (clients.isMissingNode() || clients.isNull()) {
            return List.of();
        }
        if (!clients.isArray()) {
            throw new XuiInvalidResponseException("Xui inbound settings clients must be an array");
        }
        List<XuiClientSnapshot> snapshots = new ArrayList<>();
        for (JsonNode client : clients) {
            snapshots.add(new XuiClientSnapshot(
                    textAt(client, "id", "clientId"),
                    textAt(client, "email"),
                    booleanAt(client, true, "enable", "enabled"),
                    nonNegativeLong(client, "client.totalTrafficLimitBytes", "totalGB", "total"),
                    nonNegativeLong(client, "client.uploadBytes", "up", "upload"),
                    nonNegativeLong(client, "client.downloadBytes", "down", "download"),
                    toInstant(longAt(client, "expiryTime")),
                    nonNegativeInt(client, "client.ipLimit", 0, "limitIp", "ipLimit"),
                    textAt(client, "subId", "subscriptionId")
            ));
        }
        return List.copyOf(snapshots);
    }

    private JsonNode parseObject(String payload, String fieldName) {
        if (payload == null || payload.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            JsonNode root = objectMapper.readTree(payload);
            if (root == null || !root.isObject()) {
                throw new XuiInvalidResponseException("Xui inbound " + fieldName + " must be a JSON object");
            }
            return root;
        } catch (JsonProcessingException exception) {
            throw new XuiInvalidResponseException("Xui inbound " + fieldName + " is malformed", exception);
        }
    }

    private static JsonNode path(JsonNode root, String... names) {
        JsonNode current = root;
        for (String name : names) {
            if (current == null || current.isMissingNode() || current.isNull()) {
                return null;
            }
            current = current.path(name);
        }
        return current;
    }

    private static String firstPresentText(JsonNode first, JsonNode second) {
        return Optional.ofNullable(firstText(first))
                .orElseGet(() -> firstText(second));
    }

    private static String firstText(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                String value = firstText(item);
                if (value != null) {
                    return value;
                }
            }
            return null;
        }
        if (node.isTextual() || node.isNumber() || node.isBoolean()) {
            String value = node.asText();
            return value.isBlank() ? null : value.trim();
        }
        return null;
    }

    private static String textAt(JsonNode node, String... names) {
        for (String name : names) {
            String value = firstText(node.path(name));
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static String uppercase(String value) {
        return value == null ? null : value.toUpperCase();
    }

    private static boolean booleanAt(JsonNode node, boolean defaultValue, String... names) {
        for (String name : names) {
            JsonNode value = node.path(name);
            if (value.isBoolean()) {
                return value.asBoolean();
            }
            if (value.isTextual()) {
                return Boolean.parseBoolean(value.asText());
            }
        }
        return defaultValue;
    }

    private static Long longAt(JsonNode node, String name) {
        JsonNode value = node.path(name);
        if (value.isMissingNode() || value.isNull() || value.asText().isBlank()) {
            return null;
        }
        if (!value.canConvertToLong()) {
            throw new XuiInvalidResponseException("Xui numeric field is invalid: " + name);
        }
        return value.asLong();
    }

    private static long nonNegativeLong(JsonNode node, String label, String... names) {
        for (String name : names) {
            Long value = longAt(node, name);
            if (value != null) {
                if (value < 0) {
                    throw new XuiInvalidResponseException("Xui traffic field must not be negative: " + label);
                }
                return value;
            }
        }
        return 0;
    }

    private static int nonNegativeInt(JsonNode node, String label, int defaultValue, String... names) {
        for (String name : names) {
            JsonNode value = node.path(name);
            if (!value.isMissingNode() && !value.isNull() && !value.asText().isBlank()) {
                if (!value.canConvertToInt()) {
                    throw new XuiInvalidResponseException("Xui integer field is invalid: " + label);
                }
                int result = value.asInt();
                if (result < 0) {
                    throw new XuiInvalidResponseException("Xui integer field must not be negative: " + label);
                }
                return result;
            }
        }
        return defaultValue;
    }
}
