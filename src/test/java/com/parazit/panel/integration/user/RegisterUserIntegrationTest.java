package com.parazit.panel.integration.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.parazit.panel.application.port.in.user.RegisterUserUseCase;
import com.parazit.panel.application.user.command.RegisterUserCommand;
import com.parazit.panel.application.user.result.RegisterUserResult;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.UserLanguage;
import com.parazit.panel.domain.user.UserStatus;
import com.parazit.panel.domain.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestConstructor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = {
        "spring.profiles.active=local",
        "spring.security.user.name=test",
        "spring.security.user.password=test"
})
@Testcontainers
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class RegisterUserIntegrationTest {

    private static final Instant FIRST_INSTANT = Instant.parse("2026-07-10T12:00:00Z");
    private static final Instant SECOND_INSTANT = Instant.parse("2026-07-10T12:05:00Z");

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("panel_register_user_test")
            .withUsername("panel")
            .withPassword("panel");

    private final RegisterUserUseCase registerUserUseCase;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;
    private final Flyway flyway;
    private final MutableClock clock;

    RegisterUserIntegrationTest(
            RegisterUserUseCase registerUserUseCase,
            UserRepository userRepository,
            JdbcTemplate jdbcTemplate,
            Flyway flyway,
            Clock clock
    ) {
        this.registerUserUseCase = registerUserUseCase;
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.flyway = flyway;
        this.clock = (MutableClock) clock;
    }

    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM users");
        clock.setInstant(FIRST_INSTANT);
    }

    @Test
    void registersNewUserAndPersistsRow() {
        RegisterUserResult result = registerUserUseCase.register(command("fa"));

        assertThat(result.newlyCreated()).isTrue();
        assertThat(result.userId()).isNotNull();
        assertThat(result.telegramUserId()).isEqualTo(2001L);
        assertThat(result.username()).isEqualTo("example_user");
        assertThat(result.firstName()).isEqualTo("Ali");
        assertThat(result.lastName()).isEqualTo("Ahmadi");
        assertThat(result.language()).isEqualTo(UserLanguage.FA);
        assertThat(result.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(result.blocked()).isFalse();
        assertThat(result.registeredAt()).isEqualTo(FIRST_INSTANT);
        assertThat(result.lastInteractionAt()).isEqualTo(FIRST_INSTANT);
        assertThat(rowCount()).isEqualTo(1);

        User persisted = userRepository.findByTelegramUserId(2001L).orElseThrow();
        assertThat(persisted.getId()).isEqualTo(result.userId());
        assertThat(persisted.getCreatedAt()).isEqualTo(FIRST_INSTANT);
        assertThat(persisted.getUpdatedAt()).isEqualTo(FIRST_INSTANT);
    }

    @Test
    void repeatedRegistrationRefreshesProfileWithoutCreatingDuplicate() {
        RegisterUserResult first = registerUserUseCase.register(command("en"));

        User persisted = userRepository.findByTelegramUserId(2001L).orElseThrow();
        persisted.changeLanguage(UserLanguage.FA);
        persisted.suspend();
        persisted.block();
        userRepository.save(persisted);

        clock.setInstant(SECOND_INSTANT);
        RegisterUserResult second = registerUserUseCase.register(new RegisterUserCommand(
                2001L,
                "@updated_user",
                " Sara ",
                " Karimi ",
                "en-US"
        ));

        assertThat(second.newlyCreated()).isFalse();
        assertThat(second.userId()).isEqualTo(first.userId());
        assertThat(rowCount()).isEqualTo(1);
        assertThat(second.username()).isEqualTo("updated_user");
        assertThat(second.firstName()).isEqualTo("Sara");
        assertThat(second.lastName()).isEqualTo("Karimi");
        assertThat(second.language()).isEqualTo(UserLanguage.FA);
        assertThat(second.status()).isEqualTo(UserStatus.SUSPENDED);
        assertThat(second.blocked()).isTrue();
        assertThat(second.registeredAt()).isEqualTo(FIRST_INSTANT);
        assertThat(second.lastInteractionAt()).isEqualTo(SECOND_INSTANT);

        User refreshed = userRepository.findByTelegramUserId(2001L).orElseThrow();
        assertThat(refreshed.getId()).isEqualTo(first.userId());
        assertThat(refreshed.getUsername()).isEqualTo("updated_user");
        assertThat(refreshed.getLanguage()).isEqualTo(UserLanguage.FA);
        assertThat(refreshed.getStatus()).isEqualTo(UserStatus.SUSPENDED);
        assertThat(refreshed.getBlocked()).isTrue();
        assertThat(refreshed.getLastInteractionAt()).isEqualTo(SECOND_INSTANT);
    }

    @Test
    void flywayMigrationRunsAndHibernateValidationSucceeds() {
        assertThat(flyway.info().current()).isNotNull();
        assertThat(Arrays.stream(flyway.info().applied()))
                .anySatisfy(info -> assertThat(info.getVersion().getVersion()).isEqualTo("2"));
    }

    private RegisterUserCommand command(String languageCode) {
        return new RegisterUserCommand(2001L, "@example_user", "Ali", "Ahmadi", languageCode);
    }

    private long rowCount() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Long.class);
        return count == null ? 0 : count;
    }

    @TestConfiguration
    static class MutableClockConfiguration {

        @Bean
        @Primary
        Clock mutableClock() {
            return new MutableClock(FIRST_INSTANT);
        }
    }

    static final class MutableClock extends Clock {

        private volatile Instant instant;

        MutableClock(Instant instant) {
            this.instant = instant;
        }

        void setInstant(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
