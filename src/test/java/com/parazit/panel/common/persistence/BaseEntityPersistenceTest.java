package com.parazit.panel.common.persistence;

import com.parazit.panel.test.support.PostgreSqlContainerSupport;

import static org.assertj.core.api.Assertions.assertThat;

import com.parazit.panel.common.persistence.fixture.TestPersistenceEntity;
import com.parazit.panel.common.persistence.fixture.TestPersistenceRepository;
import com.parazit.panel.config.persistence.JpaAuditingConfiguration;
import com.parazit.panel.infrastructure.persistence.repository.fixture.TestPersistenceRepositoryAdapter;
import com.parazit.panel.infrastructure.persistence.repository.fixture.TestPersistenceSpringDataRepository;
import com.parazit.panel.test.support.MutableClockTestConfiguration;
import com.parazit.panel.test.support.MutableTestClock;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.TestConstructor;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EntityScan(basePackageClasses = TestPersistenceEntity.class)
@EnableJpaRepositories(basePackageClasses = TestPersistenceSpringDataRepository.class)
@Import({JpaAuditingConfiguration.class, TestPersistenceRepositoryAdapter.class, MutableClockTestConfiguration.class})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class BaseEntityPersistenceTest extends PostgreSqlContainerSupport {



    private final TestPersistenceRepository repository;

    private final EntityManager entityManager;

    private final Flyway flyway;

    private final DataSource dataSource;
    private final MutableTestClock clock;

    BaseEntityPersistenceTest(
            TestPersistenceRepository repository,
            EntityManager entityManager,
            Flyway flyway,
            DataSource dataSource,
            Clock clock
    ) {
        this.repository = repository;
        this.entityManager = entityManager;
        this.flyway = flyway;
        this.dataSource = dataSource;
        this.clock = (MutableTestClock) clock;
    }

    @Test
    void persistsAuditedUuidEntityWithFlywayManagedSchema() {
        clock.setInstant(MutableClockTestConfiguration.DEFAULT_INSTANT);
        assertThat(dataSource).isNotNull();
        assertThat(flyway.info().current()).isNotNull();

        TestPersistenceEntity saved = repository.save(new TestPersistenceEntity("initial"));
        entityManager.flush();
        UUID id = saved.getId();

        assertThat(id).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();

        entityManager.clear();
        TestPersistenceEntity persisted = repository.findById(id).orElseThrow();
        Instant createdAt = persisted.getCreatedAt();
        Instant updatedAt = persisted.getUpdatedAt();

        clock.setInstant(MutableClockTestConfiguration.DEFAULT_INSTANT.plusSeconds(60));
        persisted.setName("updated");
        repository.save(persisted);
        entityManager.flush();
        entityManager.clear();

        TestPersistenceEntity found = repository.findById(id).orElseThrow();

        assertThat(found.getName()).isEqualTo("updated");
        assertThat(found.getCreatedAt()).isEqualTo(createdAt);
        assertThat(found.getUpdatedAt()).isAfter(updatedAt);
    }
}
