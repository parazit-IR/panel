package com.parazit.panel.application.port.out.xui;

public interface XuiClient {

    void login();

    void logout();

    boolean isLoggedIn();

    void refreshSession();

    String getInbounds();

    void createClient();

    void updateClient();

    void deleteClient();

    boolean ping();

    boolean isAuthenticated();

    boolean pingAuthenticated();
}
