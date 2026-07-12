package com.parazit.panel.infrastructure.storage.receipt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parazit.panel.application.payment.manual.receipt.ManualPaymentReceiptFileTooLargeException;
import com.parazit.panel.application.payment.manual.receipt.ManualPaymentReceiptInvalidFileException;
import com.parazit.panel.application.payment.manual.receipt.ManualPaymentReceiptUnsupportedTypeException;
import com.parazit.panel.application.port.out.payment.receipt.InspectedPaymentReceiptFile;
import com.parazit.panel.config.properties.PaymentReceiptStorageProperties;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Set;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class DefaultPaymentReceiptFileInspectorTest {

    @Test
    void acceptsValidPngAndComputesStableMetadata() throws Exception {
        byte[] png = pngBytes(2, 3);
        DefaultPaymentReceiptFileInspector inspector = new DefaultPaymentReceiptFileInspector(properties(1024, true, 10, 10));

        InspectedPaymentReceiptFile inspected = inspector.inspect(
                () -> new ByteArrayInputStream(png),
                "receipt.png",
                "image/png",
                png.length
        );

        assertThat(inspected.detectedContentType()).isEqualTo("image/png");
        assertThat(inspected.normalizedExtension()).isEqualTo("png");
        assertThat(inspected.sizeBytes()).isEqualTo(png.length);
        assertThat(inspected.sha256()).isEqualTo(sha256(png));
        assertThat(inspected.imageWidth()).isEqualTo(2);
        assertThat(inspected.imageHeight()).isEqualTo(3);
    }

    @Test
    void acceptsPdfWhenEnabled() {
        byte[] pdf = "%PDF-1.4\n% synthetic\n".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        DefaultPaymentReceiptFileInspector inspector = new DefaultPaymentReceiptFileInspector(properties(1024, true, 10, 10));

        InspectedPaymentReceiptFile inspected = inspector.inspect(
                () -> new ByteArrayInputStream(pdf),
                "receipt.pdf",
                "application/pdf",
                pdf.length
        );

        assertThat(inspected.detectedContentType()).isEqualTo("application/pdf");
        assertThat(inspected.normalizedExtension()).isEqualTo("pdf");
        assertThat(inspected.imageWidth()).isNull();
    }

    @Test
    void rejectsOversizedEmptyMismatchedAndExecutableContent() throws Exception {
        byte[] png = pngBytes(1, 1);
        DefaultPaymentReceiptFileInspector inspector = new DefaultPaymentReceiptFileInspector(properties(8, true, 10, 10));

        assertThatThrownBy(() -> inspector.inspect(() -> new ByteArrayInputStream(png), "receipt.png", "image/png", png.length))
                .isInstanceOf(ManualPaymentReceiptFileTooLargeException.class);
        assertThatThrownBy(() -> inspector.inspect(() -> new ByteArrayInputStream(new byte[0]), "receipt.png", "image/png", 0))
                .isInstanceOf(ManualPaymentReceiptInvalidFileException.class);

        DefaultPaymentReceiptFileInspector normalInspector = new DefaultPaymentReceiptFileInspector(properties(1024, true, 10, 10));
        assertThatThrownBy(() -> normalInspector.inspect(() -> new ByteArrayInputStream(png), "receipt.jpg", "image/png", png.length))
                .isInstanceOf(ManualPaymentReceiptUnsupportedTypeException.class);
        assertThatThrownBy(() -> normalInspector.inspect(
                () -> new ByteArrayInputStream("<html>not a receipt</html>".getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                "receipt.jpg",
                "image/jpeg",
                26
        )).isInstanceOf(ManualPaymentReceiptUnsupportedTypeException.class);
    }

    @Test
    void rejectsDimensionBombs() throws Exception {
        byte[] png = pngBytes(4, 4);
        DefaultPaymentReceiptFileInspector inspector = new DefaultPaymentReceiptFileInspector(properties(2048, true, 2, 2));

        assertThatThrownBy(() -> inspector.inspect(() -> new ByteArrayInputStream(png), "receipt.png", "image/png", png.length))
                .isInstanceOf(ManualPaymentReceiptInvalidFileException.class)
                .hasMessageContaining("dimensions");
    }

    private static PaymentReceiptStorageProperties properties(long maxSize, boolean allowPdf, int width, int height) {
        return new PaymentReceiptStorageProperties(
                true,
                "local",
                Path.of("/tmp/panel-test-receipts"),
                maxSize,
                Set.of("image/jpeg", "image/png", "application/pdf"),
                Set.of("jpg", "jpeg", "png", "pdf"),
                allowPdf,
                width,
                height,
                Duration.ofSeconds(30),
                true
        );
    }

    private static byte[] pngBytes(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
}
