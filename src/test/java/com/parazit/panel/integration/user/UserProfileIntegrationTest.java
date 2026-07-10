package com.parazit.panel.integration.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parazit.panel.application.port.in.user.GetUserProfileUseCase;
import com.parazit.panel.application.port.in.user.RegisterUserUseCase;
import com.parazit.panel.application.port.in.user.UpdateUserProfileUseCase;
import com.parazit.panel.application.user.InvalidUserProfileCommandException;
import com.parazit.panel.application.user.command.RegisterUserCommand;
import com.parazit.panel.application.user.command.UpdateUserProfileCommand;
import com.parazit.panel.application.user.query.GetUserProfileQuery;
import com.parazit.panel.application.user.result.UpdateUserProfileResult;
import com.parazit.panel.application.user.result.UserProfileResult;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.UserLanguage;
import com.parazit.panel.domain.user.UserStatus;
import com.parazit.panel.domain.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
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
class UserProfileIntegrationTest {

    private static final Instant REGISTERED_AT = Instant.parse("2026-07-10T12:00:00Z");
    private static final Instant PROFILE_UPDATED_AT = Instant.parse("2026-07-10T12:15:00Z");

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("panel_user_profile_test")
            .withUsername("panel")
            .withPassword("panel");

    private final RegisterUserUseCase registerUserUseCase;
    private final GetUserProfileUseCase getUserProfileUseCase;
    private final UpdateUserProfileUseCase updateUserProfileUseCase;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;
    private final MutableClock clock;

    UserProfileIntegrationTest(
            RegisterUserUseCase registerUserUseCase,
            GetUserProfileUseCase getUserProfileUseCase,
            UpdateUserProfileUseCase updateUserProfileUseCase,
            UserRepository userRepository,
            JdbcTemplate jdbcTemplate,
            Clock clock
    ) {
        this.registerUserUseCase = registerUserUseCase;
        this.getUserProfileUseCase = getUserProfileUseCase;
        this.updateUserProfileUseCase = updateUserProfileUseCase;
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
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
    void retrievesProfile() {
        registerUser();

        UserProfileResult result = getUserProfileUseCase.getProfile(new GetUserProfileQuery(6001L));

        assertThat(result.telegramUserId()).isEqualTo(6001L);
        assertThat(result.username()).isEqualTo("telegram_user");
        assertThat(result.firstName()).isEqualTo("Ali");
        assertThat(result.lastName()).isEqualTo("Ahmadi");
        assertThat(result.language()).isEqualTo(UserLanguage.FA);
        assertThat(result.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(result.blocked()).isFalse();
        assertThat(result.createdAt()).isEqualTo(REGISTERED_AT);
        assertThat(result.updatedAt()).isEqualTo(REGISTERED_AT);
        assertThat(result.lastInteractionAt()).isEqualTo(REGISTERED_AT);
    }

    @Test
    void updatePersistsProfileAndAuditTimestamp() {
        registerUser();
        User existing = userRepository.findByTelegramUserId(6001L).orElseThrow();
        existing.suspend();
        existing.block();
        userRepository.save(existing);

        clock.setInstant(PROFILE_UPDATED_AT);
        UpdateUserProfileResult result = updateUserProfileUseCase.updateProfile(
                new UpdateUserProfileCommand(6001L, " Sara ", "   ")
        );

        assertThat(result.telegramUserId()).isEqualTo(6001L);
        assertThat(result.username()).isEqualTo("telegram_user");
        assertThat(result.firstName()).isEqualTo("Sara");
        assertThat(result.lastName()).isNull();
        assertThat(result.language()).isEqualTo(UserLanguage.FA);
        assertThat(result.status()).isEqualTo(UserStatus.SUSPENDED);
        assertThat(result.blocked()).isTrue();
        assertThat(result.createdAt()).isEqualTo(REGISTERED_AT);
        assertThat(result.updatedAt()).isEqualTo(PROFILE_UPDATED_AT);
        assertThat(result.lastInteractionAt()).isEqualTo(REGISTERED_AT);

        User persisted = userRepository.findByTelegramUserId(6001L).orElseThrow();
        assertThat(persisted.getFirstName()).isEqualTo("Sara");
        assertThat(persisted.getLastName()).isNull();
        assertThat(persisted.getLanguage()).isEqualTo(UserLanguage.FA);
        assertThat(persisted.getTelegramUserId()).isEqualTo(6001L);
        assertThat(persisted.getUsername()).isEqualTo("telegram_user");
        assertThat(persisted.getStatus()).isEqualTo(UserStatus.SUSPENDED);
        assertThat(persisted.getBlocked()).isTrue();
    }

    @Test
    void rejectsInvalidUpdateCommand() {
        registerUser();

        assertThatThrownBy(() -> updateUserProfileUseCase.updateProfile(
                new UpdateUserProfileCommand(6001L, "   ", null)
        ))
                .isInstanceOf(InvalidUserProfileCommandException.class)
                .hasMessage("firstName must not be blank");
    }

    private void registerUser() {
        registerUserUseCase.register(new RegisterUserCommand(
                6001L,
                "@telegram_user",
                "Ali",
                "Ahmadi",
                "fa"
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
