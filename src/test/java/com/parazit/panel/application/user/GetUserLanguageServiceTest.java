package com.parazit.panel.application.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parazit.panel.application.user.query.GetUserLanguageQuery;
import com.parazit.panel.application.user.result.UserLanguageResult;
import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.UserLanguage;
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

class GetUserLanguageServiceTest {

    private static final Instant CREATED_AT = Instant.parse("2026-07-10T12:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-07-10T12:05:00Z");
    private static final Instant LAST_INTERACTION_AT = Instant.parse("2026-07-10T12:03:00Z");

    @Test
    void existingUserReturnsLanguage() {
        FakeUserRepository repository = new FakeUserRepository();
        User user = user(UserLanguage.EN);
        repository.save(user);
        repository.resetCounters();

        UserLanguageResult result = new GetUserLanguageService(repository)
                .getLanguage(new GetUserLanguageQuery(5001L));

        assertThat(repository.findByTelegramUserIdCalls).isEqualTo(1);
        assertThat(repository.saveCalls).isZero();
        assertThat(result.userId()).isEqualTo(user.getId());
        assertThat(result.telegramUserId()).isEqualTo(5001L);
        assertThat(result.language()).isEqualTo(UserLanguage.EN);
        assertThat(result.updatedAt()).isEqualTo(UPDATED_AT);
        assertThat(user.getLastInteractionAt()).isEqualTo(LAST_INTERACTION_AT);
    }

    @Test
    void missingUserThrowsUserNotFoundException() {
        FakeUserRepository repository = new FakeUserRepository();

        assertThatThrownBy(() -> new GetUserLanguageService(repository)
                .getLanguage(new GetUserLanguageQuery(9999L)))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found for telegramUserId 9999");

        assertThat(repository.saveCalls).isZero();
    }

    private User user(UserLanguage language) {
        User user = User.create(5001L, "@telegram_user", "Ali", "Ahmadi", language, LAST_INTERACTION_AT);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(user, "createdAt", CREATED_AT);
        ReflectionTestUtils.setField(user, "updatedAt", UPDATED_AT);
        return user;
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
            saveCalls = 0;
        }
    }
}
