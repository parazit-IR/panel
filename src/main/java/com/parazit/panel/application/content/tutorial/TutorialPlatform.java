package com.parazit.panel.application.content.tutorial;

public enum TutorialPlatform {
    ANDROID("android"),
    IOS("ios"),
    WINDOWS("windows"),
    LINUX("linux"),
    MACOS("macos"),
    GENERAL_DOWNLOADS("downloads");

    private final String code;

    TutorialPlatform(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static TutorialPlatform fromCode(String code) {
        for (TutorialPlatform platform : values()) {
            if (platform.code.equals(code)) {
                return platform;
            }
        }
        throw new IllegalArgumentException("Unsupported tutorial platform");
    }
}
