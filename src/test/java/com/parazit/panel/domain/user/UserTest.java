package com.parazit.panel.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class UserTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void createsUserWithDefaults() {
        User user = User.create(1001L, "telegram_user", "Ali", "Ahmadi", UserLanguage.FA, NOW);

        assertThat(user.getTelegramUserId()).isEqualTo(1001L);
        assertThat(user.getUsername()).isEqualTo("telegram_user");
        assertThat(user.getFirstName()).isEqualTo("Ali");
        assertThat(user.getLastName()).isEqualTo("Ahmadi");
        assertThat(user.getLanguage()).isEqualTo(UserLanguage.FA);
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getBlocked()).isFalse();
        assertThat(user.getLastInteractionAt()).isEqualTo(NOW);
    }

    @Test
    void normalizesTelegramProfileValues() {
        User user = User.create(1001L, " @telegram_user ", " Ali ", " Ahmadi ", UserLanguage.FA, NOW);

        assertThat(user.getUsername()).isEqualTo("telegram_user");
        assertThat(user.getFirstName()).isEqualTo("Ali");
        assertThat(user.getLastName()).isEqualTo("Ahmadi");
    }

    @Test
    void storesBlankOptionalValuesAsNull() {
        User user = User.create(1001L, "   ", "Ali", "   ", UserLanguage.FA, NOW);

        assertThat(user.getUsername()).isNull();
        assertThat(user.getLastName()).isNull();
    }

    @Test
    void rejectsInvalidTelegramUserId() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> User.create(0L, null, "Ali", null, UserLanguage.FA, NOW))
                .withMessage("telegramUserId must be positive");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> User.create(-1L, null, "Ali", null, UserLanguage.FA, NOW))
                .withMessage("telegramUserId must be positive");
        assertThatNullPointerException()
                .isThrownBy(() -> User.create(null, null, "Ali", null, UserLanguage.FA, NOW))
                .withMessage("telegramUserId must not be null");
    }

    @Test
    void rejectsBlankFirstName() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> User.create(1001L, null, "   ", null, UserLanguage.FA, NOW))
                .withMessage("firstName must not be blank");
    }

    @Test
    void rejectsNullRequiredValues() {
        assertThatNullPointerException()
                .isThrownBy(() -> User.create(1001L, null, null, null, UserLanguage.FA, NOW))
                .withMessage("firstName must not be null");
        assertThatNullPointerException()
                .isThrownBy(() -> User.create(1001L, null, "Ali", null, null, NOW))
                .withMessage("language must not be null");
        assertThatNullPointerException()
                .isThrownBy(() -> User.create(1001L, null, "Ali", null, UserLanguage.FA, null))
                .withMessage("currentTime must not be null");
    }

    @Test
    void changesLanguage() {
        User user = createUser();

        user.changeLanguage(UserLanguage.EN);

        assertThat(user.getLanguage()).isEqualTo(UserLanguage.EN);
    }

    @Test
    void rejectsNullLanguageChange() {
        User user = createUser();

        assertThatNullPointerException()
                .isThrownBy(() -> user.changeLanguage(null))
                .withMessage("language must not be null");
    }

    @Test
    void changesStatusExplicitly() {
        User user = createUser();

        user.deactivate();
        assertThat(user.getStatus()).isEqualTo(UserStatus.INACTIVE);

        user.suspend();
        assertThat(user.getStatus()).isEqualTo(UserStatus.SUSPENDED);

        user.activate();
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void blocksAndUnblocksUser() {
        User user = createUser();

        user.block();
        assertThat(user.getBlocked()).isTrue();

        user.unblock();
        assertThat(user.getBlocked()).isFalse();
    }

    @Test
    void recordsInteractionTimestamp() {
        User user = createUser();
        Instant later = NOW.plusSeconds(60);

        user.recordInteraction(later);

        assertThat(user.getLastInteractionAt()).isEqualTo(later);
    }

    @Test
    void rejectsNullInteractionTimestamp() {
        User user = createUser();

        assertThatNullPointerException()
                .isThrownBy(() -> user.recordInteraction(null))
                .withMessage("interactionTime must not be null");
    }

    @Test
    void updatesTelegramProfileAndInteractionTimestamp() {
        User user = createUser();
        Instant later = NOW.plusSeconds(60);

        user.updateTelegramProfile("@new_username", " Sara ", "   ", later);

        assertThat(user.getUsername()).isEqualTo("new_username");
        assertThat(user.getFirstName()).isEqualTo("Sara");
        assertThat(user.getLastName()).isNull();
        assertThat(user.getLastInteractionAt()).isEqualTo(later);
    }

    @Test
    void updatesProfileWithoutChangingTelegramIdentityUsernameStatusOrBlockedState() {
        User user = createUser();
        user.suspend();
        user.block();

        user.updateProfile(" Sara ", "   ", UserLanguage.EN);

        assertThat(user.getTelegramUserId()).isEqualTo(1001L);
        assertThat(user.getUsername()).isEqualTo("telegram_user");
        assertThat(user.getFirstName()).isEqualTo("Sara");
        assertThat(user.getLastName()).isNull();
        assertThat(user.getLanguage()).isEqualTo(UserLanguage.EN);
        assertThat(user.getStatus()).isEqualTo(UserStatus.SUSPENDED);
        assertThat(user.getBlocked()).isTrue();
    }

    @Test
    void rejectsInvalidProfileUpdate() {
        User user = createUser();

        assertThatIllegalArgumentException()
                .isThrownBy(() -> user.updateProfile("   ", null, UserLanguage.FA))
                .withMessage("firstName must not be blank");
        assertThatNullPointerException()
                .isThrownBy(() -> user.updateProfile("Ali", null, null))
                .withMessage("language must not be null");
    }

    @Test
    void doesNotAllowTelegramUserIdMutationThroughDomainOperations() {
        User user = createUser();
        Long originalTelegramUserId = user.getTelegramUserId();

        user.updateTelegramProfile("new_username", "Sara", "Karimi", NOW.plusSeconds(60));
        user.changeLanguage(UserLanguage.EN);
        user.deactivate();
        user.block();
        user.recordInteraction(NOW.plusSeconds(120));

        assertThat(user.getTelegramUserId()).isEqualTo(originalTelegramUserId);
    }

    private User createUser() {
        return User.create(1001L, "@telegram_user", "Ali", "Ahmadi", UserLanguage.FA, NOW);
    }
}
