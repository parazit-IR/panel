package com.parazit.panel.integration.user.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parazit.panel.application.port.in.user.RegisterUserUseCase;
import com.parazit.panel.application.port.in.user.settings.GetUserSettingsUseCase;
import com.parazit.panel.application.port.in.user.settings.UpdateUserSettingsUseCase;
import com.parazit.panel.application.user.UserNotFoundException;
import com.parazit.panel.application.user.command.RegisterUserCommand;
import com.parazit.panel.application.user.settings.InvalidUserSettingsCommandException;
import com.parazit.panel.application.user.settings.command.UpdateUserSettingsCommand;
import com.parazit.panel.application.user.settings.query.GetUserSettingsQuery;
import com.parazit.panel.application.user.settings.result.UserSettingsResult;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.UserLanguage;
import com.parazit.panel.domain.user.UserStatus;
import com.parazit.panel.domain.user.repository.UserRepository;
import com.parazit.panel.domain.user.settings.UserSettings;
import com.parazit.panel.domain.user.settings.repository.UserSettingsRepository;
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
class UserSettingsIntegrationTest {

    private static final Instant REGISTERED_AT = Instant.parse("2026-07-10T12:00:00Z");
    private static final Instant SETTINGS_UPDATED_AT = Instant.parse("2026-07-10T12:15:00Z");

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("panel_user_settings_test")
            .withUsername("panel")
            .withPassword("panel");

    private final RegisterUserUseCase registerUserUseCase;
    private final GetUserSettingsUseCase getUserSettingsUseCase;
    private final UpdateUserSettingsUseCase updateUserSettingsUseCase;
    private final UserRepository userRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final JdbcTemplate jdbcTemplate;
    private final MutableClock clock;

    UserSettingsIntegrationTest(
            RegisterUserUseCase registerUserUseCase,
            GetUserSettingsUseCase getUserSettingsUseCase,
            UpdateUserSettingsUseCase updateUserSettingsUseCase,
            UserRepository userRepository,
            UserSettingsRepository userSettingsRepository,
            JdbcTemplate jdbcTemplate,
            Clock clock
    ) {
        this.registerUserUseCase = registerUserUseCase;
        this.getUserSettingsUseCase = getUserSettingsUseCase;
        this.updateUserSettingsUseCase = updateUserSettingsUseCase;
        this.userRepository = userRepository;
        this.userSettingsRepository = userSettingsRepository;
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
        jdbcTemplate.update("DELETE FROM user_settings");
        jdbcTemplate.update("DELETE FROM users");
        clock.setInstant(REGISTERED_AT);
    }

    @Test
    void getCreatesDefaultsWhenMissing() {
        registerUser();
        User user = userRepository.findByTelegramUserId(8001L).orElseThrow();
        jdbcTemplate.update("DELETE FROM user_settings WHERE user_id = ?", user.getId());

        UserSettingsResult result = getUserSettingsUseCase.getSettings(new GetUserSettingsQuery(8001L));

        assertThat(result.userId()).isEqualTo(user.getId());
        assertThat(result.telegramUserId()).isEqualTo(8001L);
        assertThat(result.notificationsEnabled()).isTrue();
        assertThat(result.renewalRemindersEnabled()).isTrue();
        assertThat(result.usageAlertsEnabled()).isTrue();
        assertThat(result.usageAlertThresholdPercent()).isEqualTo(80);
        assertThat(settingsRowCount(user.getId())).isEqualTo(1);
    }

    @Test
    void getReturnsExistingSettings() {
        registerUser();
        User user = userRepository.findByTelegramUserId(8001L).orElseThrow();
        UserSettings existing = userSettingsRepository.findByUserId(user.getId()).orElseThrow();
        existing.updatePreferences(false, true, false, 65);
        userSettingsRepository.save(existing);

        UserSettingsResult result = getUserSettingsUseCase.getSettings(new GetUserSettingsQuery(8001L));

        assertThat(result.notificationsEnabled()).isFalse();
        assertThat(result.renewalRemindersEnabled()).isTrue();
        assertThat(result.usageAlertsEnabled()).isFalse();
        assertThat(result.usageAlertThresholdPercent()).isEqualTo(65);
        assertThat(settingsRowCount(user.getId())).isEqualTo(1);
    }

    @Test
    void putUpdatesSettingsAndReloadReturnsUpdatedValues() {
        registerUser();

        clock.setInstant(SETTINGS_UPDATED_AT);
        UserSettingsResult updated = updateUserSettingsUseCase.updateSettings(
                new UpdateUserSettingsCommand(8001L, false, false, false, 25)
        );

        assertThat(updated.notificationsEnabled()).isFalse();
        assertThat(updated.renewalRemindersEnabled()).isFalse();
        assertThat(updated.usageAlertsEnabled()).isFalse();
        assertThat(updated.usageAlertThresholdPercent()).isEqualTo(25);
        assertThat(updated.updatedAt()).isEqualTo(SETTINGS_UPDATED_AT);

        UserSettingsResult reloaded = getUserSettingsUseCase.getSettings(new GetUserSettingsQuery(8001L));
        assertThat(reloaded.notificationsEnabled()).isFalse();
        assertThat(reloaded.renewalRemindersEnabled()).isFalse();
        assertThat(reloaded.usageAlertsEnabled()).isFalse();
        assertThat(reloaded.usageAlertThresholdPercent()).isEqualTo(25);
        assertThat(reloaded.updatedAt()).isEqualTo(SETTINGS_UPDATED_AT);
    }

    @Test
    void invalidThresholdRejected() {
        registerUser();

        assertThatThrownBy(() -> updateUserSettingsUseCase.updateSettings(
                new UpdateUserSettingsCommand(8001L, true, true, true, 0)
        ))
                .isInstanceOf(InvalidUserSettingsCommandException.class)
                .hasMessage("usageAlertThresholdPercent must be between 1 and 100");
    }

    @Test
    void missingUserReturnsNotFound() {
        assertThatThrownBy(() -> getUserSettingsUseCase.getSettings(new GetUserSettingsQuery(9999L)))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found for telegramUserId 9999");
        assertThatThrownBy(() -> updateUserSettingsUseCase.updateSettings(
                new UpdateUserSettingsCommand(9999L, true, true, true, 80)
        ))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found for telegramUserId 9999");
    }

    @Test
    void userProfileLanguageStatusAndBlockedRemainUnchanged() {
        registerUser();
        User existing = userRepository.findByTelegramUserId(8001L).orElseThrow();
        existing.changeLanguage(UserLanguage.EN);
        existing.suspend();
        existing.block();
        userRepository.save(existing);

        updateUserSettingsUseCase.updateSettings(new UpdateUserSettingsCommand(8001L, false, false, false, 30));

        User persisted = userRepository.findByTelegramUserId(8001L).orElseThrow();
        assertThat(persisted.getUsername()).isEqualTo("telegram_user");
        assertThat(persisted.getFirstName()).isEqualTo("Ali");
        assertThat(persisted.getLastName()).isEqualTo("Ahmadi");
        assertThat(persisted.getLanguage()).isEqualTo(UserLanguage.EN);
        assertThat(persisted.getStatus()).isEqualTo(UserStatus.SUSPENDED);
        assertThat(persisted.getBlocked()).isTrue();
    }

    @Test
    void duplicateSettingsNeverOccur() {
        registerUser();
        updateUserSettingsUseCase.updateSettings(new UpdateUserSettingsCommand(8001L, true, true, true, 80));
        getUserSettingsUseCase.getSettings(new GetUserSettingsQuery(8001L));

        User user = userRepository.findByTelegramUserId(8001L).orElseThrow();
        assertThat(settingsRowCount(user.getId())).isEqualTo(1);
    }

    private void registerUser() {
        registerUserUseCase.register(new RegisterUserCommand(
                8001L,
                "@telegram_user",
                "Ali",
                "Ahmadi",
                "fa"
        ));
    }

    private long settingsRowCount(Object userId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_settings WHERE user_id = ?",
                Long.class,
                userId
        );
        return count == null ? 0 : count;
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
