package com.parazit.panel.infrastructure.xui;

import java.io.IOException;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

public class XuiLoggingInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(XuiLoggingInterceptor.class);

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution
    ) throws IOException {
        long startedAt = System.nanoTime();
        try {
            ClientHttpResponse response = execution.execute(request, body);
            log.info(
                    "Xui request method={} url={} status={} durationMs={}",
                    request.getMethod(),
                    safeUrl(request.getURI()),
                    response.getStatusCode().value(),
                    elapsedMillis(startedAt)
            );
            return response;
        } catch (IOException exception) {
            log.info(
                    "Xui request failed method={} url={} durationMs={}",
                    request.getMethod(),
                    safeUrl(request.getURI()),
                    elapsedMillis(startedAt)
            );
            throw exception;
        }
    }

    private static long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }

    private static String safeUrl(URI uri) {
        StringBuilder builder = new StringBuilder();
        builder.append(uri.getScheme()).append("://").append(uri.getHost());
        if (uri.getPort() > -1) {
            builder.append(':').append(uri.getPort());
        }
        builder.append(uri.getPath());
        return builder.toString();
    }
}
