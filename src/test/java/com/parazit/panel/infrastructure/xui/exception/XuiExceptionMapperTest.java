package com.parazit.panel.infrastructure.xui.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.ResourceAccessException;

class XuiExceptionMapperTest {

    private final XuiExceptionMapper mapper = new XuiExceptionMapper();

    @Test
    void mapsAuthenticationStatuses() {
        assertThat(mapper.mapStatus(HttpStatus.UNAUTHORIZED))
                .isInstanceOf(XuiAuthenticationException.class)
                .hasMessage("Xui authentication failed");
        assertThat(mapper.mapStatus(HttpStatus.FORBIDDEN))
                .isInstanceOf(XuiAuthenticationException.class);
    }

    @Test
    void mapsClientAndServerStatuses() {
        assertThat(mapper.mapStatus(HttpStatus.NOT_FOUND))
                .isInstanceOf(XuiClientException.class)
                .extracting("statusCode")
                .isEqualTo(404);
        assertThat(mapper.mapStatus(HttpStatus.BAD_GATEWAY))
                .isInstanceOf(XuiServerException.class)
                .extracting("statusCode")
                .isEqualTo(502);
    }

    @Test
    void mapsTimeoutAndConnectionFailuresWithoutHttpInternalsInMessage() {
        XuiException timeout = mapper.map(new ResourceAccessException(
                "I/O error",
                new SocketTimeoutException("read timed out")
        ));
        XuiException connection = mapper.map(new ResourceAccessException(
                "I/O error",
                new ConnectException("connection refused")
        ));

        assertThat(timeout)
                .isInstanceOf(XuiTimeoutException.class)
                .hasMessage("Xui request timed out");
        assertThat(connection)
                .isInstanceOf(XuiConnectionException.class)
                .hasMessage("Xui server is unreachable");
    }
}
