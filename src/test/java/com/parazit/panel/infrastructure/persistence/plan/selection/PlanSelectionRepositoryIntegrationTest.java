package com.parazit.panel.infrastructure.persistence.plan.selection;

import com.parazit.panel.test.support.PostgreSqlContainerSupport;
import com.parazit.panel.test.support.DatabaseCleaner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parazit.panel.config.persistence.JpaAuditingConfiguration;
import com.parazit.panel.domain.plan.CurrencyCode;
import com.parazit.panel.domain.plan.Plan;
import com.parazit.panel.domain.plan.PlanType;
import com.parazit.panel.domain.plan.selection.PlanSelection;
import com.parazit.panel.domain.plan.selection.PlanSelectionStatus;
import com.parazit.panel.domain.plan.selection.repository.PlanSelectionRepository;
import com.parazit.panel.test.fixture.PlanTestData;
import com.parazit.panel.test.support.MutableClockTestConfiguration;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.sql.Timestamp;
import java.time.Duration;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EntityScan(basePackageClasses = PlanSelection.class)
@EnableJpaRepositories(basePackageClasses = SpringDataPlanSelectionRepository.class)
@Import({JpaAuditingConfiguration.class, PlanSelectionRepositoryAdapter.class, MutableClockTestConfiguration.class})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class PlanSelectionRepositoryIntegrationTest extends PostgreSqlContainerSupport {

    private static final Instant NOW = Instant.parse("2026-07-11T00:00:00Z");
    private static final Duration TTL = Duration.ofMinutes(30);

    private final PlanSelectionRepository repository;
    private final EntityManager entityManager;
    private final JdbcTemplate jdbcTemplate;
    private final Flyway flyway;

    PlanSelectionRepositoryIntegrationTest(
            PlanSelectionRepository repository,
            EntityManager entityManager,
            JdbcTemplate jdbcTemplate,
            Flyway flyway
    ) {
        this.repository = repository;
        this.entityManager = entityManager;
        this.jdbcTemplate = jdbcTemplate;
        this.flyway = flyway;
    }

    @BeforeEach
    void setUp() {
        DatabaseCleaner.cleanPlanSelectionTables(jdbcTemplate);
        entityManager.clear();
    }

    @Test
    void savesSelectionAndFindsActiveByUserIdWithSnapshotsAndAuditing() {
        UUID userId = insertUser(1001L);
        Plan plan = activePlan(insertPlan("REPO_LIMITED", PlanType.TRAFFIC_LIMITED, PlanTestData.THIRTY_GIB));

        PlanSelection saved = repository.save(PlanSelection.create(userId, plan, NOW, TTL));
        entityManager.flush();
        entityManager.clear();

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        PlanSelection found = repository.findActiveByUserId(userId).orElseThrow();
        assertThat(found.getPlanId()).isEqualTo(plan.getId());
        assertThat(found.getPlanCodeSnapshot()).isEqualTo("REPO_LIMITED");
        assertThat(found.getPlanTypeSnapshot()).isEqualTo(PlanType.TRAFFIC_LIMITED);
        assertThat(found.getTrafficLimitBytesSnapshot()).isEqualTo(PlanTestData.THIRTY_GIB);
        assertThat(found.getCurrencySnapshot()).isEqualTo(CurrencyCode.IRT);
        assertThat(found.getStatus()).isEqualTo(PlanSelectionStatus.ACTIVE);
        assertThat(repository.existsActiveByUserId(userId)).isTrue();
    }

    @Test
    void ordersHistoryBySelectedAtDescending() {
        UUID userId = insertUser(1002L);
        Plan plan = activePlan(insertPlan("HISTORY_PLAN", PlanType.UNLIMITED, null));
        PlanSelection first = PlanSelection.create(userId, plan, NOW, TTL);
        first.clear(NOW.plusSeconds(1));
        repository.save(first);
        PlanSelection second = PlanSelection.create(userId, plan, NOW.plusSeconds(10), TTL);
        second.clear(NOW.plusSeconds(11));
        repository.save(second);
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findAllByUserIdOrderBySelectedAtDesc(userId))
                .extracting(PlanSelection::getSelectedAt)
                .containsExactly(NOW.plusSeconds(10), NOW);
    }

    @Test
    void allowsMultipleExpiredHistoricalRowsForOneUser() {
        UUID userId = insertUser(1007L);
        Plan plan = activePlan(insertPlan("EXPIRED_HISTORY_PLAN", PlanType.UNLIMITED, null));
        PlanSelection first = PlanSelection.create(userId, plan, NOW, TTL);
        first.expire(NOW.plus(TTL));
        repository.save(first);
        PlanSelection second = PlanSelection.create(userId, plan, NOW.plusSeconds(10), TTL);
        second.expire(NOW.plusSeconds(10).plus(TTL));
        repository.save(second);
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findAllByUserIdOrderBySelectedAtDesc(userId))
                .extracting(PlanSelection::getStatus)
                .containsExactly(PlanSelectionStatus.EXPIRED, PlanSelectionStatus.EXPIRED);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void enforcesOneActiveSelectionPerUserButAllowsTerminalHistory() {
        UUID userId = insertUser(1003L);
        Plan firstPlan = activePlan(insertPlan("UNIQUE_FIRST", PlanType.UNLIMITED, null));
        Plan secondPlan = activePlan(insertPlan("UNIQUE_SECOND", PlanType.UNLIMITED, null));
        repository.save(PlanSelection.create(userId, firstPlan, NOW, TTL));
        entityManager.clear();

        assertThatThrownBy(() -> repository.save(PlanSelection.create(userId, secondPlan, NOW.plusSeconds(1), TTL)))
                .isInstanceOfAny(DataIntegrityViolationException.class, PersistenceException.class);

        entityManager.clear();
        jdbcTemplate.update("UPDATE plan_selections SET status = 'CLEARED' WHERE user_id = ?", userId);
        repository.save(PlanSelection.create(userId, secondPlan, NOW.plusSeconds(2), TTL));

        assertThat(repository.findActiveByUserId(userId)).isPresent();
        assertThat(rowCount()).isEqualTo(2);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void enforcesForeignKeysAndSnapshotCheckConstraints() {
        UUID userId = insertUser(1004L);
        Plan plan = activePlan(insertPlan("FK_PLAN", PlanType.UNLIMITED, null));

        assertThatThrownBy(() -> insertInvalidSelection(UUID.randomUUID(), plan.getId(), "UNLIMITED", "NULL", "ACTIVE", NOW.plus(TTL)))
                .isInstanceOfAny(DataIntegrityViolationException.class, PersistenceException.class);
        assertThatThrownBy(() -> insertInvalidSelection(userId, UUID.randomUUID(), "UNLIMITED", "NULL", "ACTIVE", NOW.plus(TTL)))
                .isInstanceOfAny(DataIntegrityViolationException.class, PersistenceException.class);
        assertThatThrownBy(() -> insertInvalidSelection(userId, plan.getId(), "TRAFFIC_LIMITED", "NULL", "ACTIVE", NOW.plus(TTL)))
                .isInstanceOfAny(DataIntegrityViolationException.class, PersistenceException.class);
        assertThatThrownBy(() -> insertInvalidSelection(userId, plan.getId(), "UNLIMITED", "NULL", "BAD", NOW.plus(TTL)))
                .isInstanceOfAny(DataIntegrityViolationException.class, PersistenceException.class);
        assertThatThrownBy(() -> insertInvalidSelection(userId, plan.getId(), "UNLIMITED", "NULL", "ACTIVE", NOW))
                .isInstanceOfAny(DataIntegrityViolationException.class, PersistenceException.class);
        assertThatThrownBy(() -> insertInvalidSelectionWithValues(userId, plan.getId(), "UNLIMITED", "NULL", "ACTIVE", NOW.plus(TTL), "-1", "30", "NULL", "BAD_SELECTION", "Bad Selection"))
                .isInstanceOfAny(DataIntegrityViolationException.class, PersistenceException.class);
        assertThatThrownBy(() -> insertInvalidSelectionWithValues(userId, plan.getId(), "UNLIMITED", "NULL", "ACTIVE", NOW.plus(TTL), "500000", "0", "NULL", "BAD_SELECTION", "Bad Selection"))
                .isInstanceOfAny(DataIntegrityViolationException.class, PersistenceException.class);
        assertThatThrownBy(() -> insertInvalidSelectionWithValues(userId, plan.getId(), "UNLIMITED", "NULL", "ACTIVE", NOW.plus(TTL), "500000", "30", "0", "BAD_SELECTION", "Bad Selection"))
                .isInstanceOfAny(DataIntegrityViolationException.class, PersistenceException.class);
        assertThatThrownBy(() -> insertInvalidSelectionWithValues(userId, plan.getId(), "UNLIMITED", "NULL", "ACTIVE", NOW.plus(TTL), "500000", "30", "NULL", null, "Bad Selection"))
                .isInstanceOfAny(DataIntegrityViolationException.class, PersistenceException.class);
        assertThatThrownBy(() -> insertInvalidSelectionWithValues(userId, plan.getId(), "UNLIMITED", "NULL", "ACTIVE", NOW.plus(TTL), "500000", "30", "NULL", "BAD_SELECTION", null))
                .isInstanceOfAny(DataIntegrityViolationException.class, PersistenceException.class);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void restrictsUserAndPlanDeletionWhenSelectionHistoryExists() {
        UUID userId = insertUser(1005L);
        Plan plan = activePlan(insertPlan("RESTRICT_PLAN", PlanType.UNLIMITED, null));
        PlanSelection selection = PlanSelection.create(userId, plan, NOW, TTL);
        selection.clear(NOW.plusSeconds(1));
        repository.save(selection);

        assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void restrictsPlanDeletionWhenSelectionHistoryExists() {
        UUID userId = insertUser(1006L);
        Plan plan = activePlan(insertPlan("RESTRICT_PLAN_DELETE", PlanType.UNLIMITED, null));
        PlanSelection selection = PlanSelection.create(userId, plan, NOW, TTL);
        selection.clear(NOW.plusSeconds(1));
        repository.save(selection);

        assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM plans WHERE id = ?", plan.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void flywayMigrationRunsAndHibernateValidationSucceeds() {
        assertThat(flyway.info().current()).isNotNull();
        assertThat(Arrays.stream(flyway.info().applied()))
                .anySatisfy(info -> assertThat(info.getVersion().getVersion()).isEqualTo("6"));
    }

    private UUID insertUser(Long telegramUserId) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO users (
                    id, telegram_user_id, username, first_name, last_name, language, status, blocked,
                    last_interaction_at, created_at, updated_at
                )
                VALUES (?, ?, 'repo_user', 'Ali', NULL, 'FA', 'ACTIVE', FALSE, ?, ?, ?)
                """, id, telegramUserId, timestamp(NOW), timestamp(NOW), timestamp(NOW));
        return id;
    }

    private Plan insertPlan(String code, PlanType type, Long trafficLimitBytes) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO plans (
                    id, code, name, description, status, type, price_amount, currency, duration_days,
                    traffic_limit_bytes, max_devices, display_order, created_at, updated_at
                )
                VALUES (?, ?, ?, NULL, 'ACTIVE', ?, 500000, 'IRT', 30, ?, NULL, 1, ?, ?)
                """, id, code, code, type.name(), trafficLimitBytes, timestamp(NOW), timestamp(NOW));
        Plan plan = type == PlanType.UNLIMITED
                ? Plan.create(code, code, null, type, 500_000L, CurrencyCode.IRT, 30, null, null, 1)
                : Plan.create(code, code, null, type, 500_000L, CurrencyCode.IRT, 30, trafficLimitBytes, null, 1);
        ReflectionTestUtils.setField(plan, "id", id);
        plan.activate();
        return plan;
    }

    private Plan activePlan(Plan plan) {
        return plan;
    }

    private void insertInvalidSelection(
            UUID userId,
            UUID planId,
            String type,
            String trafficLimitBytes,
            String status,
            Instant expiresAt
    ) {
        insertInvalidSelectionWithValues(
                userId,
                planId,
                type,
                trafficLimitBytes,
                status,
                expiresAt,
                "500000",
                "30",
                "NULL",
                "BAD_SELECTION",
                "Bad Selection"
        );
    }

    private void insertInvalidSelectionWithValues(
            UUID userId,
            UUID planId,
            String type,
            String trafficLimitBytes,
            String status,
            Instant expiresAt,
            String priceAmount,
            String durationDays,
            String maxDevices,
            String code,
            String name
    ) {
        jdbcTemplate.update("""
                INSERT INTO plan_selections (
                    id, user_id, plan_id, plan_code_snapshot, plan_name_snapshot, plan_type_snapshot,
                    price_amount_snapshot, currency_snapshot, duration_days_snapshot,
                    traffic_limit_bytes_snapshot, max_devices_snapshot, status, selected_at, expires_at,
                    created_at, updated_at
                )
                VALUES (
                    gen_random_uuid(), ?, ?, ?, ?, ?,
                    %s, 'IRT', %s, %s, %s, ?, ?, ?, ?, ?
                )
                """.formatted(priceAmount, durationDays, trafficLimitBytes, maxDevices),
                userId,
                planId,
                code,
                name,
                type,
                status,
                timestamp(NOW),
                timestamp(expiresAt),
                timestamp(NOW),
                timestamp(NOW)
        );
    }

    private long rowCount() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM plan_selections", Long.class);
        return count == null ? 0 : count;
    }

    private Timestamp timestamp(Instant instant) {
        return Timestamp.from(instant);
    }
}
