package com.parazit.panel.application.port.out.payment.manual;

public interface ManualPaymentSuffixGenerator {

    long generate(long minimumInclusive, long maximumInclusive);
}
