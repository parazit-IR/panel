package com.parazit.panel.integration.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parazit.panel.application.port.in.user.ChangeUserLanguageUseCase;
import com.parazit.panel.application.port.in.user.GetUserLanguageUseCase;
import com.parazit.panel.application.port.in.user.RegisterUserUseCase;
import com.parazit.panel.application.user.UserNotFoundException;
import com.parazit.panel.application.user.command.ChangeUserLanguageCommand;
import com.parazit.panel.application.user.command.RegisterUserCommand;
import com.parazit.panel.application.user.query.GetUserLanguageQuery;
import com.parazit.panel.application.user.result.UserLanguageResult;
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
class UserLanguageIntegrationTest {

    private static final Instant REGISTERED_AT = Instant.parse("2026-07-10T12:00:00Z");
    private static final Instant STATE_UPDATED_AT = Instant.parse("2026-07-10T12:05:00Z");
    private static final Instant LANGUAGE_UPDATED_AT = Instant.parse("2026-07-10T12:15:00Z");

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("panel_user_language_test")
            .withUsername("panel")
            .withPassword("panel");

    private final RegisterUserUseCase registerUserUseCase;
    private final GetUserLanguageUseCase getUserLanguageUseCase;
    private final ChangeUserLanguageUseCase changeUserLanguageUseCase;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;
    private final Flyway flyway;
    private final MutableClock clock;

    UserLanguageIntegrationTest(
            RegisterUserUseCase registerUserUseCase,
            GetUserLanguageUseCase getUserLanguageUseCase,
            ChangeUserLanguageUseCase changeUserLanguageUseCase,
            UserRepository userRepository,
            JdbcTemplate jdbcTemplate,
            Flyway flyway,
            Clock clock
    ) {
        this.registerUserUseCase = registerUserUseCase;
        this.getUserLanguageUseCase = getUserLanguageUseCase;
        this.changeUserLanguageUseCase = changeUserLanguageUseCase;
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
        clock.setInstant(REGISTERED_AT);
    }

    @Test
    void languageCanBeReadAfterRegistration() {
        registerUser("en-US");

        UserLanguageResult result = getUserLanguageUseCase.getLanguage(new GetUserLanguageQuery(8001L));

        assertThat(result.userId()).isNotNull();
        assertThat(result.telegramUserId()).isEqualTo(8001L);
        assertThat(result.language()).isEqualTo(UserLanguage.EN);
        assertThat(result.updatedAt()).isEqualTo(REGISTERED_AT);
    }

    @Test
    void languageChangePersistsAndReloadReturnsNewLanguage() {
        registerUser("fa");

        clock.setInstant(LANGUAGE_UPDATED_AT);
        UserLanguageResult changed = changeUserLanguageUseCase.changeLanguage(
                new ChangeUserLanguageCommand(8001L, "en")
        );

        assertThat(changed.language()).isEqualTo(UserLanguage.EN);
        assertThat(changed.updatedAt()).isEqualTo(LANGUAGE_UPDATED_AT);

        UserLanguageResult reloaded = getUserLanguageUseCase.getLanguage(new GetUserLanguageQuery(8001L));
        assertThat(reloaded.language()).isEqualTo(UserLanguage.EN);
        assertThat(reloaded.updatedAt()).isEqualTo(LANGUAGE_UPDATED_AT);

        User persisted = userRepository.findByTelegramUserId(8001L).orElseThrow();
        assertThat(persisted.getLastInteractionAt()).isEqualTo(LANGUAGE_UPDATED_AT);
    }

    @Test
    void sameLanguageUpdateIsIdempotentAndDoesNotChangeUpdatedAt() {
        registerUser("fa");

        clock.setInstant(LANGUAGE_UPDATED_AT);
        UserLanguageResult result = changeUserLanguageUseCase.changeLanguage(
                new ChangeUserLanguageCommand(8001L, "fa-IR")
        );

        assertThat(result.language()).isEqualTo(UserLanguage.FA);
        assertThat(result.updatedAt()).isEqualTo(REGISTERED_AT);

        User persisted = userRepository.findByTelegramUserId(8001L).orElseThrow();
        assertThat(persisted.getUpdatedAt()).isEqualTo(REGISTERED_AT);
        assertThat(persisted.getLastInteractionAt()).isEqualTo(REGISTERED_AT);
    }

    @Test
    void missingUserThrowsUserNotFoundException() {
        assertThatThrownBy(() -> getUserLanguageUseCase.getLanguage(new GetUserLanguageQuery(9999L)))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found for telegramUserId 9999");

        assertThatThrownBy(() -> changeUserLanguageUseCase.changeLanguage(new ChangeUserLanguageCommand(9999L, "en")))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found for telegramUserId 9999");
    }

    @Test
    void blockedStatusAndProfileFieldsArePreserved() {
        registerUser("fa");
        clock.setInstant(STATE_UPDATED_AT);
        User existing = userRepository.findByTelegramUserId(8001L).orElseThrow();
        existing.suspend();
        existing.block();
        userRepository.save(existing);

        clock.setInstant(LANGUAGE_UPDATED_AT);
        changeUserLanguageUseCase.changeLanguage(new ChangeUserLanguageCommand(8001L, "en"));

        User persisted = userRepository.findByTelegramUserId(8001L).orElseThrow();
        assertThat(persisted.getUsername()).isEqualTo("telegram_user");
        assertThat(persisted.getFirstName()).isEqualTo("Ali");
        assertThat(persisted.getLastName()).isEqualTo("Ahmadi");
        assertThat(persisted.getStatus()).isEqualTo(UserStatus.SUSPENDED);
        assertThat(persisted.getBlocked()).isTrue();
        assertThat(persisted.getLanguage()).isEqualTo(UserLanguage.EN);
        assertThat(persisted.getUpdatedAt()).isEqualTo(LANGUAGE_UPDATED_AT);
    }

    @Test
    void flywayMigrationRunsAndHibernateValidationSucceeds() {
        assertThat(flyway.info().current()).isNotNull();
        assertThat(Arrays.stream(flyway.info().applied()))
                .anySatisfy(info -> assertThat(info.getVersion().getVersion()).isEqualTo("3"));
    }

    private void registerUser(String languageCode) {
        registerUserUseCase.register(new RegisterUserCommand(
                8001L,
                "@telegram_user",
                "Ali",
                "Ahmadi",
                languageCode
        ));
    }

    @TestConfiguration
    static class MutableClockConfiguration {

        @Bean
        @Primary
        Clock mutableClock() {
            return new MutableClock(REGISTERED_AT);
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
