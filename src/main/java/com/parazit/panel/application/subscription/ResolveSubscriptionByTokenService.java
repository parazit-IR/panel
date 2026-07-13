package com.parazit.panel.application.subscription;

import com.parazit.panel.application.port.in.subscription.ResolveSubscriptionByTokenUseCase;
import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.application.port.out.security.SubscriptionTokenHasher;
import com.parazit.panel.application.port.out.xui.XuiInboundClient;
import com.parazit.panel.application.subscription.model.SubscriptionConfigEntry;
import com.parazit.panel.application.subscription.model.SubscriptionContent;
import com.parazit.panel.application.subscription.model.VlessSubscriptionConfig;
import com.parazit.panel.application.subscription.render.SubscriptionRenderer;
import com.parazit.panel.application.subscription.result.ResolvedSubscriptionContent;
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
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ResolveSubscriptionByTokenService implements ResolveSubscriptionByTokenUseCase {

    private static final Logger log = LoggerFactory.getLogger(ResolveSubscriptionByTokenService.class);

    private final SubscriptionTokenHasher tokenHasher;
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final PlanSelectionRepository planSelectionRepository;
    private final XuiClientProvisionRepository provisionRepository;
    private final XuiInboundClient inboundClient;
    private final SubscriptionRenderer renderer;
    private final SubscriptionAccessMetricsService metricsService;
    private final SubscriptionLifecycleTransaction lifecycleTransaction;
    private final SystemClockPort clock;
    private final SubscriptionProperties properties;
    private final SubscriptionEndpointProperties endpointProperties;

    public ResolveSubscriptionByTokenService(
            SubscriptionTokenHasher tokenHasher,
            SubscriptionRepository subscriptionRepository,
            UserRepository userRepository,
            OrderRepository orderRepository,
            PlanSelectionRepository planSelectionRepository,
            XuiClientProvisionRepository provisionRepository,
            XuiInboundClient inboundClient,
            SubscriptionRenderer renderer,
            SubscriptionAccessMetricsService metricsService,
            SubscriptionLifecycleTransaction lifecycleTransaction,
            SystemClockPort clock,
            SubscriptionProperties properties,
            SubscriptionEndpointProperties endpointProperties
    ) {
        this.tokenHasher = Objects.requireNonNull(tokenHasher, "tokenHasher must not be null");
        this.subscriptionRepository = Objects.requireNonNull(subscriptionRepository, "subscriptionRepository must not be null");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository must not be null");
        this.planSelectionRepository = Objects.requireNonNull(planSelectionRepository, "planSelectionRepository must not be null");
        this.provisionRepository = Objects.requireNonNull(provisionRepository, "provisionRepository must not be null");
        this.inboundClient = Objects.requireNonNull(inboundClient, "inboundClient must not be null");
        this.renderer = Objects.requireNonNull(renderer, "renderer must not be null");
        this.metricsService = Objects.requireNonNull(metricsService, "metricsService must not be null");
        this.lifecycleTransaction = Objects.requireNonNull(lifecycleTransaction, "lifecycleTransaction must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.endpointProperties = Objects.requireNonNull(endpointProperties, "endpointProperties must not be null");
    }

    @Override
    public ResolvedSubscriptionContent resolve(String rawToken, String requestedFormat) {
        String normalizedToken;
        try {
            normalizedToken = SubscriptionAccessToken.normalize(rawToken);
        } catch (IllegalArgumentException exception) {
            throw new SubscriptionTokenInvalidException();
        }
        if (normalizedToken.length() > properties.maxTokenLength()) {
            throw new SubscriptionTokenInvalidException();
        }
        String hash = tokenHasher.hash(normalizedToken);
        Subscription subscription = subscriptionRepository.findByAccessTokenHash(hash)
                .filter(found -> tokenHasher.matches(normalizedToken, found.getAccessTokenHash()))
                .orElseThrow(SubscriptionTokenInvalidException::new);
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
        validateProvision(subscription, provision);
        User user = userRepository.findById(subscription.getUserId())
                .orElseThrow(SubscriptionNotAccessibleException::new);
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

        SubscriptionContent content = content(subscription, provision, selection, inbound, client);
        String format = selectFormat(requestedFormat);
        var rendered = "plain".equals(format) ? renderer.renderPlain(content) : renderer.renderBase64(content);
        metricsService.recordSuccessfulAccess(subscription.getId());
        log.atInfo()
                .addKeyValue("subscriptionId", subscription.getId())
                .addKeyValue("provisionId", provision.getId())
                .addKeyValue("format", format)
                .log("Subscription content rendered");
        return new ResolvedSubscriptionContent(subscription.getId(), provision.getId(), format, rendered);
    }

    private void validateProvision(Subscription subscription, XuiClientProvision provision) {
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
    }

    private void validateRelationships(
            Subscription subscription,
            XuiClientProvision provision,
            User user,
            PlanSelection selection,
            Order order
    ) {
        boolean valid = subscription.getUserId().equals(user.getId())
                && subscription.getPlanSelectionId().equals(selection.getId())
                && subscription.getOrderId().equals(order.getId())
                && provision.getUserId().equals(user.getId())
                && provision.getPlanSelectionId().equals(selection.getId())
                && Objects.equals(order.getPlanSelectionId(), selection.getId())
                && Objects.equals(order.getPlanId(), selection.getPlanId());
        if (!valid) {
            lifecycleTransaction.markInvalid(subscription.getId());
            throw new SubscriptionNotAccessibleException();
        }
    }

    private SubscriptionContent content(
            Subscription subscription,
            XuiClientProvision provision,
            PlanSelection selection,
            XuiInboundSnapshot inbound,
            XuiClientSnapshot client
    ) {
        VlessSubscriptionConfig config = new VlessSubscriptionConfig(
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
        Long total = client.totalTrafficLimitBytes() > 0 ? client.totalTrafficLimitBytes() : provision.getTrafficLimitBytes();
        long upload = Math.max(0, client.uploadBytes());
        long download = Math.max(0, client.downloadBytes());
        long consumed = Math.addExact(upload, download);
        Long remaining = total == null || total == 0 ? null : Math.max(0, total - consumed);
        return new SubscriptionContent(
                properties.profileTitle(),
                List.of(new SubscriptionConfigEntry(config)),
                subscription.getExpiresAt(),
                properties.includeUsageHeaders() ? upload : null,
                properties.includeUsageHeaders() ? download : null,
                properties.includeUsageHeaders() ? total : null,
                properties.includeUsageHeaders() ? remaining : null,
                properties.supportUrl() == null ? null : properties.supportUrl().toString(),
                properties.profileUpdateIntervalHours() == null ? null : String.valueOf(properties.profileUpdateIntervalHours())
        );
    }

    private String selectFormat(String requestedFormat) {
        String selected = requestedFormat == null || requestedFormat.isBlank()
                ? properties.defaultFormat()
                : requestedFormat.trim().toLowerCase(Locale.ROOT);
        if ("plain".equals(selected) && properties.allowPlainFormat()) {
            return selected;
        }
        if ("base64".equals(selected) && properties.allowBase64Format()) {
            return selected;
        }
        throw new IllegalArgumentException("Unsupported subscription format");
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
