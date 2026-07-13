package com.parazit.panel.application.customer;

import com.parazit.panel.application.customer.result.CustomerServicePageResult;
import com.parazit.panel.application.customer.result.CustomerServiceSort;
import com.parazit.panel.application.customer.result.CustomerServiceStatus;
import com.parazit.panel.application.customer.result.CustomerServiceStatusFilter;
import com.parazit.panel.application.customer.result.CustomerServiceSummaryResult;
import com.parazit.panel.application.port.in.customer.ListCustomerServicesUseCase;
import com.parazit.panel.application.port.out.customer.CustomerServiceQueryPort;
import com.parazit.panel.config.properties.CustomerServicesTelegramProperties;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class CustomerServicesQueryService implements ListCustomerServicesUseCase {

    private final CustomerServiceQueryPort queryPort;
    private final CustomerServicesTelegramProperties properties;

    public CustomerServicesQueryService(CustomerServiceQueryPort queryPort, CustomerServicesTelegramProperties properties) {
        this.queryPort = Objects.requireNonNull(queryPort, "queryPort must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    @Override
    public CustomerServicePageResult list(
            long telegramUserId,
            int page,
            int size,
            CustomerServiceSort sort,
            CustomerServiceStatusFilter statusFilter
    ) {
        int boundedPage = Math.max(page, 0);
        int boundedSize = Math.max(1, Math.min(size <= 0 ? properties.pageSize() : size, properties.maxPageSize()));
        List<CustomerServiceSummaryResult> filtered = queryPort.findAllByTelegramUserId(telegramUserId)
                .stream()
                .filter(service -> allowedByStatusPolicy(service.status()))
                .filter(service -> filterMatches(statusFilter, service.status()))
                .sorted(comparator())
                .toList();
        int totalPages = filtered.isEmpty() ? 0 : (int) Math.ceil((double) filtered.size() / (double) boundedSize);
        int from = Math.min(boundedPage * boundedSize, filtered.size());
        int to = Math.min(from + boundedSize, filtered.size());
        List<CustomerServiceSummaryResult> items = filtered.subList(from, to);
        return new CustomerServicePageResult(
                items,
                boundedPage,
                boundedSize,
                filtered.size(),
                totalPages,
                totalPages > 0 && boundedPage + 1 < totalPages,
                boundedPage > 0 && totalPages > 0
        );
    }

    private boolean allowedByStatusPolicy(CustomerServiceStatus status) {
        return switch (status) {
            case EXPIRED -> properties.showExpiredServices();
            case SUSPENDED -> properties.showSuspendedServices();
            case REVOKED -> properties.showRevokedServices();
            default -> true;
        };
    }

    private static boolean filterMatches(CustomerServiceStatusFilter filter, CustomerServiceStatus status) {
        CustomerServiceStatusFilter effective = filter == null ? CustomerServiceStatusFilter.ALL : filter;
        return switch (effective) {
            case ALL -> true;
            case ACTIVE -> status == CustomerServiceStatus.ACTIVE;
            case EXPIRED -> status == CustomerServiceStatus.EXPIRED;
            case SUSPENDED -> status == CustomerServiceStatus.SUSPENDED;
            case REVOKED -> status == CustomerServiceStatus.REVOKED;
            case PROVISIONING -> status == CustomerServiceStatus.PROVISIONING;
        };
    }

    private static Comparator<CustomerServiceSummaryResult> comparator() {
        return Comparator.comparingInt((CustomerServiceSummaryResult service) -> statusRank(service.status()))
                .thenComparing(service -> service.expiresAt().orElse(java.time.Instant.EPOCH), Comparator.reverseOrder())
                .thenComparing(CustomerServiceSummaryResult::subscriptionId);
    }

    private static int statusRank(CustomerServiceStatus status) {
        return switch (status) {
            case ACTIVE -> 0;
            case PROVISIONING -> 1;
            case SUSPENDED -> 2;
            case EXPIRED -> 3;
            case FAILED -> 4;
            case UNKNOWN -> 5;
            case REVOKED -> 6;
        };
    }
}
