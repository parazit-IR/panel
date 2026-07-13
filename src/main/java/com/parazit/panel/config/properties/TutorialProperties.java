package com.parazit.panel.config.properties;

import com.parazit.panel.application.content.tutorial.TutorialContent;
import com.parazit.panel.application.content.tutorial.TutorialDownloadLink;
import com.parazit.panel.application.content.tutorial.TutorialDownloadSource;
import com.parazit.panel.application.content.tutorial.TutorialPlatform;
import com.parazit.panel.application.security.url.TrustedExternalUrlValidator;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.telegram.tutorials")
public record TutorialProperties(
        boolean enabled,
        Set<String> trustedHosts,
        PlatformProperties android,
        PlatformProperties ios,
        PlatformProperties windows,
        PlatformProperties linux,
        PlatformProperties macos,
        PlatformProperties downloads
) {

    public TutorialProperties {
        trustedHosts = trustedHosts == null || trustedHosts.isEmpty()
                ? Set.of("github.com", "play.google.com", "apps.apple.com", "apps.microsoft.com", "microsoft.com")
                : trustedHosts.stream().map(host -> host.toLowerCase(Locale.ROOT).trim()).filter(host -> !host.isBlank()).collect(java.util.stream.Collectors.toUnmodifiableSet());
        android = android == null ? defaults(TutorialPlatform.ANDROID) : android;
        ios = ios == null ? defaults(TutorialPlatform.IOS) : ios;
        windows = windows == null ? defaults(TutorialPlatform.WINDOWS) : windows;
        linux = linux == null ? defaults(TutorialPlatform.LINUX) : linux;
        macos = macos == null ? defaults(TutorialPlatform.MACOS) : macos;
        downloads = downloads == null ? defaults(TutorialPlatform.GENERAL_DOWNLOADS) : downloads;
        List<TutorialContent> contents = List.of(
                android.toContent(TutorialPlatform.ANDROID),
                ios.toContent(TutorialPlatform.IOS),
                windows.toContent(TutorialPlatform.WINDOWS),
                linux.toContent(TutorialPlatform.LINUX),
                macos.toContent(TutorialPlatform.MACOS),
                downloads.toContent(TutorialPlatform.GENERAL_DOWNLOADS)
        );
        List<TutorialContent> enabledContents = contents.stream().filter(TutorialContent::enabled).toList();
        if (enabled && enabledContents.isEmpty()) {
            throw new IllegalArgumentException("app.telegram.tutorials must contain at least one enabled platform");
        }
        Set<String> validatedTrustedHosts = trustedHosts;
        enabledContents.forEach(content -> validateContent(content, validatedTrustedHosts));
    }

    public List<TutorialContent> enabledContents() {
        return allContents().stream().filter(TutorialContent::enabled).toList();
    }

    public List<TutorialContent> allContents() {
        return List.of(
                android.toContent(TutorialPlatform.ANDROID),
                ios.toContent(TutorialPlatform.IOS),
                windows.toContent(TutorialPlatform.WINDOWS),
                linux.toContent(TutorialPlatform.LINUX),
                macos.toContent(TutorialPlatform.MACOS),
                downloads.toContent(TutorialPlatform.GENERAL_DOWNLOADS)
        );
    }

    private static void validateContent(TutorialContent content, Set<String> trustedHosts) {
        for (TutorialDownloadLink link : content.downloadLinks()) {
            validateUrl(link.url(), trustedHosts);
        }
    }

    private static void validateUrl(URI uri, Set<String> trustedHosts) {
        new TrustedExternalUrlValidator().validate(uri, trustedHosts);
    }

    private static PlatformProperties defaults(TutorialPlatform platform) {
        return switch (platform) {
            case ANDROID -> new PlatformProperties(
                    true,
                    "آموزش اتصال در اندروید",
                    "v2rayNG",
                    "مراحل کلی اتصال در اندروید.",
                    List.of(
                            "برنامه تاییدشده را از منبع رسمی نصب کنید.",
                            "از بخش سرویس‌های من، لینک اشتراک یا QR را دریافت کنید.",
                            "در برنامه، اشتراک را از Clipboard یا QR وارد کنید.",
                            "لیست سرورها را به‌روزرسانی کنید.",
                            "یکی از سرورها را انتخاب و اتصال را فعال کنید."
                    ),
                    List.of(),
                    List.of(),
                    "1",
                    ""
            );
            case IOS -> new PlatformProperties(true, "آموزش اتصال در آیفون", "Approved iOS client", "مراحل کلی اتصال در iOS.", List.of(
                    "برنامه تاییدشده را از App Store نصب کنید.",
                    "لینک اشتراک را از بخش سرویس‌های من کپی کنید.",
                    "اشتراک را در برنامه وارد کنید.",
                    "پروفایل‌ها را به‌روزرسانی و اتصال را فعال کنید."
            ), List.of(), List.of(), "1", "");
            case WINDOWS -> new PlatformProperties(true, "آموزش اتصال در ویندوز", "Approved desktop client", "مراحل کلی اتصال در Windows.", List.of(
                    "برنامه تاییدشده دسکتاپ را از منبع رسمی نصب کنید.",
                    "لینک اشتراک را وارد کنید.",
                    "پروفایل‌ها را به‌روزرسانی کنید.",
                    "پروفایل مورد نظر را انتخاب و اتصال را فعال کنید."
            ), List.of(), List.of(), "1", "");
            case LINUX -> new PlatformProperties(true, "آموزش اتصال در لینوکس", "Approved Linux client", "مراحل کلی اتصال در Linux.", List.of(
                    "برنامه تاییدشده لینوکس را نصب کنید.",
                    "اشتراک را از لینک وارد کنید.",
                    "پروفایل‌ها را به‌روزرسانی کنید.",
                    "مطابق مستندات برنامه، اتصال را فعال کنید."
            ), List.of(), List.of(), "1", "");
            case MACOS -> new PlatformProperties(true, "آموزش اتصال در مک", "Approved macOS client", "مراحل کلی اتصال در macOS.", List.of(
                    "برنامه تاییدشده macOS را نصب کنید.",
                    "لینک اشتراک را وارد کنید.",
                    "پروفایل‌ها را به‌روزرسانی کنید.",
                    "اتصال را فعال کنید."
            ), List.of(), List.of(), "1", "");
            case GENERAL_DOWNLOADS -> new PlatformProperties(true, "لینک دانلود برنامه‌ها", "", "لینک‌های دانلود پس از تایید منبع رسمی نمایش داده می‌شوند.", List.of(), List.of(), List.of(), "1", "");
        };
    }

    public record PlatformProperties(
            boolean enabled,
            String title,
            String appName,
            String shortDescription,
            List<String> steps,
            List<DownloadLinkProperties> downloadLinks,
            List<TroubleshootingProperties> troubleshootingItems,
            String templateVersion,
            String notes
    ) {

        public TutorialContent toContent(TutorialPlatform platform) {
            List<TutorialDownloadLink> links = downloadLinks == null ? List.of() : downloadLinks.stream()
                    .map(DownloadLinkProperties::toLink)
                    .toList();
            List<com.parazit.panel.application.content.tutorial.TutorialTroubleshootingItem> troubleshooting = troubleshootingItems == null ? List.of() : troubleshootingItems.stream()
                    .map(TroubleshootingProperties::toItem)
                    .toList();
            return new TutorialContent(platform, enabled, title, appName, shortDescription, steps, links, troubleshooting, templateVersion, notes);
        }
    }

    public record DownloadLinkProperties(String label, URI url, TutorialDownloadSource source, boolean primary) {
        public TutorialDownloadLink toLink() {
            return new TutorialDownloadLink(label, url, source, primary);
        }
    }

    public record TroubleshootingProperties(String title, String description) {
        public com.parazit.panel.application.content.tutorial.TutorialTroubleshootingItem toItem() {
            return new com.parazit.panel.application.content.tutorial.TutorialTroubleshootingItem(title, description);
        }
    }

    @Override
    public String toString() {
        return "TutorialProperties[enabled=%s,platforms=%d]".formatted(enabled, enabledContents().size());
    }
}
