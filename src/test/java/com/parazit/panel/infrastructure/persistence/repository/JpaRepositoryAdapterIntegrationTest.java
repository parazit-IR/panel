package com.parazit.panel.infrastructure.persistence.repository;

import com.parazit.panel.test.support.PostgreSqlContainerSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.TestConstructor;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EntityScan(basePackageClasses = TestPersistenceEntity.class)
@EnableJpaRepositories(basePackageClasses = TestPersistenceSpringDataRepository.class)
@Import({JpaAuditingConfiguration.class, TestPersistenceRepositoryAdapter.class, MutableClockTestConfiguration.class})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class JpaRepositoryAdapterIntegrationTest extends PostgreSqlContainerSupport {



    private final TestPersistenceRepository repository;

    private final EntityManager entityManager;

    private final Flyway flyway;
    private final MutableTestClock clock;

    JpaRepositoryAdapterIntegrationTest(
            TestPersistenceRepository repository,
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
    void delegatesRepositoryOperationsToSpringDataJpa() {
        assertThat(flyway.info().current()).isNotNull();
        assertThat(repository).isNotInstanceOf(JpaRepository.class);

        TestPersistenceEntity saved = repository.save(new TestPersistenceEntity("one"));
        UUID id = saved.getId();

        assertThat(id).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(repository.findById(id)).contains(saved);
        assertThat(repository.existsById(id)).isTrue();
        assertThat(repository.count()).isEqualTo(1);
        assertThat(repository.findAll()).extracting(TestPersistenceEntity::getName).containsExactly("one");

        List<TestPersistenceEntity> savedEntities = repository.saveAll(List.of(
                new TestPersistenceEntity("two"),
                new TestPersistenceEntity("three")
        ));

        assertThat(savedEntities).hasSize(2);
        assertThat(repository.count()).isEqualTo(3);

        repository.delete(savedEntities.getFirst());
        assertThat(repository.existsById(savedEntities.getFirst().getId())).isFalse();

        repository.deleteById(savedEntities.getLast().getId());
        assertThat(repository.existsById(savedEntities.getLast().getId())).isFalse();
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void preservesUuidGenerationAndAuditingThroughAdapter() {
        clock.setInstant(MutableClockTestConfiguration.DEFAULT_INSTANT);
        TestPersistenceEntity saved = repository.save(new TestPersistenceEntity("audit"));
        UUID id = saved.getId();

        entityManager.flush();
        entityManager.clear();

        TestPersistenceEntity persisted = repository.findById(id).orElseThrow();
        Instant createdAt = persisted.getCreatedAt();
        Instant updatedAt = persisted.getUpdatedAt();

        assertThat(createdAt).isNotNull();
        assertThat(updatedAt).isNotNull();

        clock.setInstant(MutableClockTestConfiguration.DEFAULT_INSTANT.plusSeconds(60));
        persisted.setName("audit-updated");
        repository.save(persisted);
        entityManager.flush();
        entityManager.clear();

        TestPersistenceEntity found = repository.findById(id).orElseThrow();

        assertThat(found.getId()).isEqualTo(id);
        assertThat(found.getName()).isEqualTo("audit-updated");
        assertThat(found.getCreatedAt()).isEqualTo(createdAt);
        assertThat(found.getUpdatedAt()).isAfter(updatedAt);
    }

    @Test
    void rejectsNullInputs() {
        assertThatNullPointerException()
                .isThrownBy(() -> repository.findById(null))
                .withMessage("id must not be null");
        assertThatNullPointerException()
                .isThrownBy(() -> repository.save(null))
                .withMessage("entity must not be null");
        assertThatNullPointerException()
                .isThrownBy(() -> repository.saveAll(null))
                .withMessage("entities must not be null");
        List<TestPersistenceEntity> entities = new ArrayList<>();
        entities.add(new TestPersistenceEntity("valid"));
        entities.add(null);

        assertThatNullPointerException()
                .isThrownBy(() -> repository.saveAll(entities))
                .withMessage("entity must not be null");
        assertThatNullPointerException()
                .isThrownBy(() -> repository.existsById(null))
                .withMessage("id must not be null");
        assertThatNullPointerException()
                .isThrownBy(() -> repository.delete(null))
                .withMessage("entity must not be null");
        assertThatNullPointerException()
                .isThrownBy(() -> repository.deleteById(null))
                .withMessage("id must not be null");
    }
}
