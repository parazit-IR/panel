package com.parazit.panel.infrastructure.xui.client;

import com.parazit.panel.application.port.out.xui.XuiSubscriptionIdGenerator;
import com.parazit.panel.infrastructure.xui.config.XuiProperties;
import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class SecureXuiSubscriptionIdGenerator implements XuiSubscriptionIdGenerator {

    private static final char[] ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789".toCharArray();

    private final SecureRandom secureRandom;
    private final int length;

    public SecureXuiSubscriptionIdGenerator(XuiProperties properties) {
        this.secureRandom = new SecureRandom();
        int length = properties.subscriptionIdLength();
        if (length < 8 || length > 64) {
            throw new IllegalArgumentException("subscriptionIdLength must be between 8 and 64");
        }
        this.length = length;
    }

    @Override
    public String generate() {
        char[] chars = new char[length];
        for (int index = 0; index < length; index++) {
            chars[index] = ALPHABET[secureRandom.nextInt(ALPHABET.length)];
        }
        return new String(chars);
    }
}
