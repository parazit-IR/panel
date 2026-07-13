package com.parazit.panel.infrastructure.qrcode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.parazit.panel.application.qrcode.InvalidQrOptionsException;
import com.parazit.panel.application.qrcode.QrPayloadTooLargeException;
import com.parazit.panel.application.qrcode.UnsupportedQrFormatException;
import com.parazit.panel.application.qrcode.model.GeneratedQrCode;
import com.parazit.panel.application.qrcode.model.QrErrorCorrection;
import com.parazit.panel.application.qrcode.model.QrImageFormat;
import com.parazit.panel.application.qrcode.model.QrPayload;
import com.parazit.panel.application.qrcode.model.QrPayloadType;
import com.parazit.panel.application.qrcode.model.QrRenderOptions;
import com.parazit.panel.config.properties.QrCodeProperties;
import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class ZxingQrCodeGeneratorTest {

    private final ZxingQrCodeGenerator generator = new ZxingQrCodeGenerator(properties(4096));

    @Test
    void generatesDeterministicDecodablePngForSubscriptionUrlAndVlessUri() throws Exception {
        String subscriptionUrl = "https://subscriptions.example.test/sub/sub_abcdefghijklmnopqrstuvwxyz123456";
        QrRenderOptions options = new QrRenderOptions(512, 512, 4, QrErrorCorrection.MEDIUM, QrImageFormat.PNG, false);

        GeneratedQrCode first = generator.generate(new QrPayload(QrPayloadType.SUBSCRIPTION_URL, subscriptionUrl), options);
        GeneratedQrCode second = generator.generate(new QrPayload(QrPayloadType.SUBSCRIPTION_URL, subscriptionUrl), options);

        assertThat(first.bytes()).startsWith(0x89, 0x50, 0x4E, 0x47);
        assertThat(first.bytes()).containsExactly(second.bytes());
        assertThat(first.contentHash()).isEqualTo(second.contentHash());
        assertThat(decode(first)).isEqualTo(subscriptionUrl);

        String vless = "vless://11111111-1111-1111-1111-111111111111@vpn.example.test:443?encryption=none&security=reality#remark";
        assertThat(decode(generator.generate(new QrPayload(QrPayloadType.VLESS_URI, vless), options))).isEqualTo(vless);
    }

    @Test
    void supportsAllErrorCorrectionLevels() throws Exception {
        String payload = "https://subscriptions.example.test/sub/sub_abcdefghijklmnopqrstuvwxyz123456";
        for (QrErrorCorrection correction : QrErrorCorrection.values()) {
            GeneratedQrCode code = generator.generate(
                    new QrPayload(QrPayloadType.SUBSCRIPTION_URL, payload),
                    new QrRenderOptions(512, 512, 4, correction, QrImageFormat.PNG, false)
            );
            assertThat(decode(code)).isEqualTo(payload);
        }
    }

    @Test
    void rejectsUnsupportedOptionsAndOversizedPayload() {
        QrPayload payload = new QrPayload(QrPayloadType.SUBSCRIPTION_URL, "https://example.test/sub/sub_secret");

        assertThatThrownBy(() -> generator.generate(payload, new QrRenderOptions(64, 64, 4, QrErrorCorrection.MEDIUM, QrImageFormat.PNG, false)))
                .isInstanceOf(InvalidQrOptionsException.class);
        assertThatThrownBy(() -> generator.generate(payload, new QrRenderOptions(512, 512, 4, QrErrorCorrection.MEDIUM, QrImageFormat.SVG, false)))
                .isInstanceOf(UnsupportedQrFormatException.class);
        assertThatThrownBy(() -> new ZxingQrCodeGenerator(properties(10)).generate(payload, new QrRenderOptions(512, 512, 4, QrErrorCorrection.MEDIUM, QrImageFormat.PNG, false)))
                .isInstanceOf(QrPayloadTooLargeException.class);
    }

    @Test
    void concurrentGenerationIsDeterministic() throws Exception {
        String payload = "https://subscriptions.example.test/sub/sub_abcdefghijklmnopqrstuvwxyz123456";
        QrRenderOptions options = new QrRenderOptions(512, 512, 4, QrErrorCorrection.MEDIUM, QrImageFormat.PNG, false);
        Callable<String> task = () -> generator.generate(new QrPayload(QrPayloadType.SUBSCRIPTION_URL, payload), options).contentHash();
        try (var executor = Executors.newFixedThreadPool(4)) {
            List<String> hashes = executor.invokeAll(List.of(task, task, task, task))
                    .stream()
                    .map(future -> {
                        try {
                            return future.get();
                        } catch (Exception exception) {
                            throw new AssertionError(exception);
                        }
                    })
                    .toList();
            assertThat(hashes).containsOnly(hashes.getFirst());
        }
    }

    private static String decode(GeneratedQrCode code) throws Exception {
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(
                ImageIO.read(new ByteArrayInputStream(code.bytes()))
        )));
        return new MultiFormatReader().decode(bitmap).getText();
    }

    private static QrCodeProperties properties(int maxPayloadCharacters) {
        return new QrCodeProperties(
                true,
                512,
                128,
                2048,
                4,
                16,
                QrErrorCorrection.MEDIUM,
                QrImageFormat.PNG,
                false,
                false,
                maxPayloadCharacters,
                Duration.ofSeconds(2),
                1_048_576
        );
    }
}

