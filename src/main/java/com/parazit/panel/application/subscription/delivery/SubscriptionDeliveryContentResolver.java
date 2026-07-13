package com.parazit.panel.application.subscription.delivery;

import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.application.port.out.security.SubscriptionTokenHasher;
import com.parazit.panel.application.port.out.xui.XuiInboundClient;
import com.parazit.panel.application.subscription.SubscriptionLifecycleTransaction;
import com.parazit.panel.application.subscription.SubscriptionNotAccessibleException;
import com.parazit.panel.application.subscription.SubscriptionNotFoundException;
import com.parazit.panel.application.subscription.SubscriptionRenderingException;
import com.parazit.panel.application.subscription.UnsupportedInboundConfigurationException;
import com.parazit.panel.application.subscription.render.VlessUriBuilder;
import com.parazit.panel.application.xui.model.XuiClientSnapshot;
import com.parazit.panel.application.xui.model.XuiInboundSnapshot;
import com.parazit.panel.config.properties.SubscriptionEndpointProperties;
import com.parazit.panel.config.properties.SubscriptionProperties;
import com.parazit.panel.domain.order.Order;
import com.parazit.panel.domain.order.repository.OrderRepository;
import com.parazit.panel.domain.plan.selection.PlanSelection;
import com.parazit.panel.domain.plan.selection.repository.PlanSelectionRepository;
import com.parazit.panel.domain.subscription.Subscription;
import com.parazit.panel.domain.subscription.SubscriptionAccessToken;
import com.parazit.panel.domain.subscription.SubscriptionStatus;
import com.parazit.panel.domain.subscription.repository.SubscriptionRepository;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.repository.UserRepository;
import com.parazit.panel.domain.xui.provisioning.XuiClientProvision;
import com.parazit.panel.domain.xui.provisioning.XuiProvisionStatus;
import com.parazit.panel.domain.xui.provisioning.repository.XuiClientProvisionRepository;
import com.parazit.panel.application.subscription.model.VlessSubscriptionConfig;
import com.parazit.panel.application.user.UserNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class SubscriptionDeliveryContentResolver {

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final OrderRepository orderRepository;
    private final PlanSelectionRepository planSelectionRepository;
    private final XuiClientProvisionRepository provisionRepository;
    private final XuiInboundClient inboundClient;
    private final VlessUriBuilder vlessUriBuilder;
    private final SubscriptionTokenHasher tokenHasher;
    private final SubscriptionLifecycleTransaction lifecycleTransaction;
    private final SystemClockPort clock;
    private final SubscriptionProperties subscriptionProperties;
    private final SubscriptionEndpointProperties endpointProperties;

    SubscriptionDeliveryContentResolver(
            UserRepository userRepository,
            SubscriptionRepository subscriptionRepository,
            OrderRepository orderRepository,
            PlanSelectionRepository planSelectionRepository,
            XuiClientProvisionRepository provisionRepository,
            XuiInboundClient inboundClient,
            VlessUriBuilder vlessUriBuilder,
            SubscriptionTokenHasher tokenHasher,
            SubscriptionLifecycleTransaction lifecycleTransaction,
            SystemClockPort clock,
            SubscriptionProperties subscriptionProperties,
            SubscriptionEndpointProperties endpointProperties
    ) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.subscriptionRepository = Objects.requireNonNull(subscriptionRepository, "subscriptionRepository must not be null");
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository must not be null");
        this.planSelectionRepository = Objects.requireNonNull(planSelectionRepository, "planSelectionRepository must not be null");
        this.provisionRepository = Objects.requireNonNull(provisionRepository, "provisionRepository must not be null");
        this.inboundClient = Objects.requireNonNull(inboundClient, "inboundClient must not be null");
        this.vlessUriBuilder = Objects.requireNonNull(vlessUriBuilder, "vlessUriBuilder must not be null");
        this.tokenHasher = Objects.requireNonNull(tokenHasher, "tokenHasher must not be null");
        this.lifecycleTransaction = Objects.requireNonNull(lifecycleTransaction, "lifecycleTransaction must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.subscriptionProperties = Objects.requireNonNull(subscriptionProperties, "subscriptionProperties must not be null");
        this.endpointProperties = Objects.requireNonNull(endpointProperties, "endpointProperties must not be null");
    }

    SubscriptionDeliveryContent resolveContent(Long telegramUserId, UUID subscriptionId) {
        User user = userRepository.findByTelegramUserId(telegramUserId)
                .orElseThrow(() -> new UserNotFoundException(telegramUserId));
        Subscription subscription = subscriptionRepository.findByUserIdAndId(user.getId(), subscriptionId)
                .orElseThrow(() -> new SubscriptionNotFoundException(subscriptionId));
        XuiClientProvision provision = validateSubscriptionAndProvision(subscription);
        PlanSelection selection = planSelectionRepository.findById(subscription.getPlanSelectionId())
                .orElseThrow(SubscriptionNotAccessibleException::new);
        Order order = orderRepository.findById(subscription.getOrderId())
                .orElseThrow(SubscriptionNotAccessibleException::new);
        validateRelationships(subscription, provision, user, selection, order);
        XuiInboundSnapshot inbound = inboundClient.getInboundById(subscription.getInboundId())
                .orElseThrow(() -> new SubscriptionRenderingException("Trusted inbound metadata is unavailable"));
        XuiClientSnapshot client = inbound.clients()
                .stream()
                .filter(candidate -> matchesClient(provision, candidate))
                .findFirst()
                .orElseThrow(() -> new SubscriptionRenderingException("Trusted client metadata is unavailable"));
        if (!client.enabled()) {
            throw new SubscriptionNotAccessibleException();
        }
        VlessSubscriptionConfig config = config(provision, selection, inbound, client);
        ResolvedSubscriptionConfigEntry entry = new ResolvedSubscriptionConfigEntry(
                1,
                "VLESS",
                config.remark(),
                vlessUriBuilder.build(config),
                config.address(),
                config.port(),
                normalizedLower(config.transportType(), "tcp"),
                normalizedLower(config.security(), "reality")
        );
        return new SubscriptionDeliveryContent(
                subscription.getId(),
                subscription.getUserId(),
                provision.getId(),
                subscription.getStatus(),
                selection.getPlanNameSnapshot(),
                subscription.getExpiresAt(),
                subscription.getAccessTokenPrefix(),
                subscription.getTokenVersion(),
                List.of(entry)
        );
    }

    String buildValidatedSubscriptionUrl(Long telegramUserId, UUID subscriptionId, String rawToken) {
        User user = userRepository.findByTelegramUserId(telegramUserId)
                .orElseThrow(() -> new UserNotFoundException(telegramUserId));
        Subscription subscription = subscriptionRepository.findByUserIdAndId(user.getId(), subscriptionId)
                .orElseThrow(() -> new SubscriptionNotFoundException(subscriptionId));
        validateToken(subscription, rawToken);
        XuiClientProvision provision = validateSubscriptionAndProvision(subscription);
        PlanSelection selection = planSelectionRepository.findById(subscription.getPlanSelectionId())
                .orElseThrow(SubscriptionNotAccessibleException::new);
        Order order = orderRepository.findById(subscription.getOrderId())
                .orElseThrow(SubscriptionNotAccessibleException::new);
        validateRelationships(subscription, provision, user, selection, order);
        return subscriptionProperties.subscriptionUrl(SubscriptionAccessToken.normalize(rawToken)).toString();
    }

    private void validateToken(Subscription subscription, String rawToken) {
        String normalized;
        try {
            normalized = SubscriptionAccessToken.normalize(rawToken);
        } catch (IllegalArgumentException exception) {
            throw new SubscriptionNotFoundException(subscription.getId());
        }
        if (normalized.length() > subscriptionProperties.maxTokenLength()) {
            throw new SubscriptionNotFoundException(subscription.getId());
        }
        if (!tokenHasher.matches(normalized, subscription.getAccessTokenHash())) {
            throw new SubscriptionNotFoundException(subscription.getId());
        }
    }

    private XuiClientProvision validateSubscriptionAndProvision(Subscription subscription) {
        Instant now = clock.now();
        if (subscription.isExpiredAt(now)) {
            lifecycleTransaction.expire(subscription.getId(), now);
            throw new SubscriptionNotAccessibleException();
        }
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new SubscriptionNotAccessibleException();
        }
        XuiClientProvision provision = provisionRepository.findById(subscription.getXuiClientProvisionId())
                .orElseThrow(SubscriptionNotAccessibleException::new);
        if (provision.getStatus() != XuiProvisionStatus.ACTIVE || provision.getDeletedAt() != null) {
            throw new SubscriptionNotAccessibleException();
        }
        if (!subscription.getUserId().equals(provision.getUserId())
                || !subscription.getPlanSelectionId().equals(provision.getPlanSelectionId())
                || subscription.getInboundId() != provision.getInboundId()
                || !subscription.getRemoteClientId().toString().equalsIgnoreCase(provision.getRemoteClientId())) {
            lifecycleTransaction.markInvalid(subscription.getId());
            throw new SubscriptionNotAccessibleException();
        }
        return provision;
    }

    private void validateRelationships(
            Subscription subscription,
            XuiClientProvision provision,
            User user,
            PlanSelection selection,
            Order order
    ) {
        boolean valid = subscription.getUserId().equals(user.getId())
                && provision.getUserId().equals(user.getId())
                && subscription.getPlanSelectionId().equals(selection.getId())
                && provision.getPlanSelectionId().equals(selection.getId())
                && Objects.equals(order.getPlanSelectionId(), selection.getId())
                && Objects.equals(order.getPlanId(), selection.getPlanId());
        if (!valid) {
            lifecycleTransaction.markInvalid(subscription.getId());
            throw new SubscriptionNotAccessibleException();
        }
    }

    private VlessSubscriptionConfig config(
            XuiClientProvision provision,
            PlanSelection selection,
            XuiInboundSnapshot inbound,
            XuiClientSnapshot client
    ) {
        if (!"VLESS".equalsIgnoreCase(inbound.protocol())) {
            throw new UnsupportedInboundConfigurationException("Only VLESS subscription entries are supported");
        }
        return new VlessSubscriptionConfig(
                UUID.fromString(provision.getRemoteClientId()),
                endpointProperties.publicHost(),
                endpointProperties.overridePort() == null ? inbound.port() : endpointProperties.overridePort(),
                "none",
                normalizedLower(inbound.securityType(), "reality"),
                inbound.serverName(),
                inbound.publicKey(),
                inbound.shortId(),
                endpointProperties.defaultFingerprint(),
                client.flow(),
                normalizedLower(inbound.streamNetwork(), "tcp"),
                null,
                null,
                null,
                null,
                endpointProperties.defaultRemarkPrefix() + " " + selection.getPlanNameSnapshot()
        );
    }

    private static boolean matchesClient(XuiClientProvision provision, XuiClientSnapshot candidate) {
        return candidate.clientId() != null
                && candidate.clientId().equalsIgnoreCase(provision.getRemoteClientId())
                && (candidate.email() == null
                || candidate.email().isBlank()
                || candidate.email().equalsIgnoreCase(provision.getRemoteEmail()));
    }

    private static String normalizedLower(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
