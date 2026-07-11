package com.parazit.panel.infrastructure.persistence.user.settings;

import com.parazit.panel.test.support.PostgreSqlContainerSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parazit.panel.config.persistence.JpaAuditingConfiguration;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.UserLanguage;
import com.parazit.panel.domain.user.settings.UserSettings;
import com.parazit.panel.domain.user.settings.repository.UserSettingsRepository;
import com.parazit.panel.test.support.MutableClockTestConfiguration;
import com.parazit.panel.test.support.MutableTestClock;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import org.flywaydb.core.Flyway;
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

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EntityScan(basePackageClasses = {User.class, UserSettings.class})
@EnableJpaRepositories(basePackageClasses = SpringDataUserSettingsRepository.class)
@Import({JpaAuditingConfiguration.class, UserSettingsRepositoryAdapter.class, MutableClockTestConfiguration.class})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class UserSettingsRepositoryIntegrationTest extends PostgreSqlContainerSupport {

    private static final Instant NOW = Instant.parse("2026-07-10T12:00:00Z");


    private final UserSettingsRepository settingsRepository;
    private final EntityManager entityManager;
    private final JdbcTemplate jdbcTemplate;
    private final Flyway flyway;
    private final MutableTestClock clock;

    UserSettingsRepositoryIntegrationTest(
            UserSettingsRepository settingsRepository,
            EntityManager entityManager,
            JdbcTemplate jdbcTemplate,
            Flyway flyway,
            Clock clock
    ) {
        this.settingsRepository = settingsRepository;
        this.entityManager = entityManager;
        this.jdbcTemplate = jdbcTemplate;
        this.flyway = flyway;
        this.clock = (MutableTestClock) clock;
    }


    @Test
    void savesSettingsAndFindsByUserId() {
        User user = persistUser(1001L);
        UserSettings saved = settingsRepository.save(UserSettings.createDefault(user.getId()));
        entityManager.flush();
        entityManager.clear();

        UserSettings found = settingsRepository.findByUserId(user.getId()).orElseThrow();

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getUserId()).isEqualTo(user.getId());
        assertThat(found.isNotificationsEnabled()).isTrue();
        assertThat(found.isRenewalRemindersEnabled()).isTrue();
        assertThat(found.isUsageAlertsEnabled()).isTrue();
        assertThat(found.getUsageAlertThresholdPercent()).isEqualTo(80);
        assertThat(settingsRepository.existsByUserId(user.getId())).isTrue();
    }

    @Test
    void rejectsDuplicateUserId() {
        User user = persistUser(1002L);
        settingsRepository.save(UserSettings.createDefault(user.getId()));
        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(() -> settingsRepository.save(UserSettings.createDefault(user.getId())))
                .isInstanceOfAny(DataIntegrityViolationException.class, PersistenceException.class);
    }

    @Test
    void rejectsInvalidThresholdByDatabaseConstraint() {
        User user = persistUser(1003L);
        UserSettings settings = UserSettings.createDefault(user.getId());
        ReflectionTestUtils.setField(settings, "usageAlertThresholdPercent", 101);

        assertThatThrownBy(() -> settingsRepository.save(settings))
                .isInstanceOfAny(DataIntegrityViolationException.class, PersistenceException.class);
    }

    @Test
    void persistsBooleanAndThresholdUpdates() {
        clock.setInstant(MutableClockTestConfiguration.DEFAULT_INSTANT);
        User user = persistUser(1004L);
        UserSettings saved = settingsRepository.save(UserSettings.createDefault(user.getId()));

        clock.setInstant(MutableClockTestConfiguration.DEFAULT_INSTANT.plusSeconds(60));
        saved.updatePreferences(false, false, false, 35);
        settingsRepository.save(saved);
        entityManager.flush();
        entityManager.clear();

        UserSettings found = settingsRepository.findByUserId(user.getId()).orElseThrow();
        assertThat(found.isNotificationsEnabled()).isFalse();
        assertThat(found.isRenewalRemindersEnabled()).isFalse();
        assertThat(found.isUsageAlertsEnabled()).isFalse();
        assertThat(found.getUsageAlertThresholdPercent()).isEqualTo(35);
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }

    @Test
    void enforcesForeignKeyToUser() {
        assertThatThrownBy(() -> settingsRepository.save(UserSettings.createDefault(UUID.randomUUID())))
                .isInstanceOfAny(DataIntegrityViolationException.class, PersistenceException.class);
    }

    @Test
    void deletingUserCascadesSettings() {
        User user = persistUser(1005L);
        settingsRepository.save(UserSettings.createDefault(user.getId()));
        entityManager.flush();
        entityManager.clear();

        User persistedUser = entityManager.find(User.class, user.getId());
        entityManager.remove(persistedUser);
        entityManager.flush();
        entityManager.clear();

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_settings WHERE user_id = ?",
                Integer.class,
                user.getId()
        );
        assertThat(count).isZero();
    }

    @Test
    void flywayMigrationRunsAndHibernateValidationSucceeds() {
        assertThat(flyway.info().current()).isNotNull();
        assertThat(Arrays.stream(flyway.info().applied()))
                .anySatisfy(info -> assertThat(info.getVersion().getVersion()).isEqualTo("3"));
    }

    private User persistUser(Long telegramUserId) {
        User user = User.create(telegramUserId, "@telegram_user", "Ali", "Ahmadi", UserLanguage.FA, NOW);
        entityManager.persist(user);
        entityManager.flush();
        return user;
    }
}
