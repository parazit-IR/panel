package com.parazit.panel.application.customer;

import com.parazit.panel.application.customer.result.CustomerServiceSummaryResult;
import com.parazit.panel.application.port.in.customer.SearchCustomerServicesUseCase;
import com.parazit.panel.application.port.out.customer.CustomerServiceQueryPort;
import com.parazit.panel.application.telegram.menu.TelegramButtonTextNormalizer;
import com.parazit.panel.config.properties.CustomerServicesTelegramProperties;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class CustomerServiceSearchService implements SearchCustomerServicesUseCase {

    private final CustomerServiceQueryPort queryPort;
    private final CustomerServicesTelegramProperties properties;
    private final TelegramButtonTextNormalizer normalizer;

    public CustomerServiceSearchService(
            CustomerServiceQueryPort queryPort,
            CustomerServicesTelegramProperties properties,
            TelegramButtonTextNormalizer normalizer
    ) {
        this.queryPort = Objects.requireNonNull(queryPort, "queryPort must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.normalizer = Objects.requireNonNull(normalizer, "normalizer must not be null");
    }

    @Override
    public List<CustomerServiceSummaryResult> search(long telegramUserId, String query, int limit) {
        String normalized = normalizeQuery(query);
        if (normalized.length() < properties.searchMinimumLength()) {
            throw new IllegalArgumentException("service search query is too short");
        }
        if (normalized.length() > properties.searchMaximumLength()) {
            throw new IllegalArgumentException("service search query is too long");
        }
        int boundedLimit = Math.max(1, Math.min(limit <= 0 ? properties.searchResultLimit() : limit, properties.searchResultLimit()));
        return queryPort.findAllByTelegramUserId(telegramUserId)
                .stream()
                .map(service -> new ScoredService(service, score(normalized, service)))
                .filter(scored -> scored.score() < 100)
                .sorted(Comparator.comparingInt(ScoredService::score)
                        .thenComparing(scored -> scored.service().displayName()))
                .limit(boundedLimit)
                .map(ScoredService::service)
                .toList();
    }

    public String normalizeQuery(String query) {
        return normalizer.normalize(query == null ? "" : query).toLowerCase(Locale.ROOT);
    }

    private int score(String query, CustomerServiceSummaryResult service) {
        String username = normalizeQuery(service.serviceUsername());
        String display = normalizeQuery(service.displayName());
        String plan = normalizeQuery(service.planName());
        if (username.equals(query) || display.equals(query)) {
            return 0;
        }
        if (username.startsWith(query) || display.startsWith(query)) {
            return 1;
        }
        if (username.contains(query) || display.contains(query) || plan.contains(query)) {
            return 2;
        }
        return 100;
    }

    private record ScoredService(CustomerServiceSummaryResult service, int score) {
    }
}
