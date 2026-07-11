package com.parazit.panel.infrastructure.persistence.plan;

import com.parazit.panel.test.support.PostgreSqlContainerSupport;
import com.parazit.panel.test.support.DatabaseCleaner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parazit.panel.config.persistence.JpaAuditingConfiguration;
import com.parazit.panel.domain.plan.CurrencyCode;
import com.parazit.panel.domain.plan.Plan;
import com.parazit.panel.domain.plan.PlanStatus;
import com.parazit.panel.domain.plan.PlanType;
import com.parazit.panel.domain.plan.repository.PlanRepository;
import com.parazit.panel.test.fixture.PlanTestData;
import com.parazit.panel.test.support.MutableClockTestConfiguration;
import com.parazit.panel.test.support.MutableTestClock;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestConstructor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EntityScan(basePackageClasses = Plan.class)
@EnableJpaRepositories(basePackageClasses = SpringDataPlanRepository.class)
@Import({JpaAuditingConfiguration.class, PlanRepositoryAdapter.class, MutableClockTestConfiguration.class})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class PlanRepositoryIntegrationTest extends PostgreSqlContainerSupport {

    private static final Timestamp TEST_TIMESTAMP = Timestamp.from(Instant.parse("2026-07-11T00:00:00Z"));

    private final PlanRepository repository;
    private final EntityManager entityManager;
    private final Flyway flyway;
    private final MutableTestClock clock;
    private final JdbcTemplate jdbcTemplate;

    PlanRepositoryIntegrationTest(
            PlanRepository repository,
            EntityManager entityManager,
            Flyway flyway,
            Clock clock,
            JdbcTemplate jdbcTemplate
    ) {
        this.repository = repository;
        this.entityManager = entityManager;
        this.flyway = flyway;
        this.clock = (MutableTestClock) clock;
        this.jdbcTemplate = jdbcTemplate;
    }

    @BeforeEach
    void setUp() {
        DatabaseCleaner.cleanPlanTables(jdbcTemplate);
        entityManager.clear();
    }

    @Test
    void savesPlanAndFindsByCodeWithNormalizedLookup() {
        Plan saved = repository.save(PlanTestData.trafficLimitedPlan(" monthly_30gb ", 1));
        entityManager.flush();

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();

        entityManager.clear();
        Plan found = repository.findByCode(" monthly_30gb ").orElseThrow();

        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getCode()).isEqualTo("MONTHLY_30GB");
        assertThat(found.getName()).isEqualTo("Monthly 30 GiB");
        assertThat(found.getDescription()).isEqualTo("30 GiB plan");
        assertThat(found.getStatus()).isEqualTo(PlanStatus.DRAFT);
        assertThat(found.getType()).isEqualTo(PlanType.TRAFFIC_LIMITED);
        assertThat(found.getPriceAmount()).isEqualTo(500_000L);
        assertThat(found.getCurrency()).isEqualTo(CurrencyCode.IRT);
        assertThat(found.getDurationDays()).isEqualTo(30);
        assertThat(found.getTrafficLimitBytes()).isEqualTo(PlanTestData.THIRTY_GIB);
        assertThat(found.getMaxDevices()).isEqualTo(2);
        assertThat(found.getDisplayOrder()).isEqualTo(1);
        assertThat(repository.existsByCode("monthly_30gb")).isTrue();
    }

    @Test
    void persistsNullableAndNonNullableMaxDevices() {
        repository.save(PlanTestData.unlimitedPlan("UNLIMITED_NO_LIMIT", 1));
        repository.save(PlanTestData.trafficLimitedPlan("LIMITED_TWO_DEVICES", 2));
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findByCode("UNLIMITED_NO_LIMIT").orElseThrow().getMaxDevices()).isNull();
        assertThat(repository.findByCode("LIMITED_TWO_DEVICES").orElseThrow().getMaxDevices()).isEqualTo(2);
    }

    @Test
    void queriesByStatusAndOrdersByDisplayOrderThenCode() {
        Plan second = PlanTestData.unlimitedPlan("B_UNLIMITED", 2);
        Plan inactive = PlanTestData.unlimitedPlan("INACTIVE_PLAN", 0);
        inactive.activate();
        inactive.deactivate();
        Plan firstByCode = PlanTestData.trafficLimitedPlan("A_LIMITED", 1);
        Plan secondByCode = PlanTestData.trafficLimitedPlan("C_LIMITED", 1);

        repository.save(second);
        repository.save(inactive);
        repository.save(secondByCode);
        repository.save(firstByCode);
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findAllByStatusOrderByDisplayOrderAscCodeAsc(PlanStatus.DRAFT))
                .extracting(Plan::getCode)
                .containsExactly("A_LIMITED", "C_LIMITED", "B_UNLIMITED");
        assertThat(repository.findAllOrderByDisplayOrderAscCodeAsc())
                .extracting(Plan::getCode)
                .containsExactly("INACTIVE_PLAN", "A_LIMITED", "C_LIMITED", "B_UNLIMITED");
    }

    @Test
    void updatePersistsAndUpdatedAtChanges() {
        clock.setInstant(MutableClockTestConfiguration.DEFAULT_INSTANT);
        Plan saved = repository.save(PlanTestData.trafficLimitedPlan("UPDATABLE_PLAN", 1));
        entityManager.flush();
        entityManager.clear();

        Plan persisted = repository.findByCode("UPDATABLE_PLAN").orElseThrow();
        Instant createdAt = persisted.getCreatedAt();
        Instant updatedAt = persisted.getUpdatedAt();

        clock.setInstant(MutableClockTestConfiguration.DEFAULT_INSTANT.plusSeconds(60));
        persisted.updateDetails("Updated", "Updated details", PlanType.UNLIMITED, 700_000L, CurrencyCode.IRT, 60, null, null, 4);
        persisted.activate();
        repository.save(persisted);
        entityManager.flush();
        entityManager.clear();

        Plan found = repository.findByCode("UPDATABLE_PLAN").orElseThrow();
        assertThat(found.getCode()).isEqualTo("UPDATABLE_PLAN");
        assertThat(found.getName()).isEqualTo("Updated");
        assertThat(found.getDescription()).isEqualTo("Updated details");
        assertThat(found.getType()).isEqualTo(PlanType.UNLIMITED);
        assertThat(found.getPriceAmount()).isEqualTo(700_000L);
        assertThat(found.getCurrency()).isEqualTo(CurrencyCode.IRT);
        assertThat(found.getDurationDays()).isEqualTo(60);
        assertThat(found.getTrafficLimitBytes()).isNull();
        assertThat(found.getMaxDevices()).isNull();
        assertThat(found.getDisplayOrder()).isEqualTo(4);
        assertThat(found.getStatus()).isEqualTo(PlanStatus.ACTIVE);
        assertThat(found.getCreatedAt()).isEqualTo(createdAt);
        assertThat(found.getUpdatedAt()).isAfter(updatedAt);
    }

    @Test
    void enforcesUniqueCodeConstraint() {
        repository.save(PlanTestData.unlimitedPlan("DUPLICATE_PLAN", 1));
        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(() -> repository.save(PlanTestData.trafficLimitedPlan("duplicate_plan", 2)))
                .isInstanceOfAny(DataIntegrityViolationException.class, PersistenceException.class);
    }

    @Test
    void enforcesTrafficLimitedPlanRequiresTrafficLimitConstraint() {
        assertThatThrownBy(() -> insertPlan("TRAFFIC_INVALID", "TRAFFIC_LIMITED", "NULL", "30", "500000", "0", "NULL"))
                .isInstanceOf(PersistenceException.class);
    }

    @Test
    void enforcesUnlimitedPlanRequiresNullTrafficLimitConstraint() {
        assertThatThrownBy(() -> insertPlan("UNLIMITED_INVALID", "UNLIMITED", "32212254720", "30", "500000", "0", "NULL"))
                .isInstanceOf(PersistenceException.class);
    }

    @Test
    void enforcesDurationConstraint() {
        assertThatThrownBy(() -> insertPlan("BAD_DURATION", "UNLIMITED", "NULL", "0", "500000", "0", "NULL"))
                .isInstanceOf(PersistenceException.class);
    }

    @Test
    void enforcesPriceConstraint() {
        assertThatThrownBy(() -> insertPlan("BAD_PRICE", "UNLIMITED", "NULL", "30", "-1", "0", "NULL"))
                .isInstanceOf(PersistenceException.class);
    }

    @Test
    void enforcesDisplayOrderConstraint() {
        assertThatThrownBy(() -> insertPlan("BAD_DISPLAY", "UNLIMITED", "NULL", "30", "500000", "-1", "NULL"))
                .isInstanceOf(PersistenceException.class);
    }

    @Test
    void enforcesMaxDevicesConstraint() {
        assertThatThrownBy(() -> insertPlan("BAD_DEVICES", "UNLIMITED", "NULL", "30", "500000", "0", "0"))
                .isInstanceOf(PersistenceException.class);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void enforcesStatusTypeAndCurrencyConstraints() {
        assertThatThrownBy(() -> insertRawPlan("BAD_STATUS", "BAD", "UNLIMITED", "IRT", TEST_TIMESTAMP, TEST_TIMESTAMP))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertRawPlan("BAD_TYPE", "DRAFT", "BAD", "IRT", TEST_TIMESTAMP, TEST_TIMESTAMP))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertRawPlan("BAD_CURRENCY", "DRAFT", "UNLIMITED", "USD", TEST_TIMESTAMP, TEST_TIMESTAMP))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void enforcesRequiredAuditTimestamps() {
        assertThatThrownBy(() -> insertRawPlan("NO_CREATED_AT", "DRAFT", "UNLIMITED", "IRT", null, TEST_TIMESTAMP))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertRawPlan("NO_UPDATED_AT", "DRAFT", "UNLIMITED", "IRT", TEST_TIMESTAMP, null))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void flywayMigrationRunsAndHibernateValidationSucceeds() {
        assertThat(flyway.info().current()).isNotNull();
        assertThat(Arrays.stream(flyway.info().applied()))
                .anySatisfy(info -> assertThat(info.getVersion().getVersion()).isEqualTo("5"));
    }

    private void insertPlan(
            String code,
            String type,
            String trafficLimitBytes,
            String durationDays,
            String priceAmount,
            String displayOrder,
            String maxDevices
    ) {
        entityManager.createNativeQuery("""
                INSERT INTO plans (
                    id,
                    code,
                    name,
                    status,
                    type,
                    price_amount,
                    currency,
                    duration_days,
                    traffic_limit_bytes,
                    max_devices,
                    display_order,
                    created_at,
                    updated_at
                )
                VALUES (
                    '%s',
                    '%s',
                    'Invalid Plan',
                    'DRAFT',
                    '%s',
                    %s,
                    'IRT',
                    %s,
                    %s,
                    %s,
                    %s,
                    now(),
                    now()
                )
                """.formatted(UUID.randomUUID(), code, type, priceAmount, durationDays, trafficLimitBytes, maxDevices, displayOrder))
                .executeUpdate();
        entityManager.flush();
    }

    private void insertRawPlan(
            String code,
            String status,
            String type,
            String currency,
            Timestamp createdAt,
            Timestamp updatedAt
    ) {
        jdbcTemplate.update("""
                INSERT INTO plans (
                    id, code, name, status, type, price_amount, currency, duration_days,
                    traffic_limit_bytes, max_devices, display_order, created_at, updated_at
                )
                VALUES (?, ?, 'Invalid Plan', ?, ?, 500000, ?, 30, NULL, NULL, 0, ?, ?)
                """, UUID.randomUUID(), code, status, type, currency, createdAt, updatedAt);
    }
}
