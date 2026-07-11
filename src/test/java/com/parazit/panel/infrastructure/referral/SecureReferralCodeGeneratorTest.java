package com.parazit.panel.infrastructure.referral;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.parazit.panel.domain.referral.ReferralCodePolicy;
import org.junit.jupiter.api.Test;

class SecureReferralCodeGeneratorTest {

    @Test
    void generatesConfiguredNonAmbiguousUppercaseCode() {
        SecureReferralCodeGenerator generator = new SecureReferralCodeGenerator(10);

        String first = generator.generate();
        String second = generator.generate();

        assertThat(first).hasSize(10).matches(ReferralCodePolicy.REGEX);
        assertThat(first).doesNotContain("0", "O", "1", "I");
        assertThat(first).isUpperCase();
        assertThat(second).hasSize(10).matches(ReferralCodePolicy.REGEX);
    }

    @Test
    void generallyGeneratesDifferentCodes() {
        SecureReferralCodeGenerator generator = new SecureReferralCodeGenerator(10);

        assertThat(generator.generate()).isNotEqualTo(generator.generate());
    }

    @Test
    void rejectsInvalidLength() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SecureReferralCodeGenerator(7))
                .withMessage("referral code length must be between 8 and 16");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SecureReferralCodeGenerator(17))
                .withMessage("referral code length must be between 8 and 16");
    }
}
