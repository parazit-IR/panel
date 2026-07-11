package com.parazit.panel.infrastructure.referral;

import com.parazit.panel.domain.referral.ReferralCodePolicy;
import java.security.SecureRandom;
import java.util.Objects;

import com.parazit.panel.application.port.out.referral.ReferralCodeGenerator;

public class SecureReferralCodeGenerator implements ReferralCodeGenerator {

    private final SecureRandom secureRandom;
    private final int length;

    public SecureReferralCodeGenerator(int length) {
        this(new SecureRandom(), length);
    }

    SecureReferralCodeGenerator(SecureRandom secureRandom, int length) {
        this.secureRandom = Objects.requireNonNull(secureRandom, "secureRandom must not be null");
        if (length < ReferralCodePolicy.MIN_LENGTH || length > ReferralCodePolicy.MAX_LENGTH) {
            throw new IllegalArgumentException("referral code length must be between 8 and 16");
        }
        this.length = length;
    }

    @Override
    public String generate() {
        StringBuilder code = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = secureRandom.nextInt(ReferralCodePolicy.ALPHABET.length());
            code.append(ReferralCodePolicy.ALPHABET.charAt(index));
        }
        return code.toString();
    }
}
