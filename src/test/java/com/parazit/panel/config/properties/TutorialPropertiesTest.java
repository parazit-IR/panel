package com.parazit.panel.config.properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.parazit.panel.application.content.tutorial.TutorialDownloadSource;
import com.parazit.panel.application.content.tutorial.TutorialPlatform;
import java.net.URI;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TutorialPropertiesTest {

    @Test
    void suppliesSafeDefaultTutorialContent() {
        TutorialProperties properties = new TutorialProperties(true, null, null, null, null, null, null, null);

        assertThat(properties.enabledContents()).extracting("platform")
                .contains(TutorialPlatform.ANDROID, TutorialPlatform.IOS, TutorialPlatform.WINDOWS, TutorialPlatform.LINUX, TutorialPlatform.MACOS);
    }

    @Test
    void rejectsInvalidDownloadUrlForEnabledContent() {
        TutorialProperties.PlatformProperties android = new TutorialProperties.PlatformProperties(
                true,
                "Android",
                "v2rayNG",
                "",
                List.of("Install app."),
                List.of(new TutorialProperties.DownloadLinkProperties("Mirror", URI.create("http://10.0.0.1/app.apk"), TutorialDownloadSource.OFFICIAL_WEBSITE, true)),
                List.of(),
                "1",
                ""
        );

        assertThatThrownBy(() -> new TutorialProperties(true, Set.of("github.com"), android, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
