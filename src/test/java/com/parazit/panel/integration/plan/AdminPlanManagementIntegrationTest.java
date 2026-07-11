package com.parazit.panel.integration.plan;

import com.parazit.panel.test.support.PostgreSqlContainerSupport;
import com.parazit.panel.test.support.DatabaseCleaner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parazit.panel.application.plan.admin.PlanCodeAlreadyExistsException;
import com.parazit.panel.application.plan.admin.PlanModificationNotAllowedException;
import com.parazit.panel.application.plan.admin.command.ChangePlanStatusCommand;
import com.parazit.panel.application.plan.admin.command.CreatePlanCommand;
import com.parazit.panel.application.plan.admin.command.UpdatePlanCommand;
import com.parazit.panel.application.plan.admin.query.GetPlanByCodeQuery;
import com.parazit.panel.application.plan.admin.query.GetPlanByIdQuery;
import com.parazit.panel.application.plan.admin.query.ListPlansQuery;
import com.parazit.panel.application.plan.result.PlanResult;
import com.parazit.panel.application.port.in.plan.admin.ChangePlanStatusUseCase;
import com.parazit.panel.application.port.in.plan.admin.CreatePlanUseCase;
import com.parazit.panel.application.port.in.plan.admin.GetPlanUseCase;
import com.parazit.panel.application.port.in.plan.admin.ListPlansUseCase;
import com.parazit.panel.application.port.in.plan.admin.UpdatePlanUseCase;
import com.parazit.panel.domain.plan.CurrencyCode;
import com.parazit.panel.domain.plan.PlanStatus;
import com.parazit.panel.domain.plan.PlanType;
import com.parazit.panel.domain.plan.repository.PlanRepository;
import com.parazit.panel.test.fixture.PlanTestData;
import java.util.Arrays;
import org.flywaydb.core.Flyway;
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
class AdminPlanManagementIntegrationTest extends PostgreSqlContainerSupport {

    private final CreatePlanUseCase createPlanUseCase;
    private final GetPlanUseCase getPlanUseCase;
    private final ListPlansUseCase listPlansUseCase;
    private final UpdatePlanUseCase updatePlanUseCase;
    private final ChangePlanStatusUseCase changePlanStatusUseCase;
    private final PlanRepository planRepository;
    private final JdbcTemplate jdbcTemplate;
    private final Flyway flyway;

    AdminPlanManagementIntegrationTest(
            CreatePlanUseCase createPlanUseCase,
            GetPlanUseCase getPlanUseCase,
            ListPlansUseCase listPlansUseCase,
            UpdatePlanUseCase updatePlanUseCase,
            ChangePlanStatusUseCase changePlanStatusUseCase,
            PlanRepository planRepository,
            JdbcTemplate jdbcTemplate,
            Flyway flyway
    ) {
        this.createPlanUseCase = createPlanUseCase;
        this.getPlanUseCase = getPlanUseCase;
        this.listPlansUseCase = listPlansUseCase;
        this.updatePlanUseCase = updatePlanUseCase;
        this.changePlanStatusUseCase = changePlanStatusUseCase;
        this.planRepository = planRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.flyway = flyway;
    }

    @BeforeEach
    void setUp() {
        DatabaseCleaner.cleanPlanTables(jdbcTemplate);
    }

    @Test
    void createGetListUpdateAndLifecycleOperationsPersist() {
        PlanResult limited = create(limitedCommand("A_LIMITED", 1));
        PlanResult unlimited = create(unlimitedCommand("B_UNLIMITED", 2));

        assertThat(limited.id()).isNotNull();
        assertThat(limited.createdAt()).isNotNull();
        assertThat(limited.updatedAt()).isNotNull();
        assertThat(limited.status()).isEqualTo(PlanStatus.DRAFT);
        assertThat(limited.trafficLimitBytes()).isEqualTo(PlanTestData.THIRTY_GIB);
        assertThat(unlimited.trafficLimitBytes()).isNull();

        assertThat(getPlanUseCase.getById(new GetPlanByIdQuery(limited.id())).code()).isEqualTo("A_LIMITED");
        assertThat(getPlanUseCase.getByCode(new GetPlanByCodeQuery("a_limited")).id()).isEqualTo(limited.id());
        assertThat(listPlansUseCase.list(new ListPlansQuery(null, null)))
                .extracting(PlanResult::code)
                .containsExactly("A_LIMITED", "B_UNLIMITED");
        assertThat(listPlansUseCase.list(new ListPlansQuery(PlanStatus.DRAFT, null)))
                .extracting(PlanResult::code)
                .containsExactly("A_LIMITED", "B_UNLIMITED");
        assertThat(listPlansUseCase.list(new ListPlansQuery(null, PlanType.UNLIMITED)))
                .extracting(PlanResult::code)
                .containsExactly("B_UNLIMITED");
        assertThat(listPlansUseCase.list(new ListPlansQuery(PlanStatus.DRAFT, PlanType.TRAFFIC_LIMITED)))
                .extracting(PlanResult::code)
                .containsExactly("A_LIMITED");

        PlanResult updated = updatePlanUseCase.update(new UpdatePlanCommand(
                limited.id(),
                "Updated Unlimited",
                "Updated",
                PlanType.UNLIMITED,
                700_000L,
                CurrencyCode.IRT,
                60,
                null,
                null,
                3
        ));
        assertThat(updated.code()).isEqualTo("A_LIMITED");
        assertThat(updated.type()).isEqualTo(PlanType.UNLIMITED);
        assertThat(updated.trafficLimitBytes()).isNull();
        assertThat(planRepository.findByCode("A_LIMITED")).isPresent();

        assertThat(changePlanStatusUseCase.activate(new ChangePlanStatusCommand(limited.id())).status())
                .isEqualTo(PlanStatus.ACTIVE);
        assertThat(changePlanStatusUseCase.deactivate(new ChangePlanStatusCommand(limited.id())).status())
                .isEqualTo(PlanStatus.INACTIVE);
        assertThat(changePlanStatusUseCase.activate(new ChangePlanStatusCommand(limited.id())).available())
                .isTrue();
        assertThat(changePlanStatusUseCase.archive(new ChangePlanStatusCommand(limited.id())).status())
                .isEqualTo(PlanStatus.ARCHIVED);
        assertThatThrownBy(() -> updatePlanUseCase.update(new UpdatePlanCommand(
                limited.id(),
                "Archived Update",
                null,
                PlanType.UNLIMITED,
                0,
                CurrencyCode.IRT,
                30,
                null,
                null,
                1
        )))
                .isInstanceOf(PlanModificationNotAllowedException.class);
    }

    @Test
    void duplicateAndNormalizedDuplicateCodesAreRejected() {
        create(unlimitedCommand("DUPLICATE_PLAN", 1));

        assertThatThrownBy(() -> create(unlimitedCommand("DUPLICATE_PLAN", 2)))
                .isInstanceOf(PlanCodeAlreadyExistsException.class);
        assertThatThrownBy(() -> create(unlimitedCommand(" duplicate_plan ", 3)))
                .isInstanceOf(PlanCodeAlreadyExistsException.class);
        assertThat(planRowCount()).isEqualTo(1);
    }

    @Test
    void flywayMigrationRunsAndHibernateValidationSucceeds() {
        assertThat(flyway.info().current()).isNotNull();
        assertThat(Arrays.stream(flyway.info().applied()))
                .anySatisfy(info -> assertThat(info.getVersion().getVersion()).isEqualTo("5"));
    }

    private PlanResult create(CreatePlanCommand command) {
        return createPlanUseCase.create(command);
    }

    private CreatePlanCommand limitedCommand(String code, int displayOrder) {
        return new CreatePlanCommand(
                code,
                "Limited",
                "Limited plan",
                PlanType.TRAFFIC_LIMITED,
                500_000L,
                CurrencyCode.IRT,
                30,
                PlanTestData.THIRTY_GIB,
                2,
                displayOrder
        );
    }

    private CreatePlanCommand unlimitedCommand(String code, int displayOrder) {
        return new CreatePlanCommand(
                code,
                "Unlimited",
                null,
                PlanType.UNLIMITED,
                900_000L,
                CurrencyCode.IRT,
                30,
                null,
                null,
                displayOrder
        );
    }

    private long planRowCount() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM plans", Long.class);
        return count == null ? 0 : count;
    }
}
