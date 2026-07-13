package com.parazit.panel.infrastructure.qrcode;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.parazit.panel.application.port.out.qrcode.QrCodeGenerator;
import com.parazit.panel.application.qrcode.InvalidQrOptionsException;
import com.parazit.panel.application.qrcode.QrCodeGenerationException;
import com.parazit.panel.application.qrcode.QrPayloadTooLargeException;
import com.parazit.panel.application.qrcode.UnsupportedQrFormatException;
import com.parazit.panel.application.qrcode.model.GeneratedQrCode;
import com.parazit.panel.application.qrcode.model.QrErrorCorrection;
import com.parazit.panel.application.qrcode.model.QrImageFormat;
import com.parazit.panel.application.qrcode.model.QrPayload;
import com.parazit.panel.application.qrcode.model.QrRenderOptions;
import com.parazit.panel.config.properties.QrCodeProperties;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Component;

@Component
public class ZxingQrCodeGenerator implements QrCodeGenerator {

    private static final String PNG_CONTENT_TYPE = "image/png";

    private final QrCodeProperties properties;

    public ZxingQrCodeGenerator(QrCodeProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    @Override
    public GeneratedQrCode generate(QrPayload payload, QrRenderOptions options) {
        Objects.requireNonNull(payload, "payload must not be null");
        QrRenderOptions requiredOptions = Objects.requireNonNull(options, "options must not be null");
        validate(payload, requiredOptions);
        if (requiredOptions.format() == QrImageFormat.SVG) {
            throw new UnsupportedQrFormatException();
        }
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(
                    payload.value(),
                    BarcodeFormat.QR_CODE,
                    requiredOptions.width(),
                    requiredOptions.height(),
                    hints(requiredOptions)
            );
            byte[] bytes = pngBytes(matrix);
            if (bytes.length > properties.maxOutputBytes()) {
                throw new QrCodeGenerationException("Generated QR image is too large");
            }
            return new GeneratedQrCode(
                    bytes,
                    QrImageFormat.PNG,
                    PNG_CONTENT_TYPE,
                    requiredOptions.width(),
                    requiredOptions.height(),
                    sha256(bytes)
            );
        } catch (WriterException exception) {
            throw new QrCodeGenerationException("QR payload cannot be encoded", exception);
        }
    }

    private void validate(QrPayload payload, QrRenderOptions options) {
        try {
            payload.validateLength(properties.maxPayloadCharacters());
        } catch (IllegalArgumentException exception) {
            throw new QrPayloadTooLargeException();
        }
        if (options.width() < properties.minSize() || options.width() > properties.maxSize()) {
            throw new InvalidQrOptionsException("QR size is outside configured bounds");
        }
        if (options.width() != options.height()) {
            throw new InvalidQrOptionsException("QR dimensions must be square");
        }
        if (options.marginModules() > properties.maxMarginModules()) {
            throw new InvalidQrOptionsException("QR margin is outside configured bounds");
        }
        if (options.transparentBackground()) {
            throw new InvalidQrOptionsException("Transparent QR background is not supported");
        }
        if (options.format() == QrImageFormat.SVG && !properties.allowSvg()) {
            throw new UnsupportedQrFormatException();
        }
    }

    private static Map<EncodeHintType, Object> hints(QrRenderOptions options) {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
        hints.put(EncodeHintType.MARGIN, options.marginModules());
        hints.put(EncodeHintType.ERROR_CORRECTION, errorCorrection(options.errorCorrection()));
        return hints;
    }

    private static ErrorCorrectionLevel errorCorrection(QrErrorCorrection correction) {
        return switch (correction) {
            case LOW -> ErrorCorrectionLevel.L;
            case MEDIUM -> ErrorCorrectionLevel.M;
            case QUARTILE -> ErrorCorrectionLevel.Q;
            case HIGH -> ErrorCorrectionLevel.H;
        };
    }

    private static byte[] pngBytes(BitMatrix matrix) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            BufferedImage image = new BufferedImage(matrix.getWidth(), matrix.getHeight(), BufferedImage.TYPE_INT_RGB);
            int black = Color.BLACK.getRGB();
            int white = Color.WHITE.getRGB();
            for (int y = 0; y < matrix.getHeight(); y++) {
                for (int x = 0; x < matrix.getWidth(); x++) {
                    image.setRGB(x, y, matrix.get(x, y) ? black : white);
                }
            }
            ImageIO.write(image, "PNG", output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new QrCodeGenerationException("QR image could not be rendered", exception);
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder builder = new StringBuilder(digest.length * 2);
            for (byte current : digest) {
                builder.append(String.format("%02x", current));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
