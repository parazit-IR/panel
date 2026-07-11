package com.parazit.panel.test.xui;

import java.io.IOException;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public abstract class XuiMockServerSupport {

    protected MockWebServer server;

    @BeforeEach
    void startMockServer() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void stopMockServer() throws IOException {
        server.shutdown();
    }

    protected String baseUrl() {
        return server.url("/").toString();
    }
}
