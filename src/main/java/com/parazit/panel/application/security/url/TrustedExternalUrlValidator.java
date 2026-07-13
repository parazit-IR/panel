package com.parazit.panel.application.security.url;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class TrustedExternalUrlValidator {

    private static final int MAX_URL_LENGTH = 500;

    public URI validate(URI uri, Set<String> allowedHosts) {
        Objects.requireNonNull(uri, "uri must not be null");
        String value = uri.toString();
        if (value.length() > MAX_URL_LENGTH || containsControl(value)) {
            throw new IllegalArgumentException("external URL is invalid");
        }
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("external URL must use HTTPS");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("external URL host is required");
        }
        if (uri.getUserInfo() != null) {
            throw new IllegalArgumentException("external URL credentials are not allowed");
        }
        String normalizedHost = stripIpv6Brackets(host.toLowerCase(Locale.ROOT));
        if (isBlockedHost(normalizedHost)) {
            throw new IllegalArgumentException("external URL host is not trusted");
        }
        if (allowedHosts != null && !allowedHosts.isEmpty() && allowedHosts.stream().noneMatch(allowed -> hostMatches(normalizedHost, allowed))) {
            throw new IllegalArgumentException("external URL host is not allowlisted");
        }
        return normalize(uri);
    }

    public URI validate(String value, Set<String> allowedHosts) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("external URL must not be blank");
        }
        return validate(URI.create(value.trim()), allowedHosts);
    }

    private static boolean hostMatches(String host, String allowedHost) {
        if (allowedHost == null || allowedHost.isBlank()) {
            return false;
        }
        String allowed = allowedHost.trim().toLowerCase(Locale.ROOT);
        return host.equals(allowed) || host.endsWith("." + allowed);
    }

    private static boolean containsControl(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isISOControl(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBlockedHost(String host) {
        if ("localhost".equals(host) || host.endsWith(".localhost") || host.endsWith(".local")) {
            return true;
        }
        try {
            InetAddress address = InetAddress.getByName(host);
            if (host.chars().anyMatch(ch -> ch == ':' || Character.isDigit(ch)) && host.matches("[0-9a-fA-F:.]+")) {
                return address.isAnyLocalAddress()
                        || address.isLoopbackAddress()
                        || address.isLinkLocalAddress()
                        || address.isSiteLocalAddress()
                        || address.isMulticastAddress();
            }
        } catch (Exception ignored) {
            return false;
        }
        return false;
    }

    private static String stripIpv6Brackets(String host) {
        if (host != null && host.length() > 2 && host.startsWith("[") && host.endsWith("]")) {
            return host.substring(1, host.length() - 1);
        }
        return host;
    }

    private static URI normalize(URI uri) {
        try {
            return new URI(
                    uri.getScheme().toLowerCase(Locale.ROOT),
                    null,
                    uri.getHost().toLowerCase(Locale.ROOT),
                    uri.getPort(),
                    uri.getPath(),
                    uri.getQuery(),
                    null
            );
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("external URL is invalid", exception);
        }
    }
}
