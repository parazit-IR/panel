package com.parazit.panel.application.user.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parazit.panel.application.user.settings.command.UpdateUserSettingsCommand;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.UserLanguage;
import com.parazit.panel.domain.user.UserStatus;
import com.parazit.panel.domain.user.repository.UserRepository;
import com.parazit.panel.domain.user.settings.UserSettings;
import com.parazit.panel.domain.user.settings.repository.UserSettingsRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class UpdateUserSettingsServiceTest {

    private static final Instant CREATED_AT = Instant.parse("2026-07-10T12:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-07-10T12:05:00Z");
    private static final Instant LAST_INTERACTION_AT = Instant.parse("2026-07-10T12:03:00Z");

    @Test
    void updatesExistingSettings() {
        FakeUserRepository userRepository = new FakeUserRepository();
        FakeUserSettingsRepository settingsRepository = new FakeUserSettingsRepository();
        User user = user();
        UserSettings settings = settings(user.getId());
        userRepository.save(user);
        settingsRepository.save(settings);
        userRepository.resetCounters();
        settingsRepository.resetCounters();

        var result = service(userRepository, settingsRepository).updateSettings(
                new UpdateUserSettingsCommand(5001L, false, false, true, 60)
        );

        assertThat(userRepository.findByTelegramUserIdCalls).isEqualTo(1);
        assertThat(userRepository.saveCalls).isZero();
        assertThat(settingsRepository.findByUserIdCalls).isEqualTo(1);
        assertThat(settingsRepository.saveCalls).isEqualTo(1);
        assertThat(result.notificationsEnabled()).isFalse();
        assertThat(result.renewalRemindersEnabled()).isFalse();
        assertThat(result.usageAlertsEnabled()).isTrue();
        assertThat(result.usageAlertThresholdPercent()).isEqualTo(60);
    }

    @Test
    void createsDefaultsWhenMissingBeforeUpdating() {
        FakeUserRepository userRepository = new FakeUserRepository();
        FakeUserSettingsRepository settingsRepository = new FakeUserSettingsRepository();
        User user = user();
        userRepository.save(user);
        userRepository.resetCounters();
        settingsRepository.resetCounters();

        var result = service(userRepository, settingsRepository).updateSettings(
                new UpdateUserSettingsCommand(5001L, true, false, false, 45)
        );

        assertThat(settingsRepository.findByUserIdCalls).isEqualTo(1);
        assertThat(settingsRepository.saveCalls).isEqualTo(2);
        assertThat(settingsRepository.count()).isEqualTo(1);
        assertThat(result.notificationsEnabled()).isTrue();
        assertThat(result.renewalRemindersEnabled()).isFalse();
        assertThat(result.usageAlertsEnabled()).isFalse();
        assertThat(result.usageAlertThresholdPercent()).isEqualTo(45);
    }

    @Test
    void rejectsInvalidThreshold() {
        FakeUserRepository userRepository = new FakeUserRepository();
        FakeUserSettingsRepository settingsRepository = new FakeUserSettingsRepository();

        assertThatThrownBy(() -> service(userRepository, settingsRepository)
                .updateSettings(new UpdateUserSettingsCommand(5001L, true, true, true, 0)))
                .isInstanceOf(InvalidUserSettingsCommandException.class)
                .hasMessage("usageAlertThresholdPercent must be between 1 and 100");
        assertThatThrownBy(() -> service(userRepository, settingsRepository)
                .updateSettings(new UpdateUserSettingsCommand(5001L, true, true, true, null)))
                .isInstanceOf(InvalidUserSettingsCommandException.class)
                .hasMessage("usageAlertThresholdPercent must not be null");

        assertThat(userRepository.findByTelegramUserIdCalls).isZero();
        assertThat(settingsRepository.saveCalls).isZero();
    }

    @Test
    void preservesUserFields() {
        FakeUserRepository userRepository = new FakeUserRepository();
        FakeUserSettingsRepository settingsRepository = new FakeUserSettingsRepository();
        User user = user();
        user.changeLanguage(UserLanguage.EN);
        user.suspend();
        user.block();
        userRepository.save(user);
        userRepository.resetCounters();

        service(userRepository, settingsRepository).updateSettings(
                new UpdateUserSettingsCommand(5001L, false, false, false, 30)
        );

        User persisted = userRepository.findByTelegramUserId(5001L).orElseThrow();
        assertThat(userRepository.saveCalls).isZero();
        assertThat(persisted.getUsername()).isEqualTo("telegram_user");
        assertThat(persisted.getFirstName()).isEqualTo("Ali");
        assertThat(persisted.getLastName()).isEqualTo("Ahmadi");
        assertThat(persisted.getLanguage()).isEqualTo(UserLanguage.EN);
        assertThat(persisted.getStatus()).isEqualTo(UserStatus.SUSPENDED);
        assertThat(persisted.getBlocked()).isTrue();
        assertThat(persisted.getLastInteractionAt()).isEqualTo(LAST_INTERACTION_AT);
    }

    @Test
    void sameValueUpdateSucceeds() {
        FakeUserRepository userRepository = new FakeUserRepository();
        FakeUserSettingsRepository settingsRepository = new FakeUserSettingsRepository();
        User user = user();
        userRepository.save(user);
        settingsRepository.save(settings(user.getId()));
        settingsRepository.resetCounters();

        var result = service(userRepository, settingsRepository).updateSettings(
                new UpdateUserSettingsCommand(5001L, true, true, true, 80)
        );

        assertThat(settingsRepository.saveCalls).isEqualTo(1);
        assertThat(result.notificationsEnabled()).isTrue();
        assertThat(result.renewalRemindersEnabled()).isTrue();
        assertThat(result.usageAlertsEnabled()).isTrue();
        assertThat(result.usageAlertThresholdPercent()).isEqualTo(80);
    }

    private UpdateUserSettingsService service(
            FakeUserRepository userRepository,
            FakeUserSettingsRepository settingsRepository
    ) {
        UserSettingsCreationService creationService = new UserSettingsCreationService(settingsRepository);
        UserSettingsDefaultsService defaultsService = new UserSettingsDefaultsService(settingsRepository, creationService);
        return new UpdateUserSettingsService(userRepository, settingsRepository, defaultsService);
    }

    private User user() {
        User user = User.create(5001L, "@telegram_user", "Ali", "Ahmadi", UserLanguage.FA, LAST_INTERACTION_AT);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(user, "createdAt", CREATED_AT);
        ReflectionTestUtils.setField(user, "updatedAt", UPDATED_AT);
        return user;
    }

    private UserSettings settings(UUID userId) {
        UserSettings settings = UserSettings.createDefault(userId);
        ReflectionTestUtils.setField(settings, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(settings, "createdAt", CREATED_AT);
        ReflectionTestUtils.setField(settings, "updatedAt", UPDATED_AT);
        return settings;
    }

    private static final class FakeUserRepository implements UserRepository {

        private final Map<Long, User> users = new LinkedHashMap<>();
        private int findByTelegramUserIdCalls;
        private int saveCalls;

        @Override
        public Optional<User> findByTelegramUserId(Long telegramUserId) {
            findByTelegramUserIdCalls++;
            return Optional.ofNullable(users.get(telegramUserId));
        }

        @Override
        public boolean existsByTelegramUserId(Long telegramUserId) {
            return users.containsKey(telegramUserId);
        }

        @Override
        public Optional<User> findById(UUID id) {
            return users.values().stream().filter(user -> id.equals(user.getId())).findFirst();
        }

        @Override
        public List<User> findAll() {
            return new ArrayList<>(users.values());
        }

        @Override
        public User save(User user) {
            saveCalls++;
            users.put(user.getTelegramUserId(), user);
            return user;
        }

        @Override
        public List<User> saveAll(Collection<User> entities) {
            return entities.stream().map(this::save).toList();
        }

        @Override
        public boolean existsById(UUID id) {
            return findById(id).isPresent();
        }

        @Override
        public long count() {
            return users.size();
        }

        @Override
        public void delete(User entity) {
            users.remove(entity.getTelegramUserId());
        }

        @Override
        public void deleteById(UUID id) {
            findById(id).ifPresent(this::delete);
        }

        private void resetCounters() {
            findByTelegramUserIdCalls = 0;
            saveCalls = 0;
        }
    }

    private static final class FakeUserSettingsRepository implements UserSettingsRepository {

        private final Map<UUID, UserSettings> settingsByUserId = new LinkedHashMap<>();
        private int findByUserIdCalls;
        private int saveCalls;

        @Override
        public Optional<UserSettings> findByUserId(UUID userId) {
            findByUserIdCalls++;
            return Optional.ofNullable(settingsByUserId.get(userId));
        }

        @Override
        public boolean existsByUserId(UUID userId) {
            return settingsByUserId.containsKey(userId);
        }

        @Override
        public Optional<UserSettings> findById(UUID id) {
            return settingsByUserId.values().stream().filter(settings -> id.equals(settings.getId())).findFirst();
        }

        @Override
        public List<UserSettings> findAll() {
            return new ArrayList<>(settingsByUserId.values());
        }

        @Override
        public UserSettings save(UserSettings settings) {
            saveCalls++;
            if (settings.getId() == null) {
                ReflectionTestUtils.setField(settings, "id", UUID.randomUUID());
            }
            if (settings.getCreatedAt() == null) {
                ReflectionTestUtils.setField(settings, "createdAt", CREATED_AT);
            }
            ReflectionTestUtils.setField(settings, "updatedAt", UPDATED_AT);
            settingsByUserId.put(settings.getUserId(), settings);
            return settings;
        }

        @Override
        public List<UserSettings> saveAll(Collection<UserSettings> entities) {
            return entities.stream().map(this::save).toList();
        }

        @Override
        public boolean existsById(UUID id) {
            return findById(id).isPresent();
        }

        @Override
        public long count() {
            return settingsByUserId.size();
        }

        @Override
        public void delete(UserSettings entity) {
            settingsByUserId.remove(entity.getUserId());
        }

        @Override
        public void deleteById(UUID id) {
            findById(id).ifPresent(this::delete);
        }

        private void resetCounters() {
            findByUserIdCalls = 0;
            saveCalls = 0;
        }
    }
}
