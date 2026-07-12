package com.parazit.panel.domain.payment.manual.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ManualPaymentReviewTest {

    @Test
    void claimReleaseApproveAndRejectTransitionsAreControlled() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        ManualPaymentReview review = ManualPaymentReview.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                101_638L,
                101_638L,
                true
        );

        review.claim("operator-a", now);
        assertThat(review.getStatus()).isEqualTo(ManualPaymentReviewStatus.CLAIMED);
        assertThat(review.isClaimExpired(now.plus(Duration.ofMinutes(16)), Duration.ofMinutes(15))).isTrue();

        review.release("operator-a");
        review.claim("operator-b", now.plusSeconds(1));
        review.approve("operator-b", "confirmed", now.plusSeconds(2));

        assertThat(review.getStatus()).isEqualTo(ManualPaymentReviewStatus.APPROVED);
        assertThat(review.getReviewerId()).isEqualTo("operator-b");
        assertThat(review.isAmountMatched()).isTrue();
        assertThat(review.isDuplicateHashDetected()).isTrue();
    }

    @Test
    void anotherOperatorCannotDecideClaimedReviewAndReasonIsRequiredForRejection() {
        ManualPaymentReview review = ManualPaymentReview.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                100_000L,
                99_999L,
                false
        );
        review.claim("operator-a", Instant.parse("2026-01-01T00:00:00Z"));

        assertThatThrownBy(() -> review.approve("operator-b", null, Instant.now()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("another operator");
        assertThatThrownBy(() -> review.reject("operator-a", null, "bad", Instant.now()))
                .isInstanceOf(NullPointerException.class);
        assertThat(review.isAmountMatched()).isFalse();
    }
}
