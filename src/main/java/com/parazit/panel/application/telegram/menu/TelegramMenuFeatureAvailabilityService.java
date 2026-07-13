package com.parazit.panel.application.telegram.menu;

import com.parazit.panel.config.properties.PaymentProperties;
import com.parazit.panel.config.properties.SubscriptionProperties;
import com.parazit.panel.config.properties.TelegramFeaturePlaceholderProperties;
import com.parazit.panel.config.properties.TelegramMenuProperties;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class TelegramMenuFeatureAvailabilityService {

    private final TelegramMenuProperties menuProperties;
    private final TelegramFeaturePlaceholderProperties placeholderProperties;
    private final PaymentProperties paymentProperties;
    private final SubscriptionProperties subscriptionProperties;

    public TelegramMenuFeatureAvailabilityService(
            TelegramMenuProperties menuProperties,
            TelegramFeaturePlaceholderProperties placeholderProperties,
            PaymentProperties paymentProperties,
            SubscriptionProperties subscriptionProperties
    ) {
        this.menuProperties = Objects.requireNonNull(menuProperties, "menuProperties must not be null");
        this.placeholderProperties = Objects.requireNonNull(placeholderProperties, "placeholderProperties must not be null");
        this.paymentProperties = Objects.requireNonNull(paymentProperties, "paymentProperties must not be null");
        this.subscriptionProperties = Objects.requireNonNull(subscriptionProperties, "subscriptionProperties must not be null");
    }

    public TelegramMenuFeatureAvailability availability(TelegramMainMenuAction action) {
        Objects.requireNonNull(action, "action must not be null");
        if (!menuProperties.enabled()) {
            return TelegramMenuFeatureAvailability.hidden();
        }
        return switch (action) {
            case BUY_SUBSCRIPTION -> paymentProperties.enabled()
                    ? TelegramMenuFeatureAvailability.available()
                    : TelegramMenuFeatureAvailability.unavailable("telegram.feature.purchase_unavailable");
            case RENEW_SERVICE -> menuProperties.showRenewal()
                    ? availability(placeholderProperties.renewalAvailable(), "telegram.feature.renewal_unavailable")
                    : TelegramMenuFeatureAvailability.hidden();
            case MY_SERVICES -> subscriptionProperties.enabled()
                    ? TelegramMenuFeatureAvailability.available()
                    : TelegramMenuFeatureAvailability.unavailable("telegram.feature.subscription_unavailable");
            case REQUEST_TRIAL -> menuProperties.showTrial()
                    ? availability(placeholderProperties.trialAvailable(), "telegram.feature.trial_unavailable")
                    : TelegramMenuFeatureAvailability.hidden();
            case SHOW_TARIFFS -> menuProperties.showTariffs()
                    ? TelegramMenuFeatureAvailability.available()
                    : TelegramMenuFeatureAvailability.hidden();
            case SHOW_WALLET -> menuProperties.showWallet()
                    ? availability(placeholderProperties.walletAvailable(), "telegram.feature.wallet_unavailable")
                    : TelegramMenuFeatureAvailability.hidden();
            case SHOW_TUTORIALS -> menuProperties.showTutorials()
                    ? TelegramMenuFeatureAvailability.available()
                    : TelegramMenuFeatureAvailability.hidden();
            case SHOW_SUPPORT -> menuProperties.showSupport()
                    ? TelegramMenuFeatureAvailability.available()
                    : TelegramMenuFeatureAvailability.hidden();
        };
    }

    private TelegramMenuFeatureAvailability availability(boolean enabled, String unavailableKey) {
        return enabled ? TelegramMenuFeatureAvailability.available() : TelegramMenuFeatureAvailability.unavailable(unavailableKey);
    }
}
