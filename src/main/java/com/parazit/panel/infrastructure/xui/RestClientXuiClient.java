package com.parazit.panel.infrastructure.xui;

import com.parazit.panel.application.port.out.xui.XuiClient;
import com.parazit.panel.infrastructure.xui.authentication.AuthenticatedRequestExecutor;
import com.parazit.panel.infrastructure.xui.authentication.XuiAuthenticationManager;
import org.springframework.stereotype.Component;

@Component
public class RestClientXuiClient implements XuiClient {

    private final XuiRequestExecutor requestExecutor;
    private final XuiAuthenticationManager authenticationManager;
    private final AuthenticatedRequestExecutor authenticatedRequestExecutor;

    public RestClientXuiClient(
            XuiRequestExecutor requestExecutor,
            XuiAuthenticationManager authenticationManager,
            AuthenticatedRequestExecutor authenticatedRequestExecutor
    ) {
        this.requestExecutor = requestExecutor;
        this.authenticationManager = authenticationManager;
        this.authenticatedRequestExecutor = authenticatedRequestExecutor;
    }

    @Override
    public void login() {
        authenticationManager.login();
    }

    @Override
    public void logout() {
        authenticationManager.logout();
    }

    @Override
    public boolean isLoggedIn() {
        return authenticationManager.isAuthenticated();
    }

    @Override
    public void refreshSession() {
        authenticationManager.refreshSession();
    }

    @Override
    public String getInbounds() {
        throw new UnsupportedOperationException("Xui inbound retrieval is not implemented yet");
    }

    @Override
    public void createClient() {
        throw new UnsupportedOperationException("Xui client creation is not implemented yet");
    }

    @Override
    public void updateClient() {
        throw new UnsupportedOperationException("Xui client update is not implemented yet");
    }

    @Override
    public void deleteClient() {
        throw new UnsupportedOperationException("Xui client deletion is not implemented yet");
    }

    @Override
    public boolean ping() {
        requestExecutor.get("/", String.class);
        return true;
    }

    @Override
    public boolean isAuthenticated() {
        return authenticationManager.isAuthenticated();
    }

    @Override
    public boolean pingAuthenticated() {
        authenticatedRequestExecutor.get("/", String.class);
        return true;
    }
}
