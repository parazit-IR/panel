package com.parazit.panel.infrastructure.storage.receipt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parazit.panel.application.payment.manual.receipt.ManualPaymentReceiptStorageException;
import com.parazit.panel.application.port.out.payment.receipt.PaymentReceiptContent;
import com.parazit.panel.application.port.out.payment.receipt.StorePaymentReceiptCommand;
import com.parazit.panel.application.port.out.payment.receipt.StoredPaymentReceipt;
import com.parazit.panel.config.properties.PaymentReceiptStorageProperties;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalPaymentReceiptStorageTest {

    @TempDir
    Path tempDir;

    @Test
    void storesLoadsAndDeletesWithoutUsingOriginalPath() throws Exception {
        byte[] bytes = "synthetic receipt".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        LocalPaymentReceiptStorage storage = new LocalPaymentReceiptStorage(properties(true));

        StoredPaymentReceipt stored = storage.store(command(bytes, sha256(bytes)));

        assertThat(stored.storageProvider()).isEqualTo("local");
        assertThat(stored.storageKey()).startsWith("manual-receipts/");
        assertThat(stored.storageKey()).doesNotContain("..");
        assertThat(stored.storageKey()).doesNotContain("secret");
        assertThat(storage.exists(stored.storageKey())).isTrue();

        PaymentReceiptContent content = storage.load(stored.storageKey());
        assertThat(content.contentType()).isEqualTo("image/png");
        assertThat(content.sizeBytes()).isEqualTo(bytes.length);
        assertThat(content.contentSource().openStream().readAllBytes()).isEqualTo(bytes);

        storage.delete(stored.storageKey());
        assertThat(storage.exists(stored.storageKey())).isFalse();
    }

    @Test
    void rejectsDisabledStorageInvalidKeysAndHashMismatch() throws Exception {
        byte[] bytes = "synthetic receipt".getBytes(java.nio.charset.StandardCharsets.UTF_8);

        assertThatThrownBy(() -> new LocalPaymentReceiptStorage(properties(false)).store(command(bytes, sha256(bytes))))
                .isInstanceOf(ManualPaymentReceiptStorageException.class);

        LocalPaymentReceiptStorage storage = new LocalPaymentReceiptStorage(properties(true));
        assertThatThrownBy(() -> storage.exists("../outside.png"))
                .isInstanceOf(ManualPaymentReceiptStorageException.class);
        assertThatThrownBy(() -> storage.store(command(bytes, "0".repeat(64))))
                .isInstanceOf(ManualPaymentReceiptStorageException.class)
                .hasMessageContaining("hash");

        assertThat(Files.walk(tempDir).filter(Files::isRegularFile).toList()).isEmpty();
    }

    private StorePaymentReceiptCommand command(byte[] bytes, String sha256) {
        return new StorePaymentReceiptCommand(
                UUID.randomUUID(),
                "../../secret.png",
                "receipt.png",
                "image/png",
                bytes.length,
                sha256,
                () -> new ByteArrayInputStream(bytes)
        );
    }

    private PaymentReceiptStorageProperties properties(boolean enabled) {
        return new PaymentReceiptStorageProperties(
                enabled,
                "local",
                tempDir,
                5 * 1024 * 1024,
                Set.of("image/jpeg", "image/png", "application/pdf"),
                Set.of("jpg", "jpeg", "png", "pdf"),
                true,
                5000,
                5000,
                Duration.ofSeconds(30),
                true
        );
    }

    private static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
}
