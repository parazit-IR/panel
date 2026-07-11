package com.parazit.panel.integration.plan.selection;

import com.parazit.panel.test.support.PostgreSqlContainerSupport;
import com.parazit.panel.test.support.DatabaseCleaner;

import static org.assertj.core.api.Assertions.assertThat;

import com.parazit.panel.application.plan.selection.command.SelectPlanCommand;
import com.parazit.panel.application.plan.selection.result.PlanSelectionResult;
import com.parazit.panel.application.port.in.plan.selection.SelectPlanUseCase;
import com.parazit.panel.domain.plan.Plan;
import com.parazit.panel.domain.plan.selection.repository.PlanSelectionRepository;
import com.parazit.panel.domain.plan.repository.PlanRepository;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.UserLanguage;
import com.parazit.panel.domain.user.repository.UserRepository;
import com.parazit.panel.test.fixture.PlanTestData;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestConstructor;

@SpringBootTest(properties = {
        "spring.profiles.active=local",
        "spring.security.user.name=test",
        "spring.security.user.password=test",
        "app.plan-selection.ttl=PT30M"
})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class ConcurrentPlanSelectionIntegrationTest extends PostgreSqlContainerSupport {

    private static final Instant NOW = Instant.parse("2026-07-11T00:00:00Z");

    private final SelectPlanUseCase selectPlanUseCase;
    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final PlanSelectionRepository planSelectionRepository;
    private final JdbcTemplate jdbcTemplate;

    ConcurrentPlanSelectionIntegrationTest(
            SelectPlanUseCase selectPlanUseCase,
            UserRepository userRepository,
            PlanRepository planRepository,
            PlanSelectionRepository planSelectionRepository,
            JdbcTemplate jdbcTemplate
    ) {
        this.selectPlanUseCase = selectPlanUseCase;
        this.userRepository = userRepository;
        this.planRepository = planRepository;
        this.planSelectionRepository = planSelectionRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @BeforeEach
    void setUp() {
        DatabaseCleaner.cleanPlanSelectionTables(jdbcTemplate);
    }

    @Test
    void concurrentSamePlanSelectionIsIdempotentAndLeavesOneActiveSelection() throws Exception {
        User user = activeUser(9001L);
        Plan plan = activePlan(PlanTestData.unlimitedPlan("CONCURRENT_SAME", 1));
        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Callable<PlanSelectionResult> task = () -> {
                barrier.await(10, TimeUnit.SECONDS);
                return selectPlanUseCase.select(new SelectPlanCommand(9001L, plan.getId()));
            };

            Future<PlanSelectionResult> firstFuture = executor.submit(task);
            Future<PlanSelectionResult> secondFuture = executor.submit(task);
            PlanSelectionResult first = firstFuture.get(20, TimeUnit.SECONDS);
            PlanSelectionResult second = secondFuture.get(20, TimeUnit.SECONDS);

            assertThat(first.selectionId()).isEqualTo(second.selectionId());
            assertThat(List.of(first.newlySelected(), second.newlySelected()))
                    .containsExactlyInAnyOrder(true, false);
            assertThat(activeSelectionCount(user.getId())).isEqualTo(1);
            assertThat(planSelectionRepository.findActiveByUserId(user.getId()).orElseThrow().getPlanId())
                    .isEqualTo(plan.getId());
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void concurrentDifferentPlanSelectionLeavesExactlyOneActiveSelection() throws Exception {
        User user = activeUser(9002L);
        Plan firstPlan = activePlan(PlanTestData.unlimitedPlan("CONCURRENT_A", 1));
        Plan secondPlan = activePlan(PlanTestData.trafficLimitedPlan("CONCURRENT_B", 2));
        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Callable<PlanSelectionResult> firstTask = () -> {
                barrier.await(10, TimeUnit.SECONDS);
                return selectPlanUseCase.select(new SelectPlanCommand(9002L, firstPlan.getId()));
            };
            Callable<PlanSelectionResult> secondTask = () -> {
                barrier.await(10, TimeUnit.SECONDS);
                return selectPlanUseCase.select(new SelectPlanCommand(9002L, secondPlan.getId()));
            };

            Future<PlanSelectionResult> firstFuture = executor.submit(firstTask);
            Future<PlanSelectionResult> secondFuture = executor.submit(secondTask);
            PlanSelectionResult first = firstFuture.get(20, TimeUnit.SECONDS);
            PlanSelectionResult second = secondFuture.get(20, TimeUnit.SECONDS);

            assertThat(first.selectionId()).isNotEqualTo(second.selectionId());
            assertThat(activeSelectionCount(user.getId())).isEqualTo(1);
            UUID activePlanId = planSelectionRepository.findActiveByUserId(user.getId()).orElseThrow().getPlanId();
            assertThat(activePlanId).isIn(firstPlan.getId(), secondPlan.getId());
            assertThat(totalSelectionCount(user.getId())).isEqualTo(2);
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void concurrentReplacementOfExistingSelectionLeavesExactlyOneActiveSelectionAndValidHistory() throws Exception {
        User user = activeUser(9003L);
        Plan originalPlan = activePlan(PlanTestData.unlimitedPlan("CONCURRENT_ORIGINAL", 1));
        Plan firstReplacement = activePlan(PlanTestData.unlimitedPlan("CONCURRENT_REPLACE_A", 2));
        Plan secondReplacement = activePlan(PlanTestData.trafficLimitedPlan("CONCURRENT_REPLACE_B", 3));
        PlanSelectionResult original = selectPlanUseCase.select(new SelectPlanCommand(9003L, originalPlan.getId()));
        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Callable<PlanSelectionResult> firstTask = () -> {
                barrier.await(10, TimeUnit.SECONDS);
                return selectPlanUseCase.select(new SelectPlanCommand(9003L, firstReplacement.getId()));
            };
            Callable<PlanSelectionResult> secondTask = () -> {
                barrier.await(10, TimeUnit.SECONDS);
                return selectPlanUseCase.select(new SelectPlanCommand(9003L, secondReplacement.getId()));
            };

            Future<PlanSelectionResult> firstFuture = executor.submit(firstTask);
            Future<PlanSelectionResult> secondFuture = executor.submit(secondTask);
            PlanSelectionResult first = firstFuture.get(20, TimeUnit.SECONDS);
            PlanSelectionResult second = secondFuture.get(20, TimeUnit.SECONDS);

            assertThat(first.selectionId()).isNotEqualTo(second.selectionId());
            assertThat(activeSelectionCount(user.getId())).isEqualTo(1);
            assertThat(totalSelectionCount(user.getId())).isEqualTo(3);
            assertThat(planSelectionRepository.findById(original.selectionId()).orElseThrow().getStatus().name())
                    .isEqualTo("CLEARED");
            UUID activePlanId = planSelectionRepository.findActiveByUserId(user.getId()).orElseThrow().getPlanId();
            assertThat(activePlanId).isIn(firstReplacement.getId(), secondReplacement.getId());
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    private User activeUser(Long telegramUserId) {
        return userRepository.save(User.create(telegramUserId, "user" + telegramUserId, "Ali", null, UserLanguage.FA, NOW));
    }

    private Plan activePlan(Plan plan) {
        Plan saved = planRepository.save(plan);
        saved.activate();
        return planRepository.save(saved);
    }

    private long activeSelectionCount(UUID userId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM plan_selections WHERE user_id = ? AND status = 'ACTIVE'",
                Long.class,
                userId
        );
        return count == null ? 0 : count;
    }

    private long totalSelectionCount(UUID userId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM plan_selections WHERE user_id = ?",
                Long.class,
                userId
        );
        return count == null ? 0 : count;
    }
}
