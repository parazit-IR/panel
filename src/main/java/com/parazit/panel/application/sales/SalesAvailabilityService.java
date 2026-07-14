package com.parazit.panel.application.sales;

import com.parazit.panel.application.port.out.SystemClockPort;
import com.parazit.panel.application.port.out.payment.PaymentProcessor;
import com.parazit.panel.config.properties.ManualPaymentProperties;
import com.parazit.panel.config.properties.SalesControlProperties;
import com.parazit.panel.config.properties.WalletPaymentProperties;
import com.parazit.panel.config.properties.ZarinpalProperties;
import com.parazit.panel.domain.payment.PaymentMethod;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class SalesAvailabilityService {

    private final SalesControlProperties sales;
    private final ManualPaymentProperties manualPayment;
    private final ZarinpalProperties zarinpal;
    private final WalletPaymentProperties walletPayment;
    private final Set<PaymentMethod> supportedPaymentMethods;
    private final SystemClockPort clock;

    public SalesAvailabilityService(
            SalesControlProperties sales,
            ManualPaymentProperties manualPayment,
            ZarinpalProperties zarinpal,
            WalletPaymentProperties walletPayment,
            List<PaymentProcessor> paymentProcessors,
            SystemClockPort clock
    ) {
        this.sales = Objects.requireNonNull(sales, "sales must not be null");
        this.manualPayment = Objects.requireNonNull(manualPayment, "manualPayment must not be null");
        this.zarinpal = Objects.requireNonNull(zarinpal, "zarinpal must not be null");
        this.walletPayment = Objects.requireNonNull(walletPayment, "walletPayment must not be null");
        this.supportedPaymentMethods = supportedMethods(paymentProcessors);
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public SalesCapabilityAvailability availability(SalesCapability capability) {
        Objects.requireNonNull(capability, "capability must not be null");
        Instant now = clock.now();
        return switch (capability) {
            case NEW_PURCHASE -> availability(capability, true, sales.newPurchaseEnabled(), "NEW_PURCHASE_DISABLED", "telegram.purchase.disabled", now);
            case MANUAL_PAYMENT -> availability(capability, true, manualPaymentAvailable(), "MANUAL_PAYMENT_DISABLED", "telegram.purchase.manual_payment_disabled", now);
            case ONLINE_PAYMENT -> availability(capability, true, onlinePaymentAvailable(), "ONLINE_PAYMENT_DISABLED", "telegram.purchase.online_payment_disabled", now);
            case RENEWAL -> availability(capability, true, sales.renewalEnabled(), "RENEWAL_UNAVAILABLE", "telegram.feature.renewal_unavailable", now);
            case TRIAL -> availability(capability, true, sales.trialEnabled(), "TRIAL_UNAVAILABLE", "telegram.feature.trial_unavailable", now);
            case WALLET_PAYMENT -> availability(capability, true, walletPaymentAvailable(), "WALLET_UNAVAILABLE", "telegram.feature.wallet_unavailable", now);
            case DISCOUNT_CODE -> availability(capability, true, sales.discountCodeEnabled(), "DISCOUNT_UNAVAILABLE", "telegram.purchase.discount_unavailable", now);
            case GIFT_CODE -> availability(capability, true, sales.giftCodeEnabled(), "GIFT_CODE_UNAVAILABLE", "telegram.promotion.invalid_gift_code", now);
        };
    }

    public boolean newPurchaseAvailable() {
        return availability(SalesCapability.NEW_PURCHASE).enabled();
    }

    public boolean manualPaymentAvailable() {
        return sales.manualPaymentEnabled()
                && manualPayment.enabled()
                && supportedPaymentMethods.contains(PaymentMethod.CARD_TO_CARD);
    }

    public boolean onlinePaymentAvailable() {
        return sales.onlinePaymentEnabled()
                && zarinpal.enabled()
                && supportedPaymentMethods.contains(PaymentMethod.ZARINPAL);
    }

    public boolean walletPaymentAvailable() {
        return sales.walletPaymentEnabled() && walletPayment.enabled();
    }

    public Instant salesResumeAt() {
        return sales.salesResumeAt();
    }

    private SalesCapabilityAvailability availability(
            SalesCapability capability,
            boolean visible,
            boolean enabled,
            String reason,
            String messageKey,
            Instant now
    ) {
        return new SalesCapabilityAvailability(capability, visible, visible && enabled, enabled ? "" : reason, enabled ? "" : messageKey, now);
    }

    private static Set<PaymentMethod> supportedMethods(List<PaymentProcessor> processors) {
        EnumSet<PaymentMethod> methods = EnumSet.noneOf(PaymentMethod.class);
        for (PaymentProcessor processor : Objects.requireNonNull(processors, "paymentProcessors must not be null")) {
            methods.add(processor.supportedMethod());
        }
        return Set.copyOf(methods);
    }
}
