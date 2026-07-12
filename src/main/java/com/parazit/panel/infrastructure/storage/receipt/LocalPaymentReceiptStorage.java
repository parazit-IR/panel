package com.parazit.panel.infrastructure.storage.receipt;

import com.parazit.panel.application.payment.manual.receipt.ManualPaymentReceiptContentUnavailableException;
import com.parazit.panel.application.payment.manual.receipt.ManualPaymentReceiptStorageException;
import com.parazit.panel.application.port.out.payment.receipt.PaymentReceiptContent;
import com.parazit.panel.application.port.out.payment.receipt.PaymentReceiptStorage;
import com.parazit.panel.application.port.out.payment.receipt.StorePaymentReceiptCommand;
import com.parazit.panel.application.port.out.payment.receipt.StoredPaymentReceipt;
import com.parazit.panel.config.properties.PaymentReceiptStorageProperties;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class LocalPaymentReceiptStorage implements PaymentReceiptStorage {

    private static final String PROVIDER = "local";

    private final PaymentReceiptStorageProperties properties;

    public LocalPaymentReceiptStorage(PaymentReceiptStorageProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    @Override
    public StoredPaymentReceipt store(StorePaymentReceiptCommand command) {
        if (!properties.enabled()) {
            throw new ManualPaymentReceiptStorageException("Receipt storage is disabled");
        }
        String storageKey = storageKey(command.receiptId(), command.sanitizedFilename());
        Path target = resolveStorageKey(storageKey);
        Path temp = target.resolveSibling(target.getFileName() + ".tmp-" + UUID.randomUUID());
        try {
            Files.createDirectories(target.getParent());
            try (InputStream input = command.uploadSource().openStream()) {
                Files.copy(input, temp, StandardCopyOption.REPLACE_EXISTING);
            }
            long storedSize = Files.size(temp);
            if (storedSize != command.fileSizeBytes()) {
                Files.deleteIfExists(temp);
                throw new ManualPaymentReceiptStorageException("Stored receipt size mismatch");
            }
            String storedHash = sha256(temp);
            if (!storedHash.equals(command.fileSha256())) {
                Files.deleteIfExists(temp);
                throw new ManualPaymentReceiptStorageException("Stored receipt hash mismatch");
            }
            Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE);
            return new StoredPaymentReceipt(
                    PROVIDER,
                    storageKey,
                    command.sanitizedFilename(),
                    command.detectedContentType(),
                    command.fileSizeBytes(),
                    command.fileSha256()
            );
        } catch (IOException exception) {
            tryDelete(temp);
            throw new ManualPaymentReceiptStorageException("Could not store receipt file", exception);
        }
    }

    @Override
    public PaymentReceiptContent load(String storageKey) {
        Path path = resolveStorageKey(storageKey);
        if (!Files.isRegularFile(path)) {
            throw new ManualPaymentReceiptContentUnavailableException();
        }
        try {
            long size = Files.size(path);
            String filename = path.getFileName().toString();
            String contentType = contentTypeFromFilename(filename);
            return new PaymentReceiptContent(filename, contentType, size, () -> Files.newInputStream(path));
        } catch (IOException exception) {
            throw new ManualPaymentReceiptContentUnavailableException();
        }
    }

    @Override
    public void delete(String storageKey) {
        tryDelete(resolveStorageKey(storageKey));
    }

    @Override
    public boolean exists(String storageKey) {
        return Files.isRegularFile(resolveStorageKey(storageKey));
    }

    private String storageKey(UUID receiptId, String sanitizedFilename) {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        String extension = sanitizedFilename.substring(sanitizedFilename.lastIndexOf('.') + 1);
        return "manual-receipts/%04d/%02d/%s/%s.%s".formatted(
                now.getYear(),
                now.getMonthValue(),
                receiptId,
                UUID.randomUUID(),
                extension
        );
    }

    private Path resolveStorageKey(String storageKey) {
        String key = Objects.requireNonNull(storageKey, "storageKey must not be null");
        if (key.isBlank() || key.contains("..") || key.startsWith("/") || key.startsWith("\\")) {
            throw new ManualPaymentReceiptStorageException("Invalid receipt storage key");
        }
        Path root = properties.localRoot().toAbsolutePath().normalize();
        Path path = root.resolve(key).normalize();
        if (!path.startsWith(root)) {
            throw new ManualPaymentReceiptStorageException("Invalid receipt storage key");
        }
        return path;
    }

    private static String contentTypeFromFilename(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".pdf")) {
            return "application/pdf";
        }
        return "application/octet-stream";
    }

    private static void tryDelete(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Best-effort compensation. The caller logs the workflow context.
        }
    }

    private static String sha256(Path path) throws IOException {
        try (InputStream input = Files.newInputStream(path)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
