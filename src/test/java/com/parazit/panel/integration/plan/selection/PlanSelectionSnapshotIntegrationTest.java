package com.parazit.panel.integration.plan.selection;

import com.parazit.panel.test.support.PostgreSqlContainerSupport;
import com.parazit.panel.test.support.DatabaseCleaner;

import static org.assertj.core.api.Assertions.assertThat;

import com.parazit.panel.application.plan.selection.command.SelectPlanCommand;
import com.parazit.panel.application.plan.selection.command.ClearPlanSelectionCommand;
import com.parazit.panel.application.plan.selection.query.GetCurrentPlanSelectionQuery;
import com.parazit.panel.application.plan.selection.result.PlanSelectionResult;
import com.parazit.panel.application.port.in.plan.selection.ClearPlanSelectionUseCase;
import com.parazit.panel.application.port.in.plan.selection.GetCurrentPlanSelectionUseCase;
import com.parazit.panel.application.port.in.plan.selection.SelectPlanUseCase;
import com.parazit.panel.application.plan.catalog.result.AvailablePlanResult;
import com.parazit.panel.application.plan.catalog.query.GetAvailablePlanByIdQuery;
import com.parazit.panel.application.port.in.plan.catalog.GetAvailablePlanUseCase;
import com.parazit.panel.domain.plan.CurrencyCode;
import com.parazit.panel.domain.plan.Plan;
import com.parazit.panel.domain.plan.PlanType;
import com.parazit.panel.domain.plan.repository.PlanRepository;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.UserLanguage;
import com.parazit.panel.domain.user.repository.UserRepository;
import com.parazit.panel.test.fixture.PlanTestData;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestConstructor;

@SpringBootTest(properties = {
        "spring.profiles.active=local",
        "spring.security.user.name=test",
        "spring.security.user.password=test"
})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class PlanSelectionSnapshotIntegrationTest extends PostgreSqlContainerSupport {

    private static final Instant NOW = Instant.parse("2026-07-11T00:00:00Z");

    private final SelectPlanUseCase selectPlanUseCase;
    private final ClearPlanSelectionUseCase clearPlanSelectionUseCase;
    private final GetCurrentPlanSelectionUseCase getCurrentPlanSelectionUseCase;
    private final GetAvailablePlanUseCase getAvailablePlanUseCase;
    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final JdbcTemplate jdbcTemplate;

    PlanSelectionSnapshotIntegrationTest(
            SelectPlanUseCase selectPlanUseCase,
            ClearPlanSelectionUseCase clearPlanSelectionUseCase,
            GetCurrentPlanSelectionUseCase getCurrentPlanSelectionUseCase,
            GetAvailablePlanUseCase getAvailablePlanUseCase,
            UserRepository userRepository,
            PlanRepository planRepository,
            JdbcTemplate jdbcTemplate
    ) {
        this.selectPlanUseCase = selectPlanUseCase;
        this.clearPlanSelectionUseCase = clearPlanSelectionUseCase;
        this.getCurrentPlanSelectionUseCase = getCurrentPlanSelectionUseCase;
        this.getAvailablePlanUseCase = getAvailablePlanUseCase;
        this.userRepository = userRepository;
        this.planRepository = planRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @BeforeEach
    void setUp() {
        DatabaseCleaner.cleanPlanSelectionTables(jdbcTemplate);
    }

    @Test
    void oldSelectionKeepsSnapshotAndNewSelectionUsesUpdatedPlanValues() {
        User user = userRepository.save(User.create(8001L, "snapshot_user", "Ali", null, UserLanguage.FA, NOW));
        Plan plan = activePlan(PlanTestData.trafficLimitedPlan("SNAPSHOT_ACTIVE", 1));

        PlanSelectionResult selected = selectPlanUseCase.select(new SelectPlanCommand(8001L, plan.getId()));

        plan.updateDetails("Updated Plan", null, PlanType.UNLIMITED, 1_000_000L, CurrencyCode.IRT, 60, null, null, 1);
        planRepository.save(plan);

        PlanSelectionResult current = getCurrentPlanSelectionUseCase.getCurrent(new GetCurrentPlanSelectionQuery(user.getTelegramUserId()));
        AvailablePlanResult catalog = getAvailablePlanUseCase.getById(new GetAvailablePlanByIdQuery(plan.getId()));

        assertThat(current.selectionId()).isEqualTo(selected.selectionId());
        assertThat(current.planName()).isEqualTo("Monthly 30 GiB");
        assertThat(current.planType()).isEqualTo(PlanType.TRAFFIC_LIMITED);
        assertThat(current.priceAmount()).isEqualTo(500_000L);
        assertThat(current.durationDays()).isEqualTo(30);
        assertThat(current.trafficLimitBytes()).isEqualTo(PlanTestData.THIRTY_GIB);
        assertThat(catalog.name()).isEqualTo("Updated Plan");
        assertThat(catalog.type()).isEqualTo(PlanType.UNLIMITED);
        assertThat(catalog.priceAmount()).isEqualTo(1_000_000L);
        assertThat(catalog.durationDays()).isEqualTo(60);

        clearPlanSelectionUseCase.clear(new ClearPlanSelectionCommand(user.getTelegramUserId()));
        PlanSelectionResult updatedSelection = selectPlanUseCase.select(new SelectPlanCommand(user.getTelegramUserId(), plan.getId()));

        assertThat(updatedSelection.selectionId()).isNotEqualTo(selected.selectionId());
        assertThat(updatedSelection.planName()).isEqualTo("Updated Plan");
        assertThat(updatedSelection.planType()).isEqualTo(PlanType.UNLIMITED);
        assertThat(updatedSelection.priceAmount()).isEqualTo(1_000_000L);
        assertThat(updatedSelection.durationDays()).isEqualTo(60);
        assertThat(updatedSelection.trafficLimitBytes()).isNull();
    }

    @Test
    void planStatusChangeAfterSelectionDoesNotChangeReadableSelectionSnapshot() {
        User user = userRepository.save(User.create(8002L, "status_snapshot_user", "Ali", null, UserLanguage.FA, NOW));
        Plan plan = activePlan(PlanTestData.unlimitedPlan("STATUS_SNAPSHOT", 1));
        PlanSelectionResult selected = selectPlanUseCase.select(new SelectPlanCommand(user.getTelegramUserId(), plan.getId()));

        plan.archive();
        planRepository.save(plan);

        PlanSelectionResult current = getCurrentPlanSelectionUseCase.getCurrent(new GetCurrentPlanSelectionQuery(user.getTelegramUserId()));

        assertThat(current.selectionId()).isEqualTo(selected.selectionId());
        assertThat(current.planCode()).isEqualTo("STATUS_SNAPSHOT");
        assertThat(current.planName()).isEqualTo("Monthly Unlimited");
        assertThat(current.priceAmount()).isEqualTo(900_000L);
    }

    private Plan activePlan(Plan plan) {
        Plan saved = planRepository.save(plan);
        saved.activate();
        return planRepository.save(saved);
    }
}
