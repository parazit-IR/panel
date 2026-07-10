package com.parazit.panel.domain.user.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserSettingsTest {

    @Test
    void createsDefaultSettings() {
        UUID userId = UUID.randomUUID();

        UserSettings settings = UserSettings.createDefault(userId);

        assertThat(settings.getUserId()).isEqualTo(userId);
        assertThat(settings.isNotificationsEnabled()).isTrue();
        assertThat(settings.isRenewalRemindersEnabled()).isTrue();
        assertThat(settings.isUsageAlertsEnabled()).isTrue();
        assertThat(settings.getUsageAlertThresholdPercent()).isEqualTo(80);
    }

    @Test
    void rejectsNullUserId() {
        assertThatNullPointerException()
                .isThrownBy(() -> UserSettings.createDefault(null))
                .withMessage("userId must not be null");
    }

    @Test
    void rejectsThresholdBelowOne() {
        UserSettings settings = UserSettings.createDefault(UUID.randomUUID());

        assertThatIllegalArgumentException()
                .isThrownBy(() -> settings.changeUsageAlertThresholdPercent(0))
                .withMessage("usageAlertThresholdPercent must be between 1 and 100");
    }

    @Test
    void rejectsThresholdAboveOneHundred() {
        UserSettings settings = UserSettings.createDefault(UUID.randomUUID());

        assertThatIllegalArgumentException()
                .isThrownBy(() -> settings.changeUsageAlertThresholdPercent(101))
                .withMessage("usageAlertThresholdPercent must be between 1 and 100");
    }

    @Test
    void enablesAndDisablesNotifications() {
        UserSettings settings = UserSettings.createDefault(UUID.randomUUID());

        settings.disableNotifications();
        assertThat(settings.isNotificationsEnabled()).isFalse();

        settings.enableNotifications();
        assertThat(settings.isNotificationsEnabled()).isTrue();
    }

    @Test
    void changesRenewalReminderPreference() {
        UserSettings settings = UserSettings.createDefault(UUID.randomUUID());

        settings.setRenewalRemindersEnabled(false);

        assertThat(settings.isRenewalRemindersEnabled()).isFalse();
    }

    @Test
    void changesUsageAlertPreference() {
        UserSettings settings = UserSettings.createDefault(UUID.randomUUID());

        settings.setUsageAlertsEnabled(false);

        assertThat(settings.isUsageAlertsEnabled()).isFalse();
    }

    @Test
    void changesThreshold() {
        UserSettings settings = UserSettings.createDefault(UUID.randomUUID());

        settings.changeUsageAlertThresholdPercent(25);

        assertThat(settings.getUsageAlertThresholdPercent()).isEqualTo(25);
    }

    @Test
    void updatesAllPreferences() {
        UserSettings settings = UserSettings.createDefault(UUID.randomUUID());

        settings.updatePreferences(false, false, false, 55);

        assertThat(settings.isNotificationsEnabled()).isFalse();
        assertThat(settings.isRenewalRemindersEnabled()).isFalse();
        assertThat(settings.isUsageAlertsEnabled()).isFalse();
        assertThat(settings.getUsageAlertThresholdPercent()).isEqualTo(55);
    }

    @Test
    void sameValueUpdateSucceeds() {
        UserSettings settings = UserSettings.createDefault(UUID.randomUUID());

        settings.updatePreferences(true, true, true, 80);

        assertThat(settings.isNotificationsEnabled()).isTrue();
        assertThat(settings.isRenewalRemindersEnabled()).isTrue();
        assertThat(settings.isUsageAlertsEnabled()).isTrue();
        assertThat(settings.getUsageAlertThresholdPercent()).isEqualTo(80);
    }
}
