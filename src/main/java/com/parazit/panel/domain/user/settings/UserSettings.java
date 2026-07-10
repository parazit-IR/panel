package com.parazit.panel.domain.user.settings;

import com.parazit.panel.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "user_settings",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_user_settings_user_id", columnNames = "user_id")
        },
        indexes = {
                @Index(name = "idx_user_settings_user_id", columnList = "user_id")
        }
)
public class UserSettings extends BaseEntity {

    public static final int DEFAULT_USAGE_ALERT_THRESHOLD_PERCENT = 80;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "notifications_enabled", nullable = false)
    private boolean notificationsEnabled;

    @Column(name = "renewal_reminders_enabled", nullable = false)
    private boolean renewalRemindersEnabled;

    @Column(name = "usage_alerts_enabled", nullable = false)
    private boolean usageAlertsEnabled;

    @Column(name = "usage_alert_threshold_percent", nullable = false)
    private Integer usageAlertThresholdPercent;

    protected UserSettings() {
    }

    private UserSettings(UUID userId) {
        this.userId = requireUserId(userId);
        this.notificationsEnabled = true;
        this.renewalRemindersEnabled = true;
        this.usageAlertsEnabled = true;
        this.usageAlertThresholdPercent = DEFAULT_USAGE_ALERT_THRESHOLD_PERCENT;
    }

    public static UserSettings createDefault(UUID userId) {
        return new UserSettings(userId);
    }

    public void enableNotifications() {
        this.notificationsEnabled = true;
    }

    public void disableNotifications() {
        this.notificationsEnabled = false;
    }

    public void setRenewalRemindersEnabled(boolean enabled) {
        this.renewalRemindersEnabled = enabled;
    }

    public void setUsageAlertsEnabled(boolean enabled) {
        this.usageAlertsEnabled = enabled;
    }

    public void changeUsageAlertThresholdPercent(int thresholdPercent) {
        this.usageAlertThresholdPercent = requireThresholdPercent(thresholdPercent);
    }

    public void updatePreferences(
            boolean notificationsEnabled,
            boolean renewalRemindersEnabled,
            boolean usageAlertsEnabled,
            int usageAlertThresholdPercent
    ) {
        this.notificationsEnabled = notificationsEnabled;
        this.renewalRemindersEnabled = renewalRemindersEnabled;
        this.usageAlertsEnabled = usageAlertsEnabled;
        this.usageAlertThresholdPercent = requireThresholdPercent(usageAlertThresholdPercent);
    }

    public UUID getUserId() {
        return userId;
    }

    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    public boolean isRenewalRemindersEnabled() {
        return renewalRemindersEnabled;
    }

    public boolean isUsageAlertsEnabled() {
        return usageAlertsEnabled;
    }

    public Integer getUsageAlertThresholdPercent() {
        return usageAlertThresholdPercent;
    }

    private static UUID requireUserId(UUID userId) {
        return Objects.requireNonNull(userId, "userId must not be null");
    }

    private static Integer requireThresholdPercent(int thresholdPercent) {
        if (thresholdPercent < 1 || thresholdPercent > 100) {
            throw new IllegalArgumentException("usageAlertThresholdPercent must be between 1 and 100");
        }
        return thresholdPercent;
    }
}
