package com.parazit.panel.application.wallet.payment;

public record WalletOrderPaymentEligibility(
        boolean eligible,
        WalletOrderPaymentEligibilityReason reason
) {

    public static WalletOrderPaymentEligibility allowed() {
        return new WalletOrderPaymentEligibility(true, WalletOrderPaymentEligibilityReason.ELIGIBLE);
    }

    public static WalletOrderPaymentEligibility rejected(WalletOrderPaymentEligibilityReason reason) {
        return new WalletOrderPaymentEligibility(false, reason);
    }
}
