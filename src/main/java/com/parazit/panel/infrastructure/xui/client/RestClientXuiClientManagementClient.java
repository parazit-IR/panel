package com.parazit.panel.infrastructure.xui.client;

import com.parazit.panel.application.port.out.xui.XuiInboundClient;
import com.parazit.panel.application.port.out.xui.XuiClientManagementClient;
import com.parazit.panel.application.xui.client.XuiClientCreateConnectionException;
import com.parazit.panel.application.xui.client.XuiClientCreateRejectedException;
import com.parazit.panel.application.xui.client.XuiClientCreateTimeoutException;
import com.parazit.panel.application.xui.client.XuiClientRemoteOperationConnectionException;
import com.parazit.panel.application.xui.client.XuiClientRemoteOperationRejectedException;
import com.parazit.panel.application.xui.client.XuiClientRemoteOperationTimeoutException;
import com.parazit.panel.application.xui.client.XuiRemoteClientIdentityMismatchException;
import com.parazit.panel.application.xui.client.model.CreateXuiClientRequest;
import com.parazit.panel.application.xui.client.model.CreateXuiClientResponse;
import com.parazit.panel.application.xui.client.model.DeleteXuiClientRequest;
import com.parazit.panel.application.xui.client.model.DeleteXuiClientResponse;
import com.parazit.panel.application.xui.client.model.DisableXuiClientRequest;
import com.parazit.panel.application.xui.client.model.DisableXuiClientResponse;
import com.parazit.panel.application.xui.model.XuiClientSnapshot;
import com.parazit.panel.infrastructure.xui.authentication.AuthenticatedRequestExecutor;
import com.parazit.panel.infrastructure.xui.config.XuiClientLifecycleProperties;
import com.parazit.panel.infrastructure.xui.config.XuiProperties;
import com.parazit.panel.infrastructure.xui.dto.client.XuiDeleteClientRemoteResponse;
import com.parazit.panel.infrastructure.xui.dto.client.XuiCreateClientRemoteResponse;
import com.parazit.panel.infrastructure.xui.dto.client.XuiUpdateClientRemoteResponse;
import com.parazit.panel.infrastructure.xui.exception.XuiConnectionException;
import com.parazit.panel.infrastructure.xui.exception.XuiInvalidResponseException;
import com.parazit.panel.infrastructure.xui.exception.XuiTimeoutException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class RestClientXuiClientManagementClient implements XuiClientManagementClient {

    private final AuthenticatedRequestExecutor requestExecutor;
    private final XuiProperties properties;
    private final XuiClientLifecycleProperties lifecycleProperties;
    private final XuiInboundClient inboundClient;
    private final XuiCreateClientPayloadBuilder createPayloadBuilder;
    private final XuiDisableClientPayloadBuilder disablePayloadBuilder;

    public RestClientXuiClientManagementClient(
            AuthenticatedRequestExecutor requestExecutor,
            XuiProperties properties,
            XuiClientLifecycleProperties lifecycleProperties,
            XuiInboundClient inboundClient,
            XuiCreateClientPayloadBuilder createPayloadBuilder,
            XuiDisableClientPayloadBuilder disablePayloadBuilder
    ) {
        this.requestExecutor = Objects.requireNonNull(requestExecutor, "requestExecutor must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.lifecycleProperties = Objects.requireNonNull(lifecycleProperties, "lifecycleProperties must not be null");
        this.inboundClient = Objects.requireNonNull(inboundClient, "inboundClient must not be null");
        this.createPayloadBuilder = Objects.requireNonNull(createPayloadBuilder, "createPayloadBuilder must not be null");
        this.disablePayloadBuilder = Objects.requireNonNull(disablePayloadBuilder, "disablePayloadBuilder must not be null");
    }

    @Override
    public CreateXuiClientResponse createClient(CreateXuiClientRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        try {
            XuiCreateClientRemoteResponse response = requestExecutor.postEntityOnce(
                    properties.clientCreatePath(),
                    createPayloadBuilder.build(request),
                    XuiCreateClientRemoteResponse.class
            ).getBody();
            if (response == null) {
                throw new XuiInvalidResponseException("Xui create-client response is empty");
            }
            if (!Boolean.TRUE.equals(response.success())) {
                throw new XuiInvalidResponseException(safeMessage(response.msg()));
            }
            return new CreateXuiClientResponse(
                    request.inboundId(),
                    request.clientId(),
                    request.email(),
                    request.subscriptionId(),
                    true,
                    safeMessage(response.msg())
            );
        } catch (XuiTimeoutException exception) {
            throw new XuiClientCreateTimeoutException("Xui create-client request timed out", exception);
        } catch (XuiConnectionException exception) {
            throw new XuiClientCreateConnectionException("Xui create-client request could not connect", exception);
        } catch (XuiInvalidResponseException exception) {
            throw new XuiClientCreateRejectedException(exception.getMessage(), exception);
        }
    }

    @Override
    public DisableXuiClientResponse disableClient(DisableXuiClientRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        XuiClientSnapshot remoteClient = inboundClient.findClient(request.inboundId(), request.clientId(), request.email())
                .orElseThrow(() -> new XuiClientRemoteOperationRejectedException("Remote Xui client was not found", null));
        verifyIdentity(request.clientId(), request.email(), remoteClient);
        if (!remoteClient.enabled()) {
            return new DisableXuiClientResponse(request.inboundId(), request.clientId(), true, true, "Remote client is already disabled");
        }
        try {
            XuiUpdateClientRemoteResponse response = requestExecutor.postEntityOnce(
                    expand(lifecycleProperties.clientUpdatePathTemplate(), request.inboundId(), request.clientId()),
                    disablePayloadBuilder.build(request, remoteClient),
                    XuiUpdateClientRemoteResponse.class
            ).getBody();
            if (response == null) {
                throw new XuiInvalidResponseException("Xui disable-client response is empty");
            }
            if (!Boolean.TRUE.equals(response.success())) {
                throw new XuiInvalidResponseException(safeMessage(response.msg()));
            }
            return new DisableXuiClientResponse(
                    request.inboundId(),
                    request.clientId(),
                    true,
                    false,
                    safeMessage(response.msg())
            );
        } catch (XuiTimeoutException exception) {
            throw new XuiClientRemoteOperationTimeoutException("Xui disable-client request timed out", exception);
        } catch (XuiConnectionException exception) {
            throw new XuiClientRemoteOperationConnectionException("Xui disable-client request could not connect", exception);
        } catch (XuiInvalidResponseException exception) {
            throw new XuiClientRemoteOperationRejectedException(exception.getMessage(), exception);
        }
    }

    @Override
    public DeleteXuiClientResponse deleteClient(DeleteXuiClientRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        inboundClient.findClient(request.inboundId(), request.clientId(), request.email())
                .ifPresent(remote -> verifyIdentity(request.clientId(), request.email(), remote));
        try {
            XuiDeleteClientRemoteResponse response = requestExecutor.postEntityOnce(
                    expand(lifecycleProperties.clientDeletePathTemplate(), request.inboundId(), request.clientId()),
                    java.util.Map.of(),
                    XuiDeleteClientRemoteResponse.class
            ).getBody();
            if (response == null) {
                throw new XuiInvalidResponseException("Xui delete-client response is empty");
            }
            if (!Boolean.TRUE.equals(response.success())) {
                String message = safeMessage(response.msg());
                if (message.toLowerCase().contains("not found")) {
                    return new DeleteXuiClientResponse(request.inboundId(), request.clientId(), true, true, message);
                }
                throw new XuiInvalidResponseException(message);
            }
            return new DeleteXuiClientResponse(
                    request.inboundId(),
                    request.clientId(),
                    true,
                    false,
                    safeMessage(response.msg())
            );
        } catch (XuiTimeoutException exception) {
            throw new XuiClientRemoteOperationTimeoutException("Xui delete-client request timed out", exception);
        } catch (XuiConnectionException exception) {
            throw new XuiClientRemoteOperationConnectionException("Xui delete-client request could not connect", exception);
        } catch (XuiInvalidResponseException exception) {
            throw new XuiClientRemoteOperationRejectedException(exception.getMessage(), exception);
        }
    }

    private static void verifyIdentity(String expectedClientId, String expectedEmail, XuiClientSnapshot remoteClient) {
        if (remoteClient.clientId() == null || !remoteClient.clientId().equalsIgnoreCase(expectedClientId)) {
            throw new XuiRemoteClientIdentityMismatchException();
        }
        if (remoteClient.email() != null
                && !remoteClient.email().isBlank()
                && !remoteClient.email().equalsIgnoreCase(expectedEmail)) {
            throw new XuiRemoteClientIdentityMismatchException();
        }
    }

    private static String expand(String template, long inboundId, String clientId) {
        return template
                .replace("{inboundId}", Long.toString(inboundId))
                .replace("{clientId}", encode(clientId));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String safeMessage(String message) {
        if (message == null || message.isBlank()) {
            return "Xui create-client request completed";
        }
        String trimmed = message.trim();
        return trimmed.length() > 200 ? trimmed.substring(0, 200) : trimmed;
    }
}
