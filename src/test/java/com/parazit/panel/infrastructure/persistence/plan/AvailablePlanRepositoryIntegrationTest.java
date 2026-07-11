package com.parazit.panel.infrastructure.persistence.plan;

import com.parazit.panel.test.support.PostgreSqlContainerSupport;
import com.parazit.panel.test.support.DatabaseCleaner;

import static org.assertj.core.api.Assertions.assertThat;

import com.parazit.panel.config.persistence.JpaAuditingConfiguration;
import com.parazit.panel.domain.plan.Plan;
import com.parazit.panel.domain.plan.PlanStatus;
import com.parazit.panel.domain.plan.PlanType;
import com.parazit.panel.domain.plan.repository.PlanRepository;
import com.parazit.panel.test.fixture.PlanTestData;
import java.util.Arrays;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestConstructor;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EntityScan(basePackageClasses = Plan.class)
@EnableJpaRepositories(basePackageClasses = SpringDataPlanRepository.class)
@Import({JpaAuditingConfiguration.class, PlanRepositoryAdapter.class})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class AvailablePlanRepositoryIntegrationTest extends PostgreSqlContainerSupport {

    private final PlanRepository repository;
    private final JdbcTemplate jdbcTemplate;
    private final Flyway flyway;

    AvailablePlanRepositoryIntegrationTest(PlanRepository repository, JdbcTemplate jdbcTemplate, Flyway flyway) {
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
        this.flyway = flyway;
    }

    @BeforeEach
    void setUp() {
        DatabaseCleaner.cleanPlanTables(jdbcTemplate);
    }

    @Test
    void findsActivePlansOnlyWithDeterministicOrderingAndTypeFilter() {
        Plan activeLimitedB = repository.save(PlanTestData.trafficLimitedPlan("B_LIMITED_ACTIVE", 2));
        activeLimitedB.activate();
        repository.save(activeLimitedB);
        Plan activeLimitedA = repository.save(PlanTestData.trafficLimitedPlan("A_LIMITED_ACTIVE", 1));
        activeLimitedA.activate();
        repository.save(activeLimitedA);
        Plan activeUnlimited = repository.save(PlanTestData.unlimitedPlan("C_UNLIMITED_ACTIVE", 3));
        activeUnlimited.activate();
        repository.save(activeUnlimited);
        repository.save(PlanTestData.trafficLimitedPlan("DRAFT_HIDDEN", 0));
        Plan inactive = repository.save(PlanTestData.unlimitedPlan("INACTIVE_HIDDEN", 0));
        inactive.activate();
        inactive.deactivate();
        repository.save(inactive);
        Plan archived = repository.save(PlanTestData.unlimitedPlan("ARCHIVED_HIDDEN", 0));
        archived.archive();
        repository.save(archived);

        assertThat(repository.findAllByStatusOrderByDisplayOrderAscCodeAsc(PlanStatus.ACTIVE))
                .extracting(Plan::getCode)
                .containsExactly("A_LIMITED_ACTIVE", "B_LIMITED_ACTIVE", "C_UNLIMITED_ACTIVE");
        assertThat(repository.findAllByStatusAndTypeOrderByDisplayOrderAscCodeAsc(PlanStatus.ACTIVE, PlanType.TRAFFIC_LIMITED))
                .extracting(Plan::getCode)
                .containsExactly("A_LIMITED_ACTIVE", "B_LIMITED_ACTIVE");
        assertThat(repository.findAllByStatusAndTypeOrderByDisplayOrderAscCodeAsc(PlanStatus.ACTIVE, PlanType.UNLIMITED))
                .extracting(Plan::getCode)
                .containsExactly("C_UNLIMITED_ACTIVE");
    }

    @Test
    void directStatusConstrainedLookupsReturnOnlyActivePlans() {
        Plan active = repository.save(PlanTestData.unlimitedPlan("ACTIVE_LOOKUP", 1));
        active.activate();
        repository.save(active);
        Plan draft = repository.save(PlanTestData.unlimitedPlan("DRAFT_LOOKUP", 2));
        Plan inactive = repository.save(PlanTestData.unlimitedPlan("INACTIVE_LOOKUP", 3));
        inactive.activate();
        inactive.deactivate();
        repository.save(inactive);
        Plan archived = repository.save(PlanTestData.unlimitedPlan("ARCHIVED_LOOKUP", 4));
        archived.archive();
        repository.save(archived);

        assertThat(repository.findByIdAndStatus(active.getId(), PlanStatus.ACTIVE)).contains(active);
        assertThat(repository.findByCodeAndStatus("active_lookup", PlanStatus.ACTIVE)).contains(active);
        assertThat(repository.findByIdAndStatus(draft.getId(), PlanStatus.ACTIVE)).isEmpty();
        assertThat(repository.findByCodeAndStatus("inactive_lookup", PlanStatus.ACTIVE)).isEmpty();
        assertThat(repository.findByIdAndStatus(archived.getId(), PlanStatus.ACTIVE)).isEmpty();
        assertThat(repository.findByCodeAndStatus("MISSING_LOOKUP", PlanStatus.ACTIVE)).isEmpty();
    }

    @Test
    void flywayMigrationRunsAndHibernateValidationSucceeds() {
        assertThat(flyway.info().current()).isNotNull();
        assertThat(Arrays.stream(flyway.info().applied()))
                .anySatisfy(info -> assertThat(info.getVersion().getVersion()).isEqualTo("5"));
    }
}
