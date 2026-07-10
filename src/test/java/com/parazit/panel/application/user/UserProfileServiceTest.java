package com.parazit.panel.application.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parazit.panel.application.user.command.UpdateUserProfileCommand;
import com.parazit.panel.application.user.query.GetUserProfileQuery;
import com.parazit.panel.application.user.result.UpdateUserProfileResult;
import com.parazit.panel.application.user.result.UserProfileResult;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.UserLanguage;
import com.parazit.panel.domain.user.UserStatus;
import com.parazit.panel.domain.user.repository.UserRepository;
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

class UserProfileServiceTest {

    private static final Instant REGISTERED_AT = Instant.parse("2026-07-10T12:00:00Z");
    private static final Instant INITIAL_UPDATED_AT = Instant.parse("2026-07-10T12:00:00Z");
    private static final Instant PROFILE_UPDATED_AT = Instant.parse("2026-07-10T12:10:00Z");
    private static final Instant LAST_INTERACTION_AT = Instant.parse("2026-07-10T12:05:00Z");

    @Test
    void retrievesProfile() {
        FakeUserRepository repository = new FakeUserRepository();
        User user = registeredUser();
        repository.save(user);

        UserProfileResult result = new GetUserProfileService(repository)
                .getProfile(new GetUserProfileQuery(5001L));

        assertThat(repository.findByTelegramUserIdCalls).isEqualTo(1);
        assertThat(repository.saveCalls).isEqualTo(1);
        assertThat(result.telegramUserId()).isEqualTo(5001L);
        assertThat(result.username()).isEqualTo("telegram_user");
        assertThat(result.firstName()).isEqualTo("Ali");
        assertThat(result.lastName()).isEqualTo("Ahmadi");
        assertThat(result.language()).isEqualTo(UserLanguage.FA);
        assertThat(result.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(result.blocked()).isFalse();
        assertThat(result.createdAt()).isEqualTo(REGISTERED_AT);
        assertThat(result.updatedAt()).isEqualTo(INITIAL_UPDATED_AT);
        assertThat(result.lastInteractionAt()).isEqualTo(LAST_INTERACTION_AT);
    }

    @Test
    void updatesProfileAndPersistsChanges() {
        FakeUserRepository repository = new FakeUserRepository();
        User user = registeredUser();
        user.suspend();
        user.block();
        repository.save(user);
        repository.resetCounters();

        UpdateUserProfileResult result = new UpdateUserProfileService(repository)
                .updateProfile(new UpdateUserProfileCommand(5001L, " Sara ", "   ", UserLanguage.EN));

        assertThat(repository.findByTelegramUserIdCalls).isEqualTo(1);
        assertThat(repository.saveCalls).isEqualTo(1);
        assertThat(result.telegramUserId()).isEqualTo(5001L);
        assertThat(result.username()).isEqualTo("telegram_user");
        assertThat(result.firstName()).isEqualTo("Sara");
        assertThat(result.lastName()).isNull();
        assertThat(result.language()).isEqualTo(UserLanguage.EN);
        assertThat(result.status()).isEqualTo(UserStatus.SUSPENDED);
        assertThat(result.blocked()).isTrue();
        assertThat(result.createdAt()).isEqualTo(REGISTERED_AT);
        assertThat(result.updatedAt()).isEqualTo(PROFILE_UPDATED_AT);
        assertThat(result.lastInteractionAt()).isEqualTo(LAST_INTERACTION_AT);

        User persisted = repository.findByTelegramUserId(5001L).orElseThrow();
        assertThat(persisted.getTelegramUserId()).isEqualTo(5001L);
        assertThat(persisted.getUsername()).isEqualTo("telegram_user");
        assertThat(persisted.getFirstName()).isEqualTo("Sara");
        assertThat(persisted.getLastName()).isNull();
        assertThat(persisted.getLanguage()).isEqualTo(UserLanguage.EN);
    }

    @Test
    void rejectsInvalidProfileQueries() {
        GetUserProfileService service = new GetUserProfileService(new FakeUserRepository());

        assertThatThrownBy(() -> service.getProfile(null))
                .isInstanceOf(InvalidUserProfileCommandException.class)
                .hasMessage("user profile query must not be null");
        assertThatThrownBy(() -> service.getProfile(new GetUserProfileQuery(null)))
                .isInstanceOf(InvalidUserProfileCommandException.class)
                .hasMessage("telegramUserId must not be null");
        assertThatThrownBy(() -> service.getProfile(new GetUserProfileQuery(0L)))
                .isInstanceOf(InvalidUserProfileCommandException.class)
                .hasMessage("telegramUserId must be positive");
    }

    @Test
    void rejectsInvalidProfileUpdates() {
        UpdateUserProfileService service = new UpdateUserProfileService(new FakeUserRepository());

        assertThatThrownBy(() -> service.updateProfile(null))
                .isInstanceOf(InvalidUserProfileCommandException.class)
                .hasMessage("user profile update command must not be null");
        assertThatThrownBy(() -> service.updateProfile(new UpdateUserProfileCommand(null, "Ali", null, UserLanguage.FA)))
                .isInstanceOf(InvalidUserProfileCommandException.class)
                .hasMessage("telegramUserId must not be null");
        assertThatThrownBy(() -> service.updateProfile(new UpdateUserProfileCommand(0L, "Ali", null, UserLanguage.FA)))
                .isInstanceOf(InvalidUserProfileCommandException.class)
                .hasMessage("telegramUserId must be positive");
        assertThatThrownBy(() -> service.updateProfile(new UpdateUserProfileCommand(5001L, "   ", null, UserLanguage.FA)))
                .isInstanceOf(InvalidUserProfileCommandException.class)
                .hasMessage("firstName must not be blank");
        assertThatThrownBy(() -> service.updateProfile(new UpdateUserProfileCommand(5001L, "Ali", null, null)))
                .isInstanceOf(InvalidUserProfileCommandException.class)
                .hasMessage("language must not be null");
    }

    @Test
    void returnsNotFoundWhenUserDoesNotExist() {
        FakeUserRepository repository = new FakeUserRepository();

        assertThatThrownBy(() -> new GetUserProfileService(repository).getProfile(new GetUserProfileQuery(9999L)))
                .isInstanceOf(UserProfileNotFoundException.class)
                .hasMessage("User profile not found for telegramUserId 9999");
        assertThatThrownBy(() -> new UpdateUserProfileService(repository)
                .updateProfile(new UpdateUserProfileCommand(9999L, "Ali", null, UserLanguage.FA)))
                .isInstanceOf(UserProfileNotFoundException.class)
                .hasMessage("User profile not found for telegramUserId 9999");
    }

    private User registeredUser() {
        User user = User.create(5001L, "@telegram_user", "Ali", "Ahmadi", UserLanguage.FA, LAST_INTERACTION_AT);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(user, "createdAt", REGISTERED_AT);
        ReflectionTestUtils.setField(user, "updatedAt", INITIAL_UPDATED_AT);
        return user;
    }

    private static final class FakeUserRepository implements UserRepository {

        private final Map<Long, User> users = new LinkedHashMap<>();
        private int findByTelegramUserIdCalls;
        private int existsByTelegramUserIdCalls;
        private int saveCalls;

        @Override
        public Optional<User> findByTelegramUserId(Long telegramUserId) {
            findByTelegramUserIdCalls++;
            return Optional.ofNullable(users.get(telegramUserId));
        }

        @Override
        public boolean existsByTelegramUserId(Long telegramUserId) {
            existsByTelegramUserIdCalls++;
            return users.containsKey(telegramUserId);
        }

        @Override
        public Optional<User> findById(UUID id) {
            return users.values()
                    .stream()
                    .filter(user -> id.equals(user.getId()))
                    .findFirst();
        }

        @Override
        public List<User> findAll() {
            return new ArrayList<>(users.values());
        }

        @Override
        public User save(User user) {
            saveCalls++;
            if (user.getId() == null) {
                ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
                ReflectionTestUtils.setField(user, "createdAt", REGISTERED_AT);
                ReflectionTestUtils.setField(user, "updatedAt", INITIAL_UPDATED_AT);
            } else if (saveCalls > 1 || users.containsKey(user.getTelegramUserId())) {
                ReflectionTestUtils.setField(user, "updatedAt", PROFILE_UPDATED_AT);
            }
            users.put(user.getTelegramUserId(), user);
            return user;
        }

        @Override
        public List<User> saveAll(Collection<User> entities) {
            return entities.stream()
                    .map(this::save)
                    .toList();
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
            existsByTelegramUserIdCalls = 0;
            saveCalls = 0;
        }
    }
}
