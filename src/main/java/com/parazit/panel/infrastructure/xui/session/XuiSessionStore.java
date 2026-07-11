package com.parazit.panel.infrastructure.xui.session;

import java.net.HttpCookie;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class XuiSessionStore {

    private final ConcurrentHashMap<String, String> cookies = new ConcurrentHashMap<>();

    public void store(String name, String value) {
        if (name == null || name.isBlank() || value == null) {
            return;
        }
        cookies.put(name, value);
    }

    public void storeFromSetCookieHeaders(List<String> setCookieHeaders) {
        if (setCookieHeaders == null) {
            return;
        }
        setCookieHeaders.forEach(this::storeFromSetCookieHeader);
    }

    public Optional<String> cookieHeader() {
        if (cookies.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(cookies.entrySet()
                .stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("; ")));
    }

    public boolean hasSession() {
        return !cookies.isEmpty();
    }

    public Map<String, String> snapshot() {
        return Map.copyOf(cookies);
    }

    public void clear() {
        cookies.clear();
    }

    private void storeFromSetCookieHeader(String header) {
        if (header == null || header.isBlank()) {
            return;
        }
        HttpCookie.parse(header)
                .forEach(cookie -> store(cookie.getName(), cookie.getValue()));
    }
}
