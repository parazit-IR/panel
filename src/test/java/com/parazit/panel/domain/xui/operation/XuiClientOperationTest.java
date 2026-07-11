package com.parazit.panel.domain.xui.operation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class XuiClientOperationTest {

    private static final UUID OPERATION_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID PROVISION_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final Instant NOW = Instant.parse("2026-07-12T00:00:00Z");

    @Test
    void createsPendingOperationAndCompletesLifecycle() {
        XuiClientOperation operation = operation();

        assertThat(operation.getStatus()).isEqualTo(XuiClientOperationStatus.PENDING);
        assertThat(operation.getRequestFingerprint()).isEqualTo("abc123");

        operation.markInProgress();
        operation.markSucceeded(NOW.plusSeconds(1));

        assertThat(operation.getStatus()).isEqualTo(XuiClientOperationStatus.SUCCEEDED);
        assertThat(operation.getCompletedAt()).isEqualTo(NOW.plusSeconds(1));
        assertThatThrownBy(() -> operation.markFailed("FAILED", "no", NOW.plusSeconds(2)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void failedAndUnknownStatesStoreSanitizedMessages() {
        XuiClientOperation failed = operation();
        failed.markInProgress();
        failed.markFailed("REMOTE_REJECTED", "x".repeat(600), NOW.plusSeconds(1));
        assertThat(failed.getStatus()).isEqualTo(XuiClientOperationStatus.FAILED);
        assertThat(failed.getFailureMessage()).hasSize(500);

        XuiClientOperation unknown = operation();
        unknown.markInProgress();
        unknown.markUnknown("REMOTE_UNCERTAIN", "timeout");
        assertThat(unknown.getStatus()).isEqualTo(XuiClientOperationStatus.UNKNOWN);
        assertThat(unknown.getCompletedAt()).isNull();
    }

    @Test
    void rejectsInvalidCreation() {
        assertThatThrownBy(() -> XuiClientOperation.create(null, PROVISION_ID, XuiClientOperationType.ENABLE, "abc", NOW))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> XuiClientOperation.create(OPERATION_ID, PROVISION_ID, XuiClientOperationType.ENABLE, " ", NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static XuiClientOperation operation() {
        return XuiClientOperation.create(
                OPERATION_ID,
                PROVISION_ID,
                XuiClientOperationType.RENEW_EXPIRY,
                "abc123",
                NOW
        );
    }
}
