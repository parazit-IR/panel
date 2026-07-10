package com.parazit.panel.application.user.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parazit.panel.application.user.UserNotFoundException;
import com.parazit.panel.application.user.settings.query.GetUserSettingsQuery;
import com.parazit.panel.application.user.settings.result.UserSettingsResult;
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

class GetUserSettingsServiceTest {

    private static final Instant CREATED_AT = Instant.parse("2026-07-10T12:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-07-10T12:05:00Z");
    private static final Instant LAST_INTERACTION_AT = Instant.parse("2026-07-10T12:03:00Z");

    @Test
    void existingUserAndExistingSettingsReturnsSettings() {
        FakeUserRepository userRepository = new FakeUserRepository();
        FakeUserSettingsRepository settingsRepository = new FakeUserSettingsRepository();
        User user = user();
        UserSettings settings = settings(user.getId(), false, true, false, 70);
        userRepository.save(user);
        settingsRepository.save(settings);
        userRepository.resetCounters();
        settingsRepository.resetCounters();

        UserSettingsResult result = service(userRepository, settingsRepository)
                .getSettings(new GetUserSettingsQuery(5001L));

        assertThat(userRepository.findByTelegramUserIdCalls).isEqualTo(1);
        assertThat(userRepository.saveCalls).isZero();
        assertThat(settingsRepository.findByUserIdCalls).isEqualTo(1);
        assertThat(settingsRepository.saveCalls).isZero();
        assertThat(result.settingsId()).isEqualTo(settings.getId());
        assertThat(result.userId()).isEqualTo(user.getId());
        assertThat(result.telegramUserId()).isEqualTo(5001L);
        assertThat(result.notificationsEnabled()).isFalse();
        assertThat(result.renewalRemindersEnabled()).isTrue();
        assertThat(result.usageAlertsEnabled()).isFalse();
        assertThat(result.usageAlertThresholdPercent()).isEqualTo(70);
        assertThat(result.createdAt()).isEqualTo(CREATED_AT);
        assertThat(result.updatedAt()).isEqualTo(UPDATED_AT);
    }

    @Test
    void existingUserWithoutSettingsCreatesDefaults() {
        FakeUserRepository userRepository = new FakeUserRepository();
        FakeUserSettingsRepository settingsRepository = new FakeUserSettingsRepository();
        User user = user();
        userRepository.save(user);
        userRepository.resetCounters();
        settingsRepository.resetCounters();

        UserSettingsResult result = service(userRepository, settingsRepository)
                .getSettings(new GetUserSettingsQuery(5001L));

        assertThat(settingsRepository.findByUserIdCalls).isEqualTo(1);
        assertThat(settingsRepository.saveCalls).isEqualTo(1);
        assertThat(settingsRepository.count()).isEqualTo(1);
        assertThat(result.userId()).isEqualTo(user.getId());
        assertThat(result.notificationsEnabled()).isTrue();
        assertThat(result.renewalRemindersEnabled()).isTrue();
        assertThat(result.usageAlertsEnabled()).isTrue();
        assertThat(result.usageAlertThresholdPercent()).isEqualTo(80);
    }

    @Test
    void missingUserThrowsUserNotFoundException() {
        FakeUserRepository userRepository = new FakeUserRepository();
        FakeUserSettingsRepository settingsRepository = new FakeUserSettingsRepository();

        assertThatThrownBy(() -> service(userRepository, settingsRepository)
                .getSettings(new GetUserSettingsQuery(9999L)))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found for telegramUserId 9999");

        assertThat(settingsRepository.saveCalls).isZero();
    }

    @Test
    void userIsNotModifiedWhenSettingsAreRead() {
        FakeUserRepository userRepository = new FakeUserRepository();
        FakeUserSettingsRepository settingsRepository = new FakeUserSettingsRepository();
        User user = user();
        user.suspend();
        user.block();
        userRepository.save(user);
        userRepository.resetCounters();

        service(userRepository, settingsRepository).getSettings(new GetUserSettingsQuery(5001L));

        User persisted = userRepository.findByTelegramUserId(5001L).orElseThrow();
        assertThat(userRepository.saveCalls).isZero();
        assertThat(persisted.getUsername()).isEqualTo("telegram_user");
        assertThat(persisted.getFirstName()).isEqualTo("Ali");
        assertThat(persisted.getLastName()).isEqualTo("Ahmadi");
        assertThat(persisted.getLanguage()).isEqualTo(UserLanguage.FA);
        assertThat(persisted.getStatus()).isEqualTo(UserStatus.SUSPENDED);
        assertThat(persisted.getBlocked()).isTrue();
        assertThat(persisted.getLastInteractionAt()).isEqualTo(LAST_INTERACTION_AT);
    }

    private GetUserSettingsService service(
            FakeUserRepository userRepository,
            FakeUserSettingsRepository settingsRepository
    ) {
        UserSettingsCreationService creationService = new UserSettingsCreationService(settingsRepository);
        UserSettingsDefaultsService defaultsService = new UserSettingsDefaultsService(settingsRepository, creationService);
        return new GetUserSettingsService(userRepository, defaultsService);
    }

    private User user() {
        User user = User.create(5001L, "@telegram_user", "Ali", "Ahmadi", UserLanguage.FA, LAST_INTERACTION_AT);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(user, "createdAt", CREATED_AT);
        ReflectionTestUtils.setField(user, "updatedAt", UPDATED_AT);
        return user;
    }

    private UserSettings settings(
            UUID userId,
            boolean notificationsEnabled,
            boolean renewalRemindersEnabled,
            boolean usageAlertsEnabled,
            int threshold
    ) {
        UserSettings settings = UserSettings.createDefault(userId);
        settings.updatePreferences(notificationsEnabled, renewalRemindersEnabled, usageAlertsEnabled, threshold);
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
