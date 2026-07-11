package com.parazit.panel.infrastructure.xui.client;

import com.parazit.panel.application.port.out.xui.XuiClientStateClient;
import com.parazit.panel.application.port.out.xui.XuiInboundClient;
import com.parazit.panel.application.xui.client.model.GetXuiClientTrafficRequest;
import com.parazit.panel.application.xui.client.model.XuiClientTrafficSnapshot;
import com.parazit.panel.application.xui.model.XuiClientSnapshot;
import com.parazit.panel.infrastructure.xui.authentication.AuthenticatedRequestExecutor;
import com.parazit.panel.infrastructure.xui.config.XuiClientLifecycleProperties;
import com.parazit.panel.infrastructure.xui.dto.client.XuiClientTrafficRemoteDto;
import com.parazit.panel.infrastructure.xui.exception.XuiConnectionException;
import com.parazit.panel.infrastructure.xui.exception.XuiInvalidResponseException;
import com.parazit.panel.infrastructure.xui.exception.XuiTimeoutException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class RestClientXuiClientStateClient implements XuiClientStateClient {

    private final AuthenticatedRequestExecutor requestExecutor;
    private final XuiClientLifecycleProperties lifecycleProperties;
    private final XuiInboundClient inboundClient;

    public RestClientXuiClientStateClient(
            AuthenticatedRequestExecutor requestExecutor,
            XuiClientLifecycleProperties lifecycleProperties,
            XuiInboundClient inboundClient
    ) {
        this.requestExecutor = Objects.requireNonNull(requestExecutor, "requestExecutor must not be null");
        this.lifecycleProperties = Objects.requireNonNull(lifecycleProperties, "lifecycleProperties must not be null");
        this.inboundClient = Objects.requireNonNull(inboundClient, "inboundClient must not be null");
    }

    @Override
    public Optional<XuiClientSnapshot> findClient(long inboundId, String clientId) {
        return inboundClient.findClient(inboundId, clientId, null);
    }

    @Override
    public XuiClientTrafficSnapshot getTraffic(GetXuiClientTrafficRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        try {
            XuiClientTrafficRemoteDto response = requestExecutor.getEntity(
                    expand(lifecycleProperties.clientTrafficPathTemplate(), request.email()),
                    XuiClientTrafficRemoteDto.class
            ).getBody();
            if (response == null) {
                throw new XuiInvalidResponseException("Xui client traffic response is empty");
            }
            if (!Boolean.TRUE.equals(response.success())) {
                throw new XuiInvalidResponseException(safeMessage(response.msg()));
            }
            XuiClientTrafficRemoteDto.ClientTrafficObj obj = response.obj();
            if (obj == null) {
                throw new XuiInvalidResponseException("Xui client traffic response obj is empty");
            }
            long upload = nonNegative(obj.up(), "up");
            long download = nonNegative(obj.down(), "down");
            long configured = nonNegative(obj.total(), "total");
            long consumed = Math.addExact(upload, download);
            Long remaining = configured == 0 ? null : Math.max(0L, configured - consumed);
            return new XuiClientTrafficSnapshot(upload, download, consumed, configured, remaining, Instant.now());
        } catch (XuiTimeoutException | XuiConnectionException exception) {
            throw exception;
        }
    }

    private static long nonNegative(Long value, String field) {
        if (value == null) {
            return 0;
        }
        if (value < 0) {
            throw new XuiInvalidResponseException("Xui client traffic field must not be negative: " + field);
        }
        return value;
    }

    private static String expand(String template, String email) {
        return template.replace("{email}", URLEncoder.encode(email, StandardCharsets.UTF_8));
    }

    private static String safeMessage(String message) {
        if (message == null || message.isBlank()) {
            return "Xui client traffic request failed";
        }
        String trimmed = message.trim();
        return trimmed.length() > 200 ? trimmed.substring(0, 200) : trimmed;
    }
}
