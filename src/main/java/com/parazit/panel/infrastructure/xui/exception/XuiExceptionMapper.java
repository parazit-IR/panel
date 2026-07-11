package com.parazit.panel.infrastructure.xui.exception;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

public class XuiExceptionMapper {

    public XuiException map(RuntimeException exception) {
        if (exception instanceof XuiException xuiException) {
            return xuiException;
        }
        if (exception instanceof RestClientResponseException responseException) {
            return mapStatus(responseException.getStatusCode());
        }
        if (exception instanceof ResourceAccessException) {
            Throwable rootCause = rootCause(exception);
            if (isTimeout(rootCause)) {
                return new XuiTimeoutException("Xui request timed out", exception);
            }
            if (isConnectionFailure(rootCause)) {
                return new XuiConnectionException("Xui server is unreachable", exception);
            }
            return new XuiConnectionException("Xui request failed before a response was received", exception);
        }
        Throwable rootCause = rootCause(exception);
        if (isTimeout(rootCause)) {
            return new XuiTimeoutException("Xui request timed out", exception);
        }
        if (isConnectionFailure(rootCause)) {
            return new XuiConnectionException("Xui server is unreachable", exception);
        }
        return new XuiException("Xui request failed", exception);
    }

    public XuiException mapStatus(HttpStatusCode statusCode) {
        int value = statusCode.value();
        if (value == 401 || value == 403) {
            return new XuiAuthenticationException("Xui authentication failed");
        }
        if (value >= 400 && value < 500) {
            return new XuiClientException(value);
        }
        if (value >= 500) {
            return new XuiServerException(value);
        }
        return new XuiException("Unexpected Xui response status " + value);
    }

    private static Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private static boolean isTimeout(Throwable throwable) {
        return throwable instanceof SocketTimeoutException
                || throwable.getClass().getSimpleName().contains("Timeout");
    }

    private static boolean isConnectionFailure(Throwable throwable) {
        return throwable instanceof ConnectException
                || throwable instanceof NoRouteToHostException
                || throwable instanceof UnknownHostException;
    }
}
