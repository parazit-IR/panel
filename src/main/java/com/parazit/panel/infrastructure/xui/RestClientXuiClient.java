package com.parazit.panel.infrastructure.xui;

import com.parazit.panel.application.port.out.xui.XuiClient;
import org.springframework.stereotype.Component;

@Component
public class RestClientXuiClient implements XuiClient {

    private final XuiRequestExecutor requestExecutor;

    public RestClientXuiClient(XuiRequestExecutor requestExecutor) {
        this.requestExecutor = requestExecutor;
    }

    @Override
    public void login() {
        throw new UnsupportedOperationException("Xui login is not implemented yet");
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
}
