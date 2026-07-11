package com.parazit.panel.test.fixture;

import com.parazit.panel.domain.user.User;
import com.parazit.panel.domain.user.repository.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.test.util.ReflectionTestUtils;

public class FakeUserRepository implements UserRepository {

    public static final Instant CREATED_AT = Instant.parse("2026-07-10T12:00:00Z");
    public static final Instant UPDATED_AT = Instant.parse("2026-07-10T12:05:00Z");

    private final Map<UUID, User> usersById = new LinkedHashMap<>();

    public int findByTelegramUserIdCalls;
    public int saveCalls;
    public int deleteCalls;

    @Override
    public Optional<User> findByTelegramUserId(Long telegramUserId) {
        findByTelegramUserIdCalls++;
        return usersById.values()
                .stream()
                .filter(user -> user.getTelegramUserId().equals(telegramUserId))
                .findFirst();
    }

    @Override
    public Optional<User> findByTelegramUserIdForUpdate(Long telegramUserId) {
        return findByTelegramUserId(telegramUserId);
    }

    @Override
    public boolean existsByTelegramUserId(Long telegramUserId) {
        return findByTelegramUserId(telegramUserId).isPresent();
    }

    @Override
    public Optional<User> findByReferralCode(String referralCode) {
        return usersById.values()
                .stream()
                .filter(user -> referralCode.equals(user.getReferralCode()))
                .findFirst();
    }

    @Override
    public boolean existsByReferralCode(String referralCode) {
        return findByReferralCode(referralCode).isPresent();
    }

    @Override
    public Optional<User> findById(UUID id) {
        return Optional.ofNullable(usersById.get(id));
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(usersById.values());
    }

    @Override
    public User save(User user) {
        saveCalls++;
        if (user.getId() == null) {
            ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
            ReflectionTestUtils.setField(user, "createdAt", CREATED_AT);
            ReflectionTestUtils.setField(user, "updatedAt", CREATED_AT);
        } else {
            ReflectionTestUtils.setField(user, "updatedAt", UPDATED_AT);
        }
        usersById.put(user.getId(), user);
        return user;
    }

    @Override
    public List<User> saveAll(Collection<User> entities) {
        return entities.stream().map(this::save).toList();
    }

    @Override
    public boolean existsById(UUID id) {
        return usersById.containsKey(id);
    }

    @Override
    public long count() {
        return usersById.size();
    }

    @Override
    public void delete(User entity) {
        deleteCalls++;
        usersById.remove(entity.getId());
    }

    @Override
    public void deleteById(UUID id) {
        deleteCalls++;
        usersById.remove(id);
    }
}
