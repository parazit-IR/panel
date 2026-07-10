package com.parazit.panel.common.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.parazit.panel.common.persistence.fixture.TestPersistenceEntity;
import com.parazit.panel.common.persistence.fixture.TestPersistenceRepository;
import com.parazit.panel.config.persistence.JpaAuditingConfiguration;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EntityScan(basePackageClasses = TestPersistenceEntity.class)
@EnableJpaRepositories(basePackageClasses = TestPersistenceRepository.class)
@Import(JpaAuditingConfiguration.class)
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

    @Autowired
    private TestPersistenceRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private Flyway flyway;

    @Autowired
    private DataSource dataSource;

    @Test
    void persistsAuditedUuidEntityWithFlywayManagedSchema() throws InterruptedException {
        assertThat(dataSource).isNotNull();
        assertThat(flyway.info().current()).isNotNull();

        TestPersistenceEntity saved = repository.saveAndFlush(new TestPersistenceEntity("initial"));
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
        repository.saveAndFlush(persisted);
        entityManager.clear();

        TestPersistenceEntity found = repository.findById(id).orElseThrow();

        assertThat(found.getName()).isEqualTo("updated");
        assertThat(found.getCreatedAt()).isEqualTo(createdAt);
        assertThat(found.getUpdatedAt()).isAfter(updatedAt);
    }
}
