package com.parazit.panel.application.port.out.xui;

public interface XuiClient {

    void login();

    String getInbounds();

    void createClient();

    void updateClient();

    void deleteClient();

    boolean ping();
}
