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
    HELP("P"),
    SHOW_TARIFFS("T"),
    SHOW_TARIFF_PAGE("N"),
    SHOW_TUTORIALS("U"),
    SHOW_TUTORIAL_PLATFORM("O"),
    SHOW_DOWNLOAD_LINKS("D"),
    SHOW_SUPPORT("W"),
    SHOW_FAQ("F"),
    SHOW_FAQ_PAGE("G"),
    SHOW_FAQ_ITEM("I"),
    BACK_TO_TUTORIALS("Y"),
    BACK_TO_SUPPORT("J"),
    BACK_TO_FAQ("K"),
    BUY_SUBSCRIPTION("A"),
    SHOW_ACCOUNT("Z"),
    LIST_MY_SERVICES("e"),
    MY_SERVICES_PAGE("g"),
    SEARCH_MY_SERVICES("h"),
    SHOW_SERVICE_DETAILS("i"),
    REFRESH_SERVICE_STATUS("j"),
    SHOW_SUBSCRIPTION_LINK("k"),
    SHOW_SUBSCRIPTION_QR("l"),
    SHOW_VLESS_CONFIG("m"),
    REQUEST_RENEWAL("n"),
    BACK_TO_MY_SERVICES("o"),
    BACK_TO_ACCOUNT("p"),
    SHOW_PAYMENTS("q"),
    SHOW_NOTIFICATION_SETTINGS("r");

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
