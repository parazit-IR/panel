package com.parazit.panel.integration.plan.selection;

import com.parazit.panel.test.support.PostgreSqlContainerSupport;
import com.parazit.panel.test.support.DatabaseCleaner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parazit.panel.application.plan.catalog.AvailablePlanNotFoundException;
import com.parazit.panel.application.plan.selection.PlanSelectionNotFoundException;
import com.parazit.panel.application.plan.selection.UserNotEligibleForPlanSelectionException;
import com.parazit.panel.application.plan.selection.command.ClearPlanSelectionCommand;
import com.parazit.panel.application.plan.selection.command.SelectPlanCommand;
import com.parazit.panel.application.plan.selection.query.GetCurrentPlanSelectionQuery;
import com.parazit.panel.application.plan.selection.result.PlanSelectionResult;
import com.parazit.panel.application.port.in.plan.selection.ClearPlanSelectionUseCase;
import com.parazit.panel.application.port.in.plan.selection.GetCurrentPlanSelectionUseCase;
import com.parazit.panel.application.port.in.plan.selection.SelectPlanUseCase;
import com.parazit.panel.domain.plan.Plan;
import com.parazit.panel.domain.plan.selection.PlanSelection;
import com.parazit.panel.domain.plan.selection.PlanSelectionStatus;
import com.parazit.panel.domain.plan.selection.repository.PlanSelectionRepository;
import com.parazit.panel.domain.plan.repository.PlanRepository;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.UserLanguage;
import com.parazit.panel.domain.user.repository.UserRepository;
import com.parazit.panel.test.fixture.PlanTestData;
import com.parazit.panel.test.support.MutableClockTestConfiguration;
import com.parazit.panel.test.support.MutableTestClock;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestConstructor;

@SpringBootTest(properties = {
        "spring.profiles.active=local",
        "spring.security.user.name=test",
        "spring.security.user.password=test",
        "app.plan-selection.ttl=PT30M"
})
@Import(MutableClockTestConfiguration.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class PlanSelectionIntegrationTest extends PostgreSqlContainerSupport {

    private static final Instant NOW = Instant.parse("2026-07-11T00:00:00Z");

    private final SelectPlanUseCase selectPlanUseCase;
    private final GetCurrentPlanSelectionUseCase getCurrentPlanSelectionUseCase;
    private final ClearPlanSelectionUseCase clearPlanSelectionUseCase;
    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final PlanSelectionRepository planSelectionRepository;
    private final JdbcTemplate jdbcTemplate;
    private final MutableTestClock clock;
    private final Flyway flyway;

    PlanSelectionIntegrationTest(
            SelectPlanUseCase selectPlanUseCase,
            GetCurrentPlanSelectionUseCase getCurrentPlanSelectionUseCase,
            ClearPlanSelectionUseCase clearPlanSelectionUseCase,
            UserRepository userRepository,
            PlanRepository planRepository,
            PlanSelectionRepository planSelectionRepository,
            JdbcTemplate jdbcTemplate,
            Clock clock,
            Flyway flyway
    ) {
        this.selectPlanUseCase = selectPlanUseCase;
        this.getCurrentPlanSelectionUseCase = getCurrentPlanSelectionUseCase;
        this.clearPlanSelectionUseCase = clearPlanSelectionUseCase;
        this.userRepository = userRepository;
        this.planRepository = planRepository;
        this.planSelectionRepository = planSelectionRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.clock = (MutableTestClock) clock;
        this.flyway = flyway;
    }

    @BeforeEach
    void setUp() {
        DatabaseCleaner.cleanPlanSelectionTables(jdbcTemplate);
        clock.setInstant(NOW);
    }

    @Test
    void firstSelectionSameSelectionReplacementAndClearWork() {
        User user = activeUser(7001L);
        Plan planA = activePlan(PlanTestData.unlimitedPlan("SELECT_A", 1));
        Plan planB = activePlan(PlanTestData.trafficLimitedPlan("SELECT_B", 2));

        PlanSelectionResult first = selectPlanUseCase.select(new SelectPlanCommand(7001L, planA.getId()));
        PlanSelectionResult same = selectPlanUseCase.select(new SelectPlanCommand(7001L, planA.getId()));
        PlanSelectionResult replacement = selectPlanUseCase.select(new SelectPlanCommand(7001L, planB.getId()));

        assertThat(first.newlySelected()).isTrue();
        assertThat(same.selectionId()).isEqualTo(first.selectionId());
        assertThat(same.newlySelected()).isFalse();
        assertThat(replacement.selectionId()).isNotEqualTo(first.selectionId());
        assertThat(replacement.planCode()).isEqualTo("SELECT_B");
        assertThat(planSelectionRepository.findById(first.selectionId()).orElseThrow().getStatus())
                .isEqualTo(PlanSelectionStatus.CLEARED);
        assertThat(planSelectionRepository.findActiveByUserId(user.getId()).orElseThrow().getId())
                .isEqualTo(replacement.selectionId());
        assertThat(activeSelectionCount(user.getId())).isEqualTo(1);

        PlanSelectionResult cleared = clearPlanSelectionUseCase.clear(new ClearPlanSelectionCommand(7001L));
        assertThat(cleared.status()).isEqualTo(PlanSelectionStatus.CLEARED);
        assertThat(planSelectionRepository.findActiveByUserId(user.getId())).isEmpty();
        assertThat(rowCount()).isEqualTo(2);
    }

    @Test
    void expiredSelectionIsMarkedExpiredAndHiddenFromCurrentLookup() {
        User user = activeUser(7002L);
        Plan plan = activePlan(PlanTestData.unlimitedPlan("EXPIRE_ME", 1));
        PlanSelectionResult selected = selectPlanUseCase.select(new SelectPlanCommand(7002L, plan.getId()));

        clock.setInstant(selected.expiresAt());

        assertThatThrownBy(() -> getCurrentPlanSelectionUseCase.getCurrent(new GetCurrentPlanSelectionQuery(7002L)))
                .isInstanceOf(PlanSelectionNotFoundException.class);
        assertThat(planSelectionRepository.findById(selected.selectionId()).orElseThrow().getStatus())
                .isEqualTo(PlanSelectionStatus.EXPIRED);
        assertThat(planSelectionRepository.findActiveByUserId(user.getId())).isEmpty();
    }

    @Test
    void hiddenPlanAndIneligibleUserAreRejectedWithoutMutatingUserOrPlan() {
        User user = activeUser(7003L);
        User blocked = activeUser(7004L);
        blocked.block();
        userRepository.save(blocked);
        Plan draft = planRepository.save(PlanTestData.unlimitedPlan("HIDDEN_SELECT", 1));

        assertThatThrownBy(() -> selectPlanUseCase.select(new SelectPlanCommand(7003L, draft.getId())))
                .isInstanceOf(AvailablePlanNotFoundException.class);
        assertThatThrownBy(() -> selectPlanUseCase.select(new SelectPlanCommand(7004L, activePlan(PlanTestData.unlimitedPlan("ACTIVE_SELECT", 2)).getId())))
                .isInstanceOf(UserNotEligibleForPlanSelectionException.class);

        assertThat(planSelectionRepository.findActiveByUserId(user.getId())).isEmpty();
        assertThat(userRepository.findByTelegramUserId(7004L).orElseThrow().getBlocked()).isTrue();
        assertThat(planRepository.findByCode("HIDDEN_SELECT").orElseThrow().getStatus()).isEqualTo(draft.getStatus());
    }

    @Test
    void flywayMigrationRunsAndHibernateValidationSucceeds() {
        assertThat(flyway.info().current()).isNotNull();
        assertThat(java.util.Arrays.stream(flyway.info().applied()))
                .anySatisfy(info -> assertThat(info.getVersion().getVersion()).isEqualTo("6"));
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

    private long rowCount() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM plan_selections", Long.class);
        return count == null ? 0 : count;
    }
}
