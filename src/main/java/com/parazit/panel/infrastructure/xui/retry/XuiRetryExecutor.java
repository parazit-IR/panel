package com.parazit.panel.infrastructure.xui.retry;

import com.parazit.panel.infrastructure.xui.exception.XuiConnectionException;
import com.parazit.panel.infrastructure.xui.exception.XuiException;
import com.parazit.panel.infrastructure.xui.exception.XuiServerException;
import com.parazit.panel.infrastructure.xui.exception.XuiTimeoutException;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XuiRetryExecutor {

    private static final Logger log = LoggerFactory.getLogger(XuiRetryExecutor.class);

    private final int maxRetries;
    private final Duration retryDelay;
    private final Sleeper sleeper;

    public XuiRetryExecutor(int maxRetries, Duration retryDelay) {
        this(maxRetries, retryDelay, ThreadSleeper.INSTANCE);
    }

    XuiRetryExecutor(int maxRetries, Duration retryDelay, Sleeper sleeper) {
        if (maxRetries < 0) {
            throw new IllegalArgumentException("maxRetries must be zero or positive");
        }
        if (retryDelay == null || retryDelay.isNegative()) {
            throw new IllegalArgumentException("retryDelay must be zero or positive");
        }
        this.maxRetries = maxRetries;
        this.retryDelay = retryDelay;
        this.sleeper = Objects.requireNonNull(sleeper, "sleeper must not be null");
    }

    public <T> T execute(Callable<T> operation) {
        Objects.requireNonNull(operation, "operation must not be null");
        int attempt = 0;
        while (true) {
            try {
                return operation.call();
            } catch (XuiException exception) {
                if (!shouldRetry(exception) || attempt >= maxRetries) {
                    throw exception;
                }
                attempt++;
                log.warn(
                        "Retrying Xui request attempt={} maxRetries={} delay={} reason={}",
                        attempt,
                        maxRetries,
                        retryDelay,
                        exception.getMessage()
                );
                sleep();
            } catch (Exception exception) {
                throw new XuiException("Xui request failed", exception);
            }
        }
    }

    private boolean shouldRetry(XuiException exception) {
        if (exception instanceof XuiTimeoutException || exception instanceof XuiConnectionException) {
            return true;
        }
        if (exception instanceof XuiServerException serverException) {
            int statusCode = serverException.statusCode();
            return statusCode == 502 || statusCode == 503 || statusCode == 504;
        }
        return false;
    }

    private void sleep() {
        try {
            sleeper.sleep(retryDelay);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new XuiConnectionException("Interrupted while waiting to retry Xui request", exception);
        }
    }

    @FunctionalInterface
    interface Sleeper {

        void sleep(Duration duration) throws InterruptedException;
    }

    private enum ThreadSleeper implements Sleeper {
        INSTANCE;

        @Override
        public void sleep(Duration duration) throws InterruptedException {
            if (!duration.isZero()) {
                Thread.sleep(duration.toMillis());
            }
        }
    }
}
