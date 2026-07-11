package com.parazit.panel.domain.user;

import com.parazit.panel.common.persistence.BaseEntity;
import com.parazit.panel.domain.referral.ReferralCodePolicy;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_users_telegram_user_id", columnNames = "telegram_user_id"),
                @UniqueConstraint(name = "uq_users_referral_code", columnNames = "referral_code")
        },
        indexes = {
                @Index(name = "idx_users_telegram_user_id", columnList = "telegram_user_id"),
                @Index(name = "idx_users_referral_code", columnList = "referral_code"),
                @Index(name = "idx_users_status", columnList = "status"),
                @Index(name = "idx_users_created_at", columnList = "created_at")
        }
)
public class User extends BaseEntity {

    public static final int USERNAME_MAX_LENGTH = 64;
    public static final int FIRST_NAME_MAX_LENGTH = 128;
    public static final int LAST_NAME_MAX_LENGTH = 128;

    @Column(name = "telegram_user_id", nullable = false, updatable = false)
    private Long telegramUserId;

    @Column(name = "referral_code", length = ReferralCodePolicy.MAX_LENGTH)
    private String referralCode;

    @Column(name = "username", length = USERNAME_MAX_LENGTH)
    private String username;

    @Column(name = "first_name", nullable = false, length = FIRST_NAME_MAX_LENGTH)
    private String firstName;

    @Column(name = "last_name", length = LAST_NAME_MAX_LENGTH)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(name = "language", nullable = false, length = 16)
    private UserLanguage language;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private UserStatus status;

    @Column(name = "blocked", nullable = false)
    private Boolean blocked;

    @Column(name = "last_interaction_at")
    private Instant lastInteractionAt;

    protected User() {
    }

    private User(
            Long telegramUserId,
            String username,
            String firstName,
            String lastName,
            UserLanguage language,
            Instant currentTime
    ) {
        this.telegramUserId = requirePositiveTelegramUserId(telegramUserId);
        this.username = normalizeOptionalUsername(username);
        this.firstName = normalizeRequired(firstName, "firstName", FIRST_NAME_MAX_LENGTH);
        this.lastName = normalizeOptional(lastName, "lastName", LAST_NAME_MAX_LENGTH);
        this.language = requireLanguage(language);
        this.status = UserStatus.ACTIVE;
        this.blocked = false;
        this.lastInteractionAt = requireInstant(currentTime, "currentTime");
    }

    public static User create(
            Long telegramUserId,
            String username,
            String firstName,
            String lastName,
            UserLanguage language,
            Instant currentTime
    ) {
        return new User(telegramUserId, username, firstName, lastName, language, currentTime);
    }

    public void updateTelegramProfile(
            String username,
            String firstName,
            String lastName,
            Instant interactionTime
    ) {
        this.username = normalizeOptionalUsername(username);
        this.firstName = normalizeRequired(firstName, "firstName", FIRST_NAME_MAX_LENGTH);
        this.lastName = normalizeOptional(lastName, "lastName", LAST_NAME_MAX_LENGTH);
        recordInteraction(interactionTime);
    }

    public void updateProfile(
            String firstName,
            String lastName
    ) {
        this.firstName = normalizeRequired(firstName, "firstName", FIRST_NAME_MAX_LENGTH);
        this.lastName = normalizeOptional(lastName, "lastName", LAST_NAME_MAX_LENGTH);
    }

    public void changeLanguage(UserLanguage language) {
        this.language = requireLanguage(language);
    }

    public void activate() {
        this.status = UserStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = UserStatus.INACTIVE;
    }

    public void suspend() {
        this.status = UserStatus.SUSPENDED;
    }

    public void block() {
        this.blocked = true;
    }

    public void unblock() {
        this.blocked = false;
    }

    public void recordInteraction(Instant interactionTime) {
        this.lastInteractionAt = requireInstant(interactionTime, "interactionTime");
    }

    public void assignReferralCode(String referralCode) {
        String normalized = ReferralCodePolicy.normalizeAndValidate(referralCode);
        if (this.referralCode == null) {
            this.referralCode = normalized;
            return;
        }
        if (!this.referralCode.equals(normalized)) {
            throw new IllegalStateException("referralCode cannot be reassigned");
        }
    }

    public Long getTelegramUserId() {
        return telegramUserId;
    }

    public String getReferralCode() {
        return referralCode;
    }

    public String getUsername() {
        return username;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public UserLanguage getLanguage() {
        return language;
    }

    public UserStatus getStatus() {
        return status;
    }

    public Boolean getBlocked() {
        return blocked;
    }

    public Instant getLastInteractionAt() {
        return lastInteractionAt;
    }

    private static Long requirePositiveTelegramUserId(Long telegramUserId) {
        Objects.requireNonNull(telegramUserId, "telegramUserId must not be null");
        if (telegramUserId <= 0) {
            throw new IllegalArgumentException("telegramUserId must be positive");
        }
        return telegramUserId;
    }

    private static UserLanguage requireLanguage(UserLanguage language) {
        return Objects.requireNonNull(language, "language must not be null");
    }

    private static Instant requireInstant(Instant instant, String fieldName) {
        return Objects.requireNonNull(instant, fieldName + " must not be null");
    }

    private static String normalizeOptionalUsername(String username) {
        if (username == null) {
            return null;
        }

        String normalized = username.trim();
        while (normalized.startsWith("@")) {
            normalized = normalized.substring(1).trim();
        }
        if (normalized.isBlank()) {
            return null;
        }
        if (normalized.length() > USERNAME_MAX_LENGTH) {
            throw new IllegalArgumentException("username must be at most " + USERNAME_MAX_LENGTH + " characters");
        }
        return normalized;
    }

    private static String normalizeRequired(String value, String fieldName, int maxLength) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " must be at most " + maxLength + " characters");
        }
        return normalized;
    }

    private static String normalizeOptional(String value, String fieldName, int maxLength) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        if (normalized.isBlank()) {
            return null;
        }
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " must be at most " + maxLength + " characters");
        }
        return normalized;
    }
}
