package com.parazit.panel.application.telegram.model;

import java.util.Arrays;

public enum TelegramCallbackAction {
    MAIN_MENU("M"),
    MY_SUBSCRIPTIONS("S"),
    VIEW_SUBSCRIPTION("V"),
    SHOW_CONFIG("C"),
    SHOW_CONFIG_QR("Q"),
    REQUEST_SUBSCRIPTION_LINK("L"),
    CONFIRM_ROTATE_SUBSCRIPTION_TOKEN("R"),
    CANCEL_ROTATION("X"),
    BACK_TO_SUBSCRIPTIONS("B"),
    BACK_TO_MAIN("H"),
    HELP("P");

    private final String code;

    TelegramCallbackAction(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static TelegramCallbackAction fromCode(String code) {
        return Arrays.stream(values())
                .filter(action -> action.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported callback action"));
    }
}
