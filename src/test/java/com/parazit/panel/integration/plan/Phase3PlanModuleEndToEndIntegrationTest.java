package com.parazit.panel.integration.plan;

import com.parazit.panel.test.support.PostgreSqlContainerSupport;
import com.parazit.panel.test.support.DatabaseCleaner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parazit.panel.application.plan.admin.PlanModificationNotAllowedException;
import com.parazit.panel.application.plan.admin.command.ChangePlanStatusCommand;
import com.parazit.panel.application.plan.admin.command.CreatePlanCommand;
import com.parazit.panel.application.plan.admin.command.UpdatePlanCommand;
import com.parazit.panel.application.plan.admin.query.GetPlanByIdQuery;
import com.parazit.panel.application.plan.catalog.AvailablePlanNotFoundException;
import com.parazit.panel.application.plan.catalog.query.ListAvailablePlansQuery;
import com.parazit.panel.application.plan.catalog.result.AvailablePlanResult;
import com.parazit.panel.application.plan.result.PlanResult;
import com.parazit.panel.application.plan.selection.command.ClearPlanSelectionCommand;
import com.parazit.panel.application.plan.selection.command.SelectPlanCommand;
import com.parazit.panel.application.plan.selection.query.GetCurrentPlanSelectionQuery;
import com.parazit.panel.application.plan.selection.result.PlanSelectionResult;
import com.parazit.panel.application.port.in.plan.admin.ChangePlanStatusUseCase;
import com.parazit.panel.application.port.in.plan.admin.CreatePlanUseCase;
import com.parazit.panel.application.port.in.plan.admin.GetPlanUseCase;
import com.parazit.panel.application.port.in.plan.admin.UpdatePlanUseCase;
import com.parazit.panel.application.port.in.plan.catalog.ListAvailablePlansUseCase;
import com.parazit.panel.application.port.in.plan.selection.ClearPlanSelectionUseCase;
import com.parazit.panel.application.port.in.plan.selection.GetCurrentPlanSelectionUseCase;
import com.parazit.panel.application.port.in.plan.selection.SelectPlanUseCase;
import com.parazit.panel.application.user.command.RegisterUserCommand;
import com.parazit.panel.application.user.result.RegisterUserResult;
import com.parazit.panel.application.port.in.user.RegisterUserUseCase;
import com.parazit.panel.domain.plan.CurrencyCode;
import com.parazit.panel.domain.plan.PlanStatus;
import com.parazit.panel.domain.plan.PlanType;
import com.parazit.panel.test.fixture.PlanTestData;
import java.util.Set;
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
class Phase3PlanModuleEndToEndIntegrationTest extends PostgreSqlContainerSupport {

    private final RegisterUserUseCase registerUserUseCase;
    private final CreatePlanUseCase createPlanUseCase;
    private final ChangePlanStatusUseCase changePlanStatusUseCase;
    private final GetPlanUseCase getPlanUseCase;
    private final UpdatePlanUseCase updatePlanUseCase;
    private final ListAvailablePlansUseCase listAvailablePlansUseCase;
    private final SelectPlanUseCase selectPlanUseCase;
    private final GetCurrentPlanSelectionUseCase getCurrentPlanSelectionUseCase;
    private final ClearPlanSelectionUseCase clearPlanSelectionUseCase;
    private final JdbcTemplate jdbcTemplate;

    Phase3PlanModuleEndToEndIntegrationTest(
            RegisterUserUseCase registerUserUseCase,
            CreatePlanUseCase createPlanUseCase,
            ChangePlanStatusUseCase changePlanStatusUseCase,
            GetPlanUseCase getPlanUseCase,
            UpdatePlanUseCase updatePlanUseCase,
            ListAvailablePlansUseCase listAvailablePlansUseCase,
            SelectPlanUseCase selectPlanUseCase,
            GetCurrentPlanSelectionUseCase getCurrentPlanSelectionUseCase,
            ClearPlanSelectionUseCase clearPlanSelectionUseCase,
            JdbcTemplate jdbcTemplate
    ) {
        this.registerUserUseCase = registerUserUseCase;
        this.createPlanUseCase = createPlanUseCase;
        this.changePlanStatusUseCase = changePlanStatusUseCase;
        this.getPlanUseCase = getPlanUseCase;
        this.updatePlanUseCase = updatePlanUseCase;
        this.listAvailablePlansUseCase = listAvailablePlansUseCase;
        this.selectPlanUseCase = selectPlanUseCase;
        this.getCurrentPlanSelectionUseCase = getCurrentPlanSelectionUseCase;
        this.clearPlanSelectionUseCase = clearPlanSelectionUseCase;
        this.jdbcTemplate = jdbcTemplate;
    }

    @BeforeEach
    void setUp() {
        DatabaseCleaner.cleanPlanSelectionTables(jdbcTemplate);
    }

    @Test
    void completePhaseThreePlanFlowPreservesVisibilitySnapshotsAndSelectionRules() {
        RegisterUserResult user = registerUserUseCase.register(new RegisterUserCommand(
                20_001L,
                "phase3_user",
                "Ali",
                "Ahmadi",
                "en"
        ));
        PlanResult draftPlan = createPlanUseCase.create(new CreatePlanCommand(
                "PHASE3_LIMITED",
                "Phase 3 Limited A1",
                "Phase 3 quality gate",
                PlanType.TRAFFIC_LIMITED,
                500_000L,
                CurrencyCode.IRT,
                30,
                PlanTestData.THIRTY_GIB,
                2,
                1
        ));

        assertThat(listAvailablePlansUseCase.list(new ListAvailablePlansQuery(null))).isEmpty();

        PlanResult activePlan = changePlanStatusUseCase.activate(new ChangePlanStatusCommand(draftPlan.id()));
        assertThat(activePlan.status()).isEqualTo(PlanStatus.ACTIVE);
        assertThat(listAvailablePlansUseCase.list(new ListAvailablePlansQuery(null)))
                .extracting(AvailablePlanResult::code)
                .containsExactly("PHASE3_LIMITED");

        PlanSelectionResult firstSelection = selectPlanUseCase.select(new SelectPlanCommand(user.telegramUserId(), activePlan.id()));
        assertThat(firstSelection.planName()).isEqualTo("Phase 3 Limited A1");
        assertThat(firstSelection.priceAmount()).isEqualTo(500_000L);
        assertThat(firstSelection.durationDays()).isEqualTo(30);
        assertThat(firstSelection.trafficLimitBytes()).isEqualTo(PlanTestData.THIRTY_GIB);
        assertThat(firstSelection.maxDevices()).isEqualTo(2);

        PlanResult updatedPlan = updatePlanUseCase.update(new UpdatePlanCommand(
                activePlan.id(),
                "Phase 3 Limited A2",
                "Updated for quality gate",
                PlanType.TRAFFIC_LIMITED,
                750_000L,
                CurrencyCode.IRT,
                45,
                50L * PlanTestData.GIB,
                3,
                1
        ));
        assertThat(listAvailablePlansUseCase.list(new ListAvailablePlansQuery(null)).getFirst().name())
                .isEqualTo("Phase 3 Limited A2");

        PlanSelectionResult oldSnapshot = getCurrentPlanSelectionUseCase.getCurrent(new GetCurrentPlanSelectionQuery(user.telegramUserId()));
        assertThat(oldSnapshot.selectionId()).isEqualTo(firstSelection.selectionId());
        assertThat(oldSnapshot.planName()).isEqualTo("Phase 3 Limited A1");
        assertThat(oldSnapshot.priceAmount()).isEqualTo(500_000L);
        assertThat(oldSnapshot.durationDays()).isEqualTo(30);
        assertThat(oldSnapshot.maxDevices()).isEqualTo(2);

        clearPlanSelectionUseCase.clear(new ClearPlanSelectionCommand(user.telegramUserId()));
        PlanSelectionResult updatedSnapshot = selectPlanUseCase.select(new SelectPlanCommand(user.telegramUserId(), updatedPlan.id()));
        assertThat(updatedSnapshot.selectionId()).isNotEqualTo(firstSelection.selectionId());
        assertThat(updatedSnapshot.planName()).isEqualTo("Phase 3 Limited A2");
        assertThat(updatedSnapshot.priceAmount()).isEqualTo(750_000L);
        assertThat(updatedSnapshot.durationDays()).isEqualTo(45);
        assertThat(updatedSnapshot.trafficLimitBytes()).isEqualTo(50L * PlanTestData.GIB);
        assertThat(updatedSnapshot.maxDevices()).isEqualTo(3);
        assertThat(activeSelectionCount()).isEqualTo(1);

        changePlanStatusUseCase.deactivate(new ChangePlanStatusCommand(updatedPlan.id()));
        assertThat(listAvailablePlansUseCase.list(new ListAvailablePlansQuery(null))).isEmpty();
        assertThatThrownBy(() -> selectPlanUseCase.select(new SelectPlanCommand(user.telegramUserId(), updatedPlan.id())))
                .isInstanceOf(AvailablePlanNotFoundException.class);
        assertThat(getCurrentPlanSelectionUseCase.getCurrent(new GetCurrentPlanSelectionQuery(user.telegramUserId())).selectionId())
                .isEqualTo(updatedSnapshot.selectionId());

        PlanResult archived = changePlanStatusUseCase.archive(new ChangePlanStatusCommand(updatedPlan.id()));
        assertThat(getPlanUseCase.getById(new GetPlanByIdQuery(updatedPlan.id())).status()).isEqualTo(PlanStatus.ARCHIVED);
        assertThat(archived.status()).isEqualTo(PlanStatus.ARCHIVED);
        assertThatThrownBy(() -> updatePlanUseCase.update(new UpdatePlanCommand(
                updatedPlan.id(),
                "Should fail",
                null,
                PlanType.UNLIMITED,
                1,
                CurrencyCode.IRT,
                30,
                null,
                null,
                1
        ))).isInstanceOf(PlanModificationNotAllowedException.class);

        assertThat(existingStillDeferredOperationalTables()).isEmpty();
    }

    private long activeSelectionCount() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM plan_selections WHERE status = 'ACTIVE'",
                Long.class
        );
        return count == null ? 0 : count;
    }

    private Set<String> existingStillDeferredOperationalTables() {
        return Set.copyOf(jdbcTemplate.queryForList("""
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name IN ('vpn_clients')
                """, String.class));
    }
}
