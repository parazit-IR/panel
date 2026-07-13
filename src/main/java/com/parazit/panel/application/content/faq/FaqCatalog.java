package com.parazit.panel.application.content.faq;

import com.parazit.panel.config.properties.FaqProperties;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class FaqCatalog {

    private final FaqProperties properties;

    public FaqCatalog(FaqProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    public boolean enabled() {
        return properties.enabled();
    }

    public int pageSize() {
        return properties.pageSize();
    }

    public List<FaqItem> enabledItems() {
        return enabled() ? properties.enabledItems() : List.of();
    }

    public Optional<FaqItem> findEnabled(String id) {
        if (id == null || id.isBlank() || !enabled()) {
            return Optional.empty();
        }
        return enabledItems().stream()
                .filter(item -> item.id().equals(id))
                .findFirst();
    }

    public List<FaqItem> page(int page) {
        List<FaqItem> items = enabledItems();
        int safePage = Math.max(1, page);
        int from = Math.min((safePage - 1) * pageSize(), items.size());
        int to = Math.min(from + pageSize(), items.size());
        return items.subList(from, to);
    }

    public int totalPages() {
        int count = enabledItems().size();
        return Math.max(1, (int) Math.ceil(count / (double) pageSize()));
    }
}
