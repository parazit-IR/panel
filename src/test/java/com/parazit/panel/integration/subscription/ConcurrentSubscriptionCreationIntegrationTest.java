package com.parazit.panel.integration.subscription;

import static org.assertj.core.api.Assertions.assertThat;

import com.parazit.panel.application.port.in.subscription.CreateSubscriptionUseCase;
import com.parazit.panel.application.port.in.subscription.RotateSubscriptionTokenUseCase;
import com.parazit.panel.application.port.out.security.SubscriptionTokenHasher;
import com.parazit.panel.application.subscription.command.CreateSubscriptionCommand;
import com.parazit.panel.application.subscription.command.RotateSubscriptionTokenCommand;
import com.parazit.panel.application.subscription.result.CreateSubscriptionResult;
import com.parazit.panel.domain.order.Order;
import com.parazit.panel.domain.order.repository.OrderRepository;
import com.parazit.panel.domain.plan.Plan;
import com.parazit.panel.domain.plan.repository.PlanRepository;
import com.parazit.panel.domain.plan.selection.PlanSelection;
import com.parazit.panel.domain.plan.selection.repository.PlanSelectionRepository;
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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestConstructor;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(properties = {
        "spring.profiles.active=local",
        "spring.security.user.name=test",
        "spring.security.user.password=test"
})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class ConcurrentSubscriptionCreationIntegrationTest extends PostgreSqlContainerSupport {

    private static final Instant NOW = Instant.parse("2026-07-13T00:00:00Z");
    private static final long TELEGRAM_USER_ID = 950001L;

    private final CreateSubscriptionUseCase createUseCase;
    private final RotateSubscriptionTokenUseCase rotateUseCase;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionTokenHasher tokenHasher;
    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final PlanSelectionRepository planSelectionRepository;
    private final OrderRepository orderRepository;
    private final XuiClientProvisionRepository provisionRepository;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    ConcurrentSubscriptionCreationIntegrationTest(
            CreateSubscriptionUseCase createUseCase,
            RotateSubscriptionTokenUseCase rotateUseCase,
            SubscriptionRepository subscriptionRepository,
            SubscriptionTokenHasher tokenHasher,
            UserRepository userRepository,
            PlanRepository planRepository,
            PlanSelectionRepository planSelectionRepository,
            OrderRepository orderRepository,
            XuiClientProvisionRepository provisionRepository,
            JdbcTemplate jdbcTemplate,
            TransactionTemplate transactionTemplate
    ) {
        this.createUseCase = createUseCase;
        this.rotateUseCase = rotateUseCase;
        this.subscriptionRepository = subscriptionRepository;
        this.tokenHasher = tokenHasher;
        this.userRepository = userRepository;
        this.planRepository = planRepository;
        this.planSelectionRepository = planSelectionRepository;
        this.orderRepository = orderRepository;
        this.provisionRepository = provisionRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
    }

    @BeforeEach
    void setUp() {
        DatabaseCleaner.cleanPlanSelectionTables(jdbcTemplate);
    }

    @Test
    void concurrentCreationCreatesOneSubscriptionAndReturnsRawTokenOnce() throws Exception {
        XuiClientProvision provision = fixture();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch firstCreatedInsideOpenTransaction = new CountDownLatch(1);
        CountDownLatch releaseFirstTransaction = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);

        try {
            Future<CreateSubscriptionResult> first = executor.submit(() -> transactionTemplate.execute(status -> {
                CreateSubscriptionResult result = createUseCase.create(command(provision));
                firstCreatedInsideOpenTransaction.countDown();
                awaitUnchecked(releaseFirstTransaction);
                return result;
            }));
            assertThat(firstCreatedInsideOpenTransaction.await(5, TimeUnit.SECONDS)).isTrue();

            Future<CreateSubscriptionResult> second = executor.submit(() -> {
                secondStarted.countDown();
                return createUseCase.create(command(provision));
            });
            assertThat(secondStarted.await(5, TimeUnit.SECONDS)).isTrue();

            releaseFirstTransaction.countDown();
            List<CreateSubscriptionResult> results = List.of(
                    first.get(5, TimeUnit.SECONDS),
                    second.get(5, TimeUnit.SECONDS)
            );

            assertThat(results).extracting(CreateSubscriptionResult::subscriptionId).containsOnly(results.getFirst().subscriptionId());
            assertThat(results).filteredOn(result -> result.rawAccessToken() != null).hasSize(1);
            assertThat(subscriptionRepository.findByXuiClientProvisionId(provision.getId())).isPresent();
            assertThat(subscriptionRepository.findAllByUserIdOrderByCreatedAtDesc(provision.getUserId())).hasSize(1);
        } finally {
            releaseFirstTransaction.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void concurrentRotationsSerializeVersionsAndLeaveOneFinalTokenHash() throws Exception {
        XuiClientProvision provision = fixture();
        CreateSubscriptionResult created = createUseCase.create(command(provision));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<CreateSubscriptionResult> first = executor.submit(() -> rotateAfterStart(created, ready, start));
            Future<CreateSubscriptionResult> second = executor.submit(() -> rotateAfterStart(created, ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<CreateSubscriptionResult> results = List.of(
                    first.get(5, TimeUnit.SECONDS),
                    second.get(5, TimeUnit.SECONDS)
            );

            assertThat(results).extracting(CreateSubscriptionResult::subscriptionId).containsOnly(created.subscriptionId());
            assertThat(results).extracting(CreateSubscriptionResult::tokenVersion).containsExactlyInAnyOrder(2, 3);
            assertThat(results).extracting(CreateSubscriptionResult::rawAccessToken).doesNotContainNull().doesNotHaveDuplicates();
            assertThat(subscriptionRepository.findAllByUserIdOrderByCreatedAtDesc(provision.getUserId())).hasSize(1);
            String finalHash = subscriptionRepository.findByXuiClientProvisionId(provision.getId())
                    .orElseThrow()
                    .getAccessTokenHash();
            assertThat(results.stream().map(CreateSubscriptionResult::rawAccessToken).map(tokenHasher::hash).toList())
                    .contains(finalHash);
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    private static CreateSubscriptionCommand command(XuiClientProvision provision) {
        return new CreateSubscriptionCommand(TELEGRAM_USER_ID, provision.getId());
    }

    private XuiClientProvision fixture() {
        User user = userRepository.save(User.create(TELEGRAM_USER_ID, "subrace", "Ali", null, UserLanguage.FA, NOW));
        Plan plan = planRepository.save(PlanTestData.trafficLimitedPlan("SUB_RACE_" + UUID.randomUUID(), 1));
        plan.activate();
        plan = planRepository.save(plan);
        PlanSelection selection = planSelectionRepository.save(PlanSelection.create(user.getId(), plan, NOW, Duration.ofHours(1)));
        Order order = Order.createForPlanSelection(user.getId(), plan.getId(), selection.getId(), 1000, "IRT");
        order.markPaid(NOW.plusSeconds(1));
        order.markProvisioning(NOW.plusSeconds(2));
        order.markCompleted(NOW.plusSeconds(3));
        orderRepository.save(order);
        XuiClientProvision provision = XuiClientProvision.createPending(
                user.getId(),
                plan.getId(),
                selection.getId(),
                7,
                UUID.randomUUID().toString(),
                "race_" + UUID.randomUUID(),
                "sub-" + UUID.randomUUID(),
                1024,
                NOW.plus(Duration.ofDays(30)),
                1,
                NOW
        );
        provision.markProvisioning();
        provision.markActive(NOW.plusSeconds(4));
        return provisionRepository.save(provision);
    }

    private CreateSubscriptionResult rotateAfterStart(
            CreateSubscriptionResult created,
            CountDownLatch ready,
            CountDownLatch start
    ) {
        ready.countDown();
        awaitUnchecked(start);
        return rotateUseCase.rotate(new RotateSubscriptionTokenCommand(
                TELEGRAM_USER_ID,
                created.subscriptionId(),
                "concurrent test"
        ));
    }

    private static void awaitUnchecked(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for test latch");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for test latch", exception);
        }
    }
}
