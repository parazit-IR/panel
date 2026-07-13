package com.parazit.panel.integration.xui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parazit.panel.application.xui.client.XuiClientLifecycleTransaction;
import com.parazit.panel.application.xui.client.XuiClientOperationFingerprint;
import com.parazit.panel.application.xui.client.XuiClientOperationInProgressException;
import com.parazit.panel.application.xui.client.XuiClientOperationTransaction;
import com.parazit.panel.application.xui.client.XuiOperationIdConflictException;
import com.parazit.panel.domain.plan.Plan;
import com.parazit.panel.domain.plan.repository.PlanRepository;
import com.parazit.panel.domain.plan.selection.PlanSelection;
import com.parazit.panel.domain.plan.selection.repository.PlanSelectionRepository;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.UserLanguage;
import com.parazit.panel.domain.user.repository.UserRepository;
import com.parazit.panel.domain.xui.operation.XuiClientOperation;
import com.parazit.panel.domain.xui.operation.XuiClientOperationStatus;
import com.parazit.panel.domain.xui.operation.XuiClientOperationType;
import com.parazit.panel.domain.xui.operation.repository.XuiClientOperationRepository;
import com.parazit.panel.domain.xui.provisioning.XuiClientProvision;
import com.parazit.panel.domain.xui.provisioning.repository.XuiClientProvisionRepository;
import com.parazit.panel.test.fixture.PlanTestData;
import com.parazit.panel.test.support.DatabaseCleaner;
import com.parazit.panel.test.support.PostgreSqlContainerSupport;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
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
class ConcurrentXuiClientOperationIntegrationTest extends PostgreSqlContainerSupport {

    private static final Instant NOW = Instant.parse("2026-07-12T00:00:00Z");
    private static final long TELEGRAM_USER_ID = 920001L;

    private final XuiClientOperationTransaction operationTransaction;
    private final XuiClientLifecycleTransaction lifecycleTransaction;
    private final XuiClientOperationRepository operationRepository;
    private final XuiClientProvisionRepository provisionRepository;
    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final PlanSelectionRepository planSelectionRepository;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    ConcurrentXuiClientOperationIntegrationTest(
            XuiClientOperationTransaction operationTransaction,
            XuiClientLifecycleTransaction lifecycleTransaction,
            XuiClientOperationRepository operationRepository,
            XuiClientProvisionRepository provisionRepository,
            UserRepository userRepository,
            PlanRepository planRepository,
            PlanSelectionRepository planSelectionRepository,
            JdbcTemplate jdbcTemplate,
            TransactionTemplate transactionTemplate
    ) {
        this.operationTransaction = operationTransaction;
        this.lifecycleTransaction = lifecycleTransaction;
        this.operationRepository = operationRepository;
        this.provisionRepository = provisionRepository;
        this.userRepository = userRepository;
        this.planRepository = planRepository;
        this.planSelectionRepository = planSelectionRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
    }

    @BeforeEach
    void setUp() {
        DatabaseCleaner.cleanPlanSelectionTables(jdbcTemplate);
    }

    @Test
    void concurrentResetAndRenewPrepareLeaveOnlyOneInProgressOperation() throws Exception {
        XuiClientProvision provision = activeProvision();
        UUID resetOperationId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        UUID renewOperationId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch firstPreparedInsideOpenTransaction = new CountDownLatch(1);
        CountDownLatch releaseFirstTransaction = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);

        try {
            Future<?> first = executor.submit(() -> transactionTemplate.execute(status -> {
                operationTransaction.prepare(
                        TELEGRAM_USER_ID,
                        provision.getId(),
                        resetOperationId,
                        XuiClientOperationType.RESET_TRAFFIC,
                        fingerprint(provision.getId(), XuiClientOperationType.RESET_TRAFFIC, Map.of("resetTraffic", true)),
                        NOW
                );
                firstPreparedInsideOpenTransaction.countDown();
                awaitUnchecked(releaseFirstTransaction);
                return null;
            }));
            assertThat(firstPreparedInsideOpenTransaction.await(5, TimeUnit.SECONDS)).isTrue();

            Future<?> second = executor.submit(() -> {
                secondStarted.countDown();
                return operationTransaction.prepare(
                        TELEGRAM_USER_ID,
                        provision.getId(),
                        renewOperationId,
                        XuiClientOperationType.RENEW_EXPIRY,
                        fingerprint(provision.getId(), XuiClientOperationType.RENEW_EXPIRY, Map.of("durationDays", 30, "renewalMode", "EXTEND_FROM_EXPIRY")),
                        NOW.plusSeconds(1)
                );
            });
            assertThat(secondStarted.await(5, TimeUnit.SECONDS)).isTrue();

            releaseFirstTransaction.countDown();
            first.get(5, TimeUnit.SECONDS);
            assertThatThrownBy(() -> second.get(5, TimeUnit.SECONDS))
                    .isInstanceOf(ExecutionException.class)
                    .cause()
                    .isInstanceOf(XuiClientOperationInProgressException.class);

            assertThat(operationRepository.findAllByProvisionIdOrderByRequestedAtDesc(provision.getId()))
                    .singleElement()
                    .satisfies(operation -> {
                        assertThat(operation.getOperationId()).isEqualTo(resetOperationId);
                        assertThat(operation.getStatus()).isEqualTo(XuiClientOperationStatus.IN_PROGRESS);
                    });
        } finally {
            releaseFirstTransaction.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void deleteAndDisableAreRejectedWhileUpdateOperationIsInProgress() {
        XuiClientProvision provision = activeProvision();
        UUID operationId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
        operationTransaction.prepare(
                TELEGRAM_USER_ID,
                provision.getId(),
                operationId,
                XuiClientOperationType.ADD_TRAFFIC,
                fingerprint(provision.getId(), XuiClientOperationType.ADD_TRAFFIC, Map.of("additionalTrafficBytes", 512)),
                NOW
        );

        assertThatThrownBy(() -> lifecycleTransaction.prepareDelete(TELEGRAM_USER_ID, provision.getId(), true))
                .isInstanceOf(XuiClientOperationInProgressException.class);
        assertThatThrownBy(() -> lifecycleTransaction.prepareDisable(TELEGRAM_USER_ID, provision.getId()))
                .isInstanceOf(XuiClientOperationInProgressException.class);
    }

    @Test
    void succeededOperationReplaysAndConflictingReplayIsRejected() {
        XuiClientProvision provision = activeProvision();
        UUID operationId = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
        String fingerprint = fingerprint(
                provision.getId(),
                XuiClientOperationType.REPLACE_TRAFFIC_LIMIT,
                Map.of("trafficLimitBytes", 2048)
        );
        operationTransaction.prepare(
                TELEGRAM_USER_ID,
                provision.getId(),
                operationId,
                XuiClientOperationType.REPLACE_TRAFFIC_LIMIT,
                fingerprint,
                NOW
        );
        operationTransaction.markSucceeded(provision.getId(), operationId, NOW.plusSeconds(1), saved -> {
        });

        assertThat(operationTransaction.prepare(
                TELEGRAM_USER_ID,
                provision.getId(),
                operationId,
                XuiClientOperationType.REPLACE_TRAFFIC_LIMIT,
                fingerprint,
                NOW.plusSeconds(2)
        ).replay()).isTrue();

        assertThatThrownBy(() -> operationTransaction.prepare(
                TELEGRAM_USER_ID,
                provision.getId(),
                operationId,
                XuiClientOperationType.REPLACE_TRAFFIC_LIMIT,
                fingerprint(provision.getId(), XuiClientOperationType.REPLACE_TRAFFIC_LIMIT, Map.of("trafficLimitBytes", 4096)),
                NOW.plusSeconds(3)
        )).isInstanceOf(XuiOperationIdConflictException.class);
    }

    private XuiClientProvision activeProvision() {
        User user = userRepository.save(User.create(TELEGRAM_USER_ID, "xuiop", "Ali", null, UserLanguage.FA, NOW));
        Plan plan = planRepository.save(PlanTestData.trafficLimitedPlan("XUI_CONCURRENT_" + UUID.randomUUID(), 1));
        plan.activate();
        plan = planRepository.save(plan);
        PlanSelection selection = planSelectionRepository.save(
                PlanSelection.create(user.getId(), plan, NOW, Duration.ofHours(1))
        );
        XuiClientProvision provision = XuiClientProvision.createPending(
                user.getId(),
                plan.getId(),
                selection.getId(),
                7,
                UUID.randomUUID().toString(),
                "vpn_" + UUID.randomUUID(),
                "sub-" + UUID.randomUUID(),
                1024,
                NOW.plus(Duration.ofDays(30)),
                1,
                NOW
        );
        provision.markProvisioning();
        provision.markActive(NOW.plusSeconds(1));
        return provisionRepository.save(provision);
    }

    private static String fingerprint(UUID provisionId, XuiClientOperationType type, Map<String, ?> values) {
        return XuiClientOperationFingerprint.of(provisionId, type, values);
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
