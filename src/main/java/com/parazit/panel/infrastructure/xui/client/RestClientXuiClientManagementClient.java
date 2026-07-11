package com.parazit.panel.infrastructure.xui.client;

import com.parazit.panel.application.port.out.xui.XuiClientManagementClient;
import com.parazit.panel.application.xui.client.XuiClientCreateConnectionException;
import com.parazit.panel.application.xui.client.XuiClientCreateRejectedException;
import com.parazit.panel.application.xui.client.XuiClientCreateTimeoutException;
import com.parazit.panel.application.xui.client.model.CreateXuiClientRequest;
import com.parazit.panel.application.xui.client.model.CreateXuiClientResponse;
import com.parazit.panel.infrastructure.xui.authentication.AuthenticatedRequestExecutor;
import com.parazit.panel.infrastructure.xui.config.XuiProperties;
import com.parazit.panel.infrastructure.xui.dto.client.XuiCreateClientRemoteResponse;
import com.parazit.panel.infrastructure.xui.exception.XuiConnectionException;
import com.parazit.panel.infrastructure.xui.exception.XuiInvalidResponseException;
import com.parazit.panel.infrastructure.xui.exception.XuiTimeoutException;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class RestClientXuiClientManagementClient implements XuiClientManagementClient {

    private final AuthenticatedRequestExecutor requestExecutor;
    private final XuiProperties properties;
    private final XuiCreateClientPayloadBuilder payloadBuilder;

    public RestClientXuiClientManagementClient(
            AuthenticatedRequestExecutor requestExecutor,
            XuiProperties properties,
            XuiCreateClientPayloadBuilder payloadBuilder
    ) {
        this.requestExecutor = Objects.requireNonNull(requestExecutor, "requestExecutor must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.payloadBuilder = Objects.requireNonNull(payloadBuilder, "payloadBuilder must not be null");
    }

    @Override
    public CreateXuiClientResponse createClient(CreateXuiClientRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        try {
            XuiCreateClientRemoteResponse response = requestExecutor.postEntityOnce(
                    properties.clientCreatePath(),
                    payloadBuilder.build(request),
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

    private static String safeMessage(String message) {
        if (message == null || message.isBlank()) {
            return "Xui create-client request completed";
        }
        String trimmed = message.trim();
        return trimmed.length() > 200 ? trimmed.substring(0, 200) : trimmed;
    }
}
