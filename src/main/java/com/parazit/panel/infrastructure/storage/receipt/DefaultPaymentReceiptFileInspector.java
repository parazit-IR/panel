package com.parazit.panel.infrastructure.storage.receipt;

import com.parazit.panel.application.payment.manual.receipt.ManualPaymentReceiptFileTooLargeException;
import com.parazit.panel.application.payment.manual.receipt.ManualPaymentReceiptInvalidFileException;
import com.parazit.panel.application.payment.manual.receipt.ManualPaymentReceiptUnsupportedTypeException;
import com.parazit.panel.application.port.out.payment.receipt.InspectedPaymentReceiptFile;
import com.parazit.panel.application.port.out.payment.receipt.PaymentReceiptFileInspector;
import com.parazit.panel.application.port.out.payment.receipt.ReceiptUploadSource;
import com.parazit.panel.config.properties.PaymentReceiptStorageProperties;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Component;

@Component
public class DefaultPaymentReceiptFileInspector implements PaymentReceiptFileInspector {

    private final PaymentReceiptStorageProperties properties;

    public DefaultPaymentReceiptFileInspector(PaymentReceiptStorageProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    @Override
    public InspectedPaymentReceiptFile inspect(
            ReceiptUploadSource uploadSource,
            String originalFilename,
            String declaredContentType,
            long declaredSizeBytes
    ) {
        Objects.requireNonNull(uploadSource, "uploadSource must not be null");
        if (declaredSizeBytes <= 0) {
            throw new ManualPaymentReceiptInvalidFileException("Receipt file is empty");
        }
        if (declaredSizeBytes > properties.maxFileSizeBytes()) {
            throw new ManualPaymentReceiptFileTooLargeException();
        }
        byte[] bytes = readBounded(uploadSource);
        if (bytes.length == 0) {
            throw new ManualPaymentReceiptInvalidFileException("Receipt file is empty");
        }
        if (bytes.length > properties.maxFileSizeBytes()) {
            throw new ManualPaymentReceiptFileTooLargeException();
        }
        String detectedContentType = detectContentType(bytes);
        if (!properties.allowedContentTypes().contains(detectedContentType)) {
            throw new ManualPaymentReceiptUnsupportedTypeException("Receipt file type is not supported");
        }
        String extension = extensionFor(detectedContentType);
        validateOriginalExtension(originalFilename, extension);
        validateDeclaredContentType(declaredContentType, detectedContentType);
        String sha256 = sha256(bytes);
        if (detectedContentType.startsWith("image/")) {
            BufferedImage image = decodeImage(bytes);
            if (image.getWidth() > properties.imageMaxWidth() || image.getHeight() > properties.imageMaxHeight()) {
                throw new ManualPaymentReceiptInvalidFileException("Receipt image dimensions are too large");
            }
            return new InspectedPaymentReceiptFile(
                    detectedContentType,
                    extension,
                    bytes.length,
                    sha256,
                    image.getWidth(),
                    image.getHeight()
            );
        }
        return new InspectedPaymentReceiptFile(detectedContentType, extension, bytes.length, sha256, null, null);
    }

    private byte[] readBounded(ReceiptUploadSource uploadSource) {
        try (InputStream input = uploadSource.openStream(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            long total = 0;
            int read;
            while ((read = input.read(buffer)) != -1) {
                total += read;
                if (total > properties.maxFileSizeBytes()) {
                    throw new ManualPaymentReceiptFileTooLargeException();
                }
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        } catch (IOException exception) {
            throw new ManualPaymentReceiptInvalidFileException("Could not read receipt file");
        }
    }

    private static String detectContentType(byte[] bytes) {
        if (bytes.length >= 3
                && (bytes[0] & 0xff) == 0xff
                && (bytes[1] & 0xff) == 0xd8
                && (bytes[2] & 0xff) == 0xff) {
            return "image/jpeg";
        }
        if (bytes.length >= 8
                && (bytes[0] & 0xff) == 0x89
                && bytes[1] == 0x50
                && bytes[2] == 0x4e
                && bytes[3] == 0x47
                && bytes[4] == 0x0d
                && bytes[5] == 0x0a
                && bytes[6] == 0x1a
                && bytes[7] == 0x0a) {
            return "image/png";
        }
        if (bytes.length >= 5
                && bytes[0] == '%'
                && bytes[1] == 'P'
                && bytes[2] == 'D'
                && bytes[3] == 'F'
                && bytes[4] == '-') {
            return "application/pdf";
        }
        String prefix = new String(bytes, 0, Math.min(bytes.length, 64)).toLowerCase(Locale.ROOT);
        if (prefix.contains("<svg") || prefix.contains("<html") || prefix.contains("<script") || prefix.startsWith("mz")) {
            throw new ManualPaymentReceiptUnsupportedTypeException("Receipt file type is not supported");
        }
        throw new ManualPaymentReceiptUnsupportedTypeException("Receipt file type is not supported");
    }

    private static String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "application/pdf" -> "pdf";
            default -> throw new ManualPaymentReceiptUnsupportedTypeException("Receipt file type is not supported");
        };
    }

    private void validateOriginalExtension(String originalFilename, String expectedExtension) {
        String lower = Objects.requireNonNullElse(originalFilename, "").toLowerCase(Locale.ROOT);
        int dot = lower.lastIndexOf('.');
        if (dot < 0) {
            throw new ManualPaymentReceiptUnsupportedTypeException("Receipt filename extension is required");
        }
        String extension = lower.substring(dot + 1);
        if (extension.equals("jpeg")) {
            extension = "jpg";
        }
        if (!extension.equals(expectedExtension) || !properties.allowedExtensions().contains(extension)) {
            throw new ManualPaymentReceiptUnsupportedTypeException("Receipt filename extension does not match content");
        }
    }

    private static void validateDeclaredContentType(String declaredContentType, String detectedContentType) {
        if (declaredContentType == null || declaredContentType.isBlank()) {
            return;
        }
        String normalized = declaredContentType.toLowerCase(Locale.ROOT).split(";")[0].trim();
        if (normalized.equals("image/jpg")) {
            normalized = "image/jpeg";
        }
        if (!normalized.equals(detectedContentType)) {
            throw new ManualPaymentReceiptUnsupportedTypeException("Declared receipt content type does not match content");
        }
    }

    private static BufferedImage decodeImage(byte[] bytes) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) {
                throw new ManualPaymentReceiptInvalidFileException("Receipt image is malformed");
            }
            return image;
        } catch (IOException exception) {
            throw new ManualPaymentReceiptInvalidFileException("Receipt image is malformed");
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
