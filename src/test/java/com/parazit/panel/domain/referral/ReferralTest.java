package com.parazit.panel.domain.referral;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReferralTest {

    private static final UUID REFERRER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID REFERRED_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final Instant NOW = Instant.parse("2026-07-10T12:00:00Z");

    @Test
    void createsPendingReferral() {
        Referral referral = Referral.create(REFERRER_ID, REFERRED_ID, "abcd2345", NOW);

        assertThat(referral.getReferrerUserId()).isEqualTo(REFERRER_ID);
        assertThat(referral.getReferredUserId()).isEqualTo(REFERRED_ID);
        assertThat(referral.getReferralCodeUsed()).isEqualTo("ABCD2345");
        assertThat(referral.getStatus()).isEqualTo(ReferralStatus.PENDING);
        assertThat(referral.getReferredAt()).isEqualTo(NOW);
    }

    @Test
    void rejectsInvalidCreationInput() {
        assertThatNullPointerException()
                .isThrownBy(() -> Referral.create(null, REFERRED_ID, "ABCD2345", NOW))
                .withMessage("referrerUserId must not be null");
        assertThatNullPointerException()
                .isThrownBy(() -> Referral.create(REFERRER_ID, null, "ABCD2345", NOW))
                .withMessage("referredUserId must not be null");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Referral.create(REFERRER_ID, REFERRER_ID, "ABCD2345", NOW))
                .withMessage("a user cannot refer themselves");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Referral.create(REFERRER_ID, REFERRED_ID, "   ", NOW))
                .withMessage("referralCode must not be blank");
        assertThatNullPointerException()
                .isThrownBy(() -> Referral.create(REFERRER_ID, REFERRED_ID, "ABCD2345", null))
                .withMessage("referredAt must not be null");
    }

    @Test
    void supportsExplicitPendingTransitionsOnly() {
        Referral confirmed = Referral.create(REFERRER_ID, REFERRED_ID, "ABCD2345", NOW);
        confirmed.confirm();
        assertThat(confirmed.getStatus()).isEqualTo(ReferralStatus.CONFIRMED);

        Referral cancelled = Referral.create(REFERRER_ID, REFERRED_ID, "ABCD2345", NOW);
        cancelled.cancel();
        assertThat(cancelled.getStatus()).isEqualTo(ReferralStatus.CANCELLED);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> Referral.create(REFERRER_ID, REFERRED_ID, "ABC01I", NOW));
    }
}
