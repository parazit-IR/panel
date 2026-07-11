package com.parazit.panel.infrastructure.persistence.user;

import com.parazit.panel.test.support.PostgreSqlContainerSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parazit.panel.config.persistence.JpaAuditingConfiguration;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.UserLanguage;
import com.parazit.panel.domain.user.UserStatus;
import com.parazit.panel.domain.user.repository.UserRepository;
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
import org.springframework.test.context.TestConstructor;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EntityScan(basePackageClasses = User.class)
@EnableJpaRepositories(basePackageClasses = SpringDataUserRepository.class)
@Import({JpaAuditingConfiguration.class, UserRepositoryAdapter.class, MutableClockTestConfiguration.class})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class UserRepositoryAdapterIntegrationTest extends PostgreSqlContainerSupport {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");


    private final UserRepository repository;
    private final EntityManager entityManager;
    private final Flyway flyway;
    private final MutableTestClock clock;

    UserRepositoryAdapterIntegrationTest(
            UserRepository repository,
            EntityManager entityManager,
            Flyway flyway,
            Clock clock
    ) {
        this.repository = repository;
        this.entityManager = entityManager;
        this.flyway = flyway;
        this.clock = (MutableTestClock) clock;
    }


    @Test
    void persistsUserAndFindsByTelegramUserId() {
        User saved = repository.save(createUser(1001L));
        entityManager.flush();

        UUID id = saved.getId();
        assertThat(id).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();

        entityManager.clear();
        User found = repository.findByTelegramUserId(1001L).orElseThrow();

        assertThat(found.getId()).isEqualTo(id);
        assertThat(found.getTelegramUserId()).isEqualTo(1001L);
        assertThat(found.getUsername()).isEqualTo("telegram_user");
        assertThat(found.getFirstName()).isEqualTo("Ali");
        assertThat(found.getLastName()).isEqualTo("Ahmadi");
        assertThat(found.getLanguage()).isEqualTo(UserLanguage.FA);
        assertThat(found.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(found.getBlocked()).isFalse();
        assertThat(found.getLastInteractionAt()).isEqualTo(NOW);
        assertThat(repository.existsByTelegramUserId(1001L)).isTrue();
    }

    @Test
    void updatesProfileLanguageStatusBlockedAndAuditTimestamp() {
        clock.setInstant(MutableClockTestConfiguration.DEFAULT_INSTANT);
        User saved = repository.save(createUser(1002L));
        entityManager.flush();
        entityManager.clear();

        User persisted = repository.findByTelegramUserId(1002L).orElseThrow();
        Instant createdAt = persisted.getCreatedAt();
        Instant updatedAt = persisted.getUpdatedAt();
        Instant interactionTime = NOW.plusSeconds(120);

        clock.setInstant(MutableClockTestConfiguration.DEFAULT_INSTANT.plusSeconds(60));
        persisted.updateTelegramProfile("@updated_user", " Sara ", "   ", interactionTime);
        persisted.changeLanguage(UserLanguage.EN);
        persisted.suspend();
        persisted.block();
        repository.save(persisted);
        entityManager.flush();
        entityManager.clear();

        User found = repository.findByTelegramUserId(1002L).orElseThrow();

        assertThat(found.getUsername()).isEqualTo("updated_user");
        assertThat(found.getFirstName()).isEqualTo("Sara");
        assertThat(found.getLastName()).isNull();
        assertThat(found.getLanguage()).isEqualTo(UserLanguage.EN);
        assertThat(found.getStatus()).isEqualTo(UserStatus.SUSPENDED);
        assertThat(found.getBlocked()).isTrue();
        assertThat(found.getLastInteractionAt()).isEqualTo(interactionTime);
        assertThat(found.getCreatedAt()).isEqualTo(createdAt);
        assertThat(found.getUpdatedAt()).isAfter(updatedAt);
    }

    @Test
    void rejectsDuplicateTelegramUserId() {
        repository.save(createUser(1003L));
        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(() -> repository.save(User.create(
                1003L,
                "another_user",
                "Sara",
                null,
                UserLanguage.EN,
                NOW
        )))
                .isInstanceOfAny(DataIntegrityViolationException.class, PersistenceException.class);
    }

    @Test
    void flywayMigrationRunsAndHibernateValidationSucceeds() {
        assertThat(flyway.info().current()).isNotNull();
        assertThat(Arrays.stream(flyway.info().applied()))
                .anySatisfy(info -> assertThat(info.getVersion().getVersion()).isEqualTo("3"));
    }

    private User createUser(Long telegramUserId) {
        return User.create(telegramUserId, "@telegram_user", "Ali", "Ahmadi", UserLanguage.FA, NOW);
    }
}
