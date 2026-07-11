package com.parazit.panel.infrastructure.xui.retry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parazit.panel.infrastructure.xui.exception.XuiClientException;
import com.parazit.panel.infrastructure.xui.exception.XuiConnectionException;
import com.parazit.panel.infrastructure.xui.exception.XuiServerException;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class XuiRetryExecutorTest {

    @Test
    void retriesTransientConnectionFailures() {
        AtomicInteger attempts = new AtomicInteger();
        XuiRetryExecutor executor = new XuiRetryExecutor(2, Duration.ZERO, duration -> {
        });

        String result = executor.execute(() -> {
            if (attempts.incrementAndGet() < 3) {
                throw new XuiConnectionException("temporary connection failure", new IOException("refused"));
            }
            return "ok";
        });

        assertThat(result).isEqualTo("ok");
        assertThat(attempts).hasValue(3);
    }

    @Test
    void retriesOnlyGatewayAndUnavailableServerStatuses() {
        AtomicInteger attempts = new AtomicInteger();
        XuiRetryExecutor executor = new XuiRetryExecutor(1, Duration.ZERO, duration -> {
        });

        String result = executor.execute(() -> {
            if (attempts.incrementAndGet() == 1) {
                throw new XuiServerException(503);
            }
            return "recovered";
        });

        assertThat(result).isEqualTo("recovered");
        assertThat(attempts).hasValue(2);
    }

    @Test
    void doesNotRetryValidationOrClientFailures() {
        AtomicInteger attempts = new AtomicInteger();
        XuiRetryExecutor executor = new XuiRetryExecutor(3, Duration.ZERO, duration -> {
        });

        assertThatThrownBy(() -> executor.execute(() -> {
            attempts.incrementAndGet();
            throw new XuiClientException(400);
        })).isInstanceOf(XuiClientException.class);

        assertThat(attempts).hasValue(1);
    }

    @Test
    void stopsAfterConfiguredRetries() {
        AtomicInteger attempts = new AtomicInteger();
        XuiRetryExecutor executor = new XuiRetryExecutor(1, Duration.ZERO, duration -> {
        });

        assertThatThrownBy(() -> executor.execute(() -> {
            attempts.incrementAndGet();
            throw new XuiServerException(504);
        })).isInstanceOf(XuiServerException.class);

        assertThat(attempts).hasValue(2);
    }
}
