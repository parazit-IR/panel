package com.parazit.panel.application.xui.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.parazit.panel.domain.xui.operation.XuiClientOperationType;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class XuiClientOperationFingerprintTest {

    private static final UUID PROVISION_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Test
    void fingerprintIsStableAndExcludesOperationId() {
        String first = XuiClientOperationFingerprint.of(
                PROVISION_ID,
                XuiClientOperationType.ADD_TRAFFIC,
                new LinkedHashMap<>(Map.of("additionalTrafficBytes", 1024L, "mode", "add"))
        );
        String second = XuiClientOperationFingerprint.of(
                PROVISION_ID,
                XuiClientOperationType.ADD_TRAFFIC,
                new LinkedHashMap<>(Map.of("mode", "add", "additionalTrafficBytes", 1024L))
        );

        assertThat(first).isEqualTo(second);
        assertThat(first).matches("[0-9a-f]{64}");
        assertThat(first).doesNotContain(PROVISION_ID.toString());
    }

    @Test
    void differentValuesChangeFingerprint() {
        String first = XuiClientOperationFingerprint.of(PROVISION_ID, XuiClientOperationType.ADD_TRAFFIC, Map.of("bytes", 1));
        String second = XuiClientOperationFingerprint.of(PROVISION_ID, XuiClientOperationType.ADD_TRAFFIC, Map.of("bytes", 2));

        assertThat(first).isNotEqualTo(second);
    }
}
