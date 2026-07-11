package com.parazit.panel.config.referral;

import com.parazit.panel.application.port.out.referral.ReferralCodeGenerator;
import com.parazit.panel.config.properties.ReferralProperties;
import com.parazit.panel.infrastructure.referral.SecureReferralCodeGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ReferralConfiguration {

    @Bean
    public ReferralCodeGenerator referralCodeGenerator(ReferralProperties properties) {
        return new SecureReferralCodeGenerator(properties.codeLength());
    }
}
