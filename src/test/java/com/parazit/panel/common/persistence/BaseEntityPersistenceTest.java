package com.parazit.panel.common.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.parazit.panel.common.persistence.fixture.TestPersistenceEntity;
import com.parazit.panel.common.persistence.fixture.TestPersistenceRepository;
import com.parazit.panel.config.persistence.JpaAuditingConfiguration;
import com.parazit.panel.infrastructure.persistence.repository.fixture.TestPersistenceRepositoryAdapter;
import com.parazit.panel.infrastructure.persistence.repository.fixture.TestPersistenceSpringDataRepository;
import jakarta.persistence.EntityManager;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EntityScan(basePackageClasses = TestPersistenceEntity.class)
@EnableJpaRepositories(basePackageClasses = TestPersistenceSpringDataRepository.class)
@Import({JpaAuditingConfiguration.class, TestPersistenceRepositoryAdapter.class})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class BaseEntityPersistenceTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("panel_test")
            .withUsername("panel")
            .withPassword("panel");

    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    private final TestPersistenceRepository repository;

    private final EntityManager entityManager;

    private final Flyway flyway;

    private final DataSource dataSource;

    BaseEntityPersistenceTest(
            TestPersistenceRepository repository,
            EntityManager entityManager,
            Flyway flyway,
            DataSource dataSource
    ) {
        this.repository = repository;
        this.entityManager = entityManager;
        this.flyway = flyway;
        this.dataSource = dataSource;
    }

    @Test
    void persistsAuditedUuidEntityWithFlywayManagedSchema() throws InterruptedException {
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

        Thread.sleep(10);
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
