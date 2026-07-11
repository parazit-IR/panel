package com.parazit.panel.infrastructure.xui.inbound;

import com.parazit.panel.application.port.out.xui.XuiInboundClient;
import com.parazit.panel.application.xui.model.XuiClientSnapshot;
import com.parazit.panel.application.xui.model.XuiInboundSnapshot;
import com.parazit.panel.infrastructure.xui.authentication.AuthenticatedRequestExecutor;
import com.parazit.panel.infrastructure.xui.config.XuiProperties;
import com.parazit.panel.infrastructure.xui.dto.inbound.XuiInboundListResponse;
import com.parazit.panel.infrastructure.xui.exception.XuiInvalidResponseException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RestClientXuiInboundClient implements XuiInboundClient {

    private static final Logger log = LoggerFactory.getLogger(RestClientXuiInboundClient.class);

    private final AuthenticatedRequestExecutor requestExecutor;
    private final XuiProperties properties;
    private final XuiInboundMapper mapper;

    public RestClientXuiInboundClient(
            AuthenticatedRequestExecutor requestExecutor,
            XuiProperties properties,
            XuiInboundMapper mapper
    ) {
        this.requestExecutor = Objects.requireNonNull(requestExecutor, "requestExecutor must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public List<XuiInboundSnapshot> getInbounds() {
        log.debug("Requesting Xui inbound list");
        XuiInboundListResponse response = requestExecutor.get(
                properties.inboundListPath(),
                XuiInboundListResponse.class
        );
        if (response == null) {
            throw new XuiInvalidResponseException("Xui inbound list response is empty");
        }
        if (!Boolean.TRUE.equals(response.success())) {
            throw new XuiInvalidResponseException("Xui inbound list request was not successful");
        }
        if (response.obj() == null || response.obj().isEmpty()) {
            log.debug("Received empty Xui inbound list");
            return List.of();
        }
        List<XuiInboundSnapshot> inbounds = response.obj()
                .stream()
                .map(mapper::toSnapshot)
                .toList();
        log.atDebug()
                .addKeyValue("inboundCount", inbounds.size())
                .log("Received Xui inbound list");
        return inbounds;
    }

    @Override
    public Optional<XuiInboundSnapshot> getInboundById(long inboundId) {
        if (inboundId <= 0) {
            throw new IllegalArgumentException("inboundId must be positive");
        }
        return getInbounds()
                .stream()
                .filter(inbound -> inbound.id() == inboundId)
                .findFirst();
    }

    @Override
    public Optional<XuiClientSnapshot> findClient(long inboundId, String clientId, String email) {
        return getInboundById(inboundId)
                .stream()
                .flatMap(inbound -> inbound.clients().stream())
                .filter(client -> matches(client, clientId, email))
                .findFirst();
    }

    private static boolean matches(XuiClientSnapshot client, String clientId, String email) {
        boolean clientIdMatches = clientId != null
                && !clientId.isBlank()
                && client.clientId() != null
                && client.clientId().equalsIgnoreCase(clientId);
        if (clientId != null && !clientId.isBlank()) {
            return clientIdMatches;
        }
        boolean emailMatches = email != null
                && !email.isBlank()
                && client.email() != null
                && client.email().equalsIgnoreCase(email);
        return emailMatches;
    }
}
