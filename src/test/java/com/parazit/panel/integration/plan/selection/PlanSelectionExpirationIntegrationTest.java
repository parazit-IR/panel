package com.parazit.panel.integration.plan.selection;

import com.parazit.panel.test.support.PostgreSqlContainerSupport;
import com.parazit.panel.test.support.DatabaseCleaner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parazit.panel.application.plan.selection.PlanSelectionNotFoundException;
import com.parazit.panel.application.plan.selection.command.ClearPlanSelectionCommand;
import com.parazit.panel.application.plan.selection.command.SelectPlanCommand;
import com.parazit.panel.application.plan.selection.query.GetCurrentPlanSelectionQuery;
import com.parazit.panel.application.plan.selection.result.PlanSelectionResult;
import com.parazit.panel.application.port.in.plan.selection.ClearPlanSelectionUseCase;
import com.parazit.panel.application.port.in.plan.selection.GetCurrentPlanSelectionUseCase;
import com.parazit.panel.application.port.in.plan.selection.SelectPlanUseCase;
import com.parazit.panel.domain.plan.Plan;
import com.parazit.panel.domain.plan.repository.PlanRepository;
import com.parazit.panel.domain.plan.selection.PlanSelectionStatus;
import com.parazit.panel.domain.plan.selection.repository.PlanSelectionRepository;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.UserLanguage;
import com.parazit.panel.domain.user.repository.UserRepository;
import com.parazit.panel.test.fixture.PlanTestData;
import com.parazit.panel.test.support.MutableClockTestConfiguration;
import com.parazit.panel.test.support.MutableTestClock;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
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
class PlanSelectionExpirationIntegrationTest extends PostgreSqlContainerSupport {

    private static final Instant NOW = Instant.parse("2026-07-11T00:00:00Z");

    private final SelectPlanUseCase selectPlanUseCase;
    private final GetCurrentPlanSelectionUseCase getCurrentPlanSelectionUseCase;
    private final ClearPlanSelectionUseCase clearPlanSelectionUseCase;
    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final PlanSelectionRepository planSelectionRepository;
    private final JdbcTemplate jdbcTemplate;
    private final MutableTestClock clock;

    PlanSelectionExpirationIntegrationTest(
            SelectPlanUseCase selectPlanUseCase,
            GetCurrentPlanSelectionUseCase getCurrentPlanSelectionUseCase,
            ClearPlanSelectionUseCase clearPlanSelectionUseCase,
            UserRepository userRepository,
            PlanRepository planRepository,
            PlanSelectionRepository planSelectionRepository,
            JdbcTemplate jdbcTemplate,
            Clock clock
    ) {
        this.selectPlanUseCase = selectPlanUseCase;
        this.getCurrentPlanSelectionUseCase = getCurrentPlanSelectionUseCase;
        this.clearPlanSelectionUseCase = clearPlanSelectionUseCase;
        this.userRepository = userRepository;
        this.planRepository = planRepository;
        this.planSelectionRepository = planSelectionRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.clock = (MutableTestClock) clock;
    }

    @BeforeEach
    void setUp() {
        DatabaseCleaner.cleanPlanSelectionTables(jdbcTemplate);
        clock.setInstant(NOW);
    }

    @Test
    void selectionExpiresExactlyAtBoundaryAndReselectCreatesOneNewActiveRow() {
        User user = activeUser(21_001L);
        Plan plan = activePlan(PlanTestData.unlimitedPlan("EXPIRATION_PLAN", 1));
        PlanSelectionResult selected = selectPlanUseCase.select(new SelectPlanCommand(user.getTelegramUserId(), plan.getId()));

        clock.setInstant(selected.expiresAt().minusNanos(1));
        assertThat(getCurrentPlanSelectionUseCase.getCurrent(new GetCurrentPlanSelectionQuery(user.getTelegramUserId())).selectionId())
                .isEqualTo(selected.selectionId());

        clock.setInstant(selected.expiresAt());
        assertThatThrownBy(() -> getCurrentPlanSelectionUseCase.getCurrent(new GetCurrentPlanSelectionQuery(user.getTelegramUserId())))
                .isInstanceOf(PlanSelectionNotFoundException.class);
        assertThat(planSelectionRepository.findById(selected.selectionId()).orElseThrow().getStatus())
                .isEqualTo(PlanSelectionStatus.EXPIRED);

        PlanSelectionResult reselected = selectPlanUseCase.select(new SelectPlanCommand(user.getTelegramUserId(), plan.getId()));
        assertThat(reselected.selectionId()).isNotEqualTo(selected.selectionId());
        assertThat(reselected.newlySelected()).isTrue();
        assertThat(activeSelectionCount(user.getId())).isEqualTo(1);
        assertThat(statusCount(user.getId(), PlanSelectionStatus.EXPIRED)).isEqualTo(1);
    }

    @Test
    void clearAtExpirationMarksSelectionExpiredAndReturnsNotFound() {
        User user = activeUser(21_002L);
        Plan plan = activePlan(PlanTestData.unlimitedPlan("CLEAR_EXPIRED", 1));
        PlanSelectionResult selected = selectPlanUseCase.select(new SelectPlanCommand(user.getTelegramUserId(), plan.getId()));

        clock.setInstant(selected.expiresAt());

        assertThatThrownBy(() -> clearPlanSelectionUseCase.clear(new ClearPlanSelectionCommand(user.getTelegramUserId())))
                .isInstanceOf(PlanSelectionNotFoundException.class);
        assertThat(planSelectionRepository.findById(selected.selectionId()).orElseThrow().getStatus())
                .isEqualTo(PlanSelectionStatus.EXPIRED);
        assertThat(activeSelectionCount(user.getId())).isZero();
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
        return statusCount(userId, PlanSelectionStatus.ACTIVE);
    }

    private long statusCount(UUID userId, PlanSelectionStatus status) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM plan_selections WHERE user_id = ? AND status = ?",
                Long.class,
                userId,
                status.name()
        );
        return count == null ? 0 : count;
    }
}
