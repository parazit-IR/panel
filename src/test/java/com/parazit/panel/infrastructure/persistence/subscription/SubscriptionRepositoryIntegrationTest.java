package com.parazit.panel.infrastructure.persistence.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parazit.panel.domain.order.Order;
import com.parazit.panel.domain.order.repository.OrderRepository;
import com.parazit.panel.domain.plan.Plan;
import com.parazit.panel.domain.plan.repository.PlanRepository;
import com.parazit.panel.domain.plan.selection.PlanSelection;
import com.parazit.panel.domain.plan.selection.repository.PlanSelectionRepository;
import com.parazit.panel.domain.subscription.Subscription;
import com.parazit.panel.domain.subscription.SubscriptionStatus;
import com.parazit.panel.domain.subscription.repository.SubscriptionRepository;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.UserLanguage;
import com.parazit.panel.domain.user.repository.UserRepository;
import com.parazit.panel.domain.xui.provisioning.XuiClientProvision;
import com.parazit.panel.domain.xui.provisioning.repository.XuiClientProvisionRepository;
import com.parazit.panel.test.fixture.PlanTestData;
import com.parazit.panel.test.support.DatabaseCleaner;
import com.parazit.panel.test.support.PostgreSqlContainerSupport;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestConstructor;

@SpringBootTest(properties = {
        "spring.profiles.active=local",
        "spring.security.user.name=test",
        "spring.security.user.password=test"
})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class SubscriptionRepositoryIntegrationTest extends PostgreSqlContainerSupport {

    private static final Instant NOW = Instant.parse("2026-07-13T00:00:00Z");

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final PlanSelectionRepository planSelectionRepository;
    private final OrderRepository orderRepository;
    private final XuiClientProvisionRepository provisionRepository;
    private final JdbcTemplate jdbcTemplate;

    SubscriptionRepositoryIntegrationTest(
            SubscriptionRepository subscriptionRepository,
            UserRepository userRepository,
            PlanRepository planRepository,
            PlanSelectionRepository planSelectionRepository,
            OrderRepository orderRepository,
            XuiClientProvisionRepository provisionRepository,
            JdbcTemplate jdbcTemplate
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
        this.planRepository = planRepository;
        this.planSelectionRepository = planSelectionRepository;
        this.orderRepository = orderRepository;
        this.provisionRepository = provisionRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @BeforeEach
    void setUp() {
        DatabaseCleaner.cleanPlanSelectionTables(jdbcTemplate);
    }

    @Test
    void savesLoadsAndUpdatesAccessMetadata() {
        Fixture fixture = fixture();
        Subscription subscription = subscription(fixture, hash('a'));
        subscription = subscriptionRepository.save(subscription);

        assertThat(subscriptionRepository.findByAccessTokenHash(hash('a'))).contains(subscription);
        assertThat(subscriptionRepository.findByXuiClientProvisionId(fixture.provision.getId())).contains(subscription);
        assertThat(subscriptionRepository.findAllByUserIdOrderByCreatedAtDesc(fixture.user.getId())).singleElement();

        assertThat(subscriptionRepository.incrementAccessMetadata(subscription.getId(), NOW.plusSeconds(10))).isTrue();
        Subscription updated = subscriptionRepository.findById(subscription.getId()).orElseThrow();
        assertThat(updated.getAccessCount()).isEqualTo(1);
        assertThat(updated.getLastAccessedAt()).isEqualTo(NOW.plusSeconds(10));
    }

    @Test
    void databaseEnforcesUniqueProvisionAndTokenHash() {
        Fixture fixture = fixture();
        subscriptionRepository.save(subscription(fixture, hash('a')));

        assertThatThrownBy(() -> subscriptionRepository.save(subscription(fixture, hash('b'))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void persistsLifecycleStatusAndTokenRotation() {
        Fixture fixture = fixture();
        Subscription subscription = subscriptionRepository.save(subscription(fixture, hash('a')));

        subscription.rotateToken(hash('b'), "sub_prefix_b");
        subscription.suspend();
        subscription = subscriptionRepository.save(subscription);

        Subscription loaded = subscriptionRepository.findById(subscription.getId()).orElseThrow();
        assertThat(loaded.getStatus()).isEqualTo(SubscriptionStatus.SUSPENDED);
        assertThat(loaded.getTokenVersion()).isEqualTo(2);
        assertThat(loaded.getAccessTokenHash()).isEqualTo(hash('b'));
    }

    private Fixture fixture() {
        User user = userRepository.save(User.create(930001L, "subrepo", "Ali", null, UserLanguage.FA, NOW));
        Plan plan = planRepository.save(PlanTestData.trafficLimitedPlan("SUB_REPO_" + UUID.randomUUID(), 1));
        plan.activate();
        plan = planRepository.save(plan);
        PlanSelection selection = planSelectionRepository.save(PlanSelection.create(user.getId(), plan, NOW, Duration.ofHours(1)));
        Order order = Order.createForPlanSelection(user.getId(), plan.getId(), selection.getId(), 1000, "IRT");
        order.markPaid(NOW.plusSeconds(1));
        order.markProvisioning(NOW.plusSeconds(2));
        order.markCompleted(NOW.plusSeconds(3));
        order = orderRepository.save(order);
        XuiClientProvision provision = XuiClientProvision.createPending(
                user.getId(),
                plan.getId(),
                selection.getId(),
                7,
                UUID.randomUUID().toString(),
                "repo_" + UUID.randomUUID(),
                "sub-" + UUID.randomUUID(),
                1024,
                NOW.plus(Duration.ofDays(30)),
                1,
                NOW
        );
        provision.markProvisioning();
        provision.markActive(NOW.plusSeconds(4));
        provision = provisionRepository.save(provision);
        return new Fixture(user, selection, order, provision);
    }

    private static Subscription subscription(Fixture fixture, String hash) {
        return Subscription.activate(
                fixture.user.getId(),
                fixture.order.getId(),
                fixture.selection.getId(),
                fixture.provision.getId(),
                fixture.provision.getInboundId(),
                UUID.fromString(fixture.provision.getRemoteClientId()),
                hash,
                "sub_prefix",
                NOW,
                fixture.provision.getExpiresAt(),
                "Repo plan",
                "v1"
        );
    }

    private static String hash(char value) {
        return String.valueOf(value).repeat(64);
    }

    private record Fixture(
            User user,
            PlanSelection selection,
            Order order,
            XuiClientProvision provision
    ) {
    }
}
