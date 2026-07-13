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
    SHOW_NOTIFICATION_SETTINGS("r"),
    SHOW_PLAN_CATALOG("E"),
    SHOW_PLAN_DETAILS("b"),
    SELECT_PLAN("c"),
    SHOW_PRE_INVOICE("d"),
    CONTINUE_TO_PAYMENT("f"),
    SHOW_PAYMENT_METHODS("s"),
    SELECT_MANUAL_PAYMENT("t"),
    SELECT_ONLINE_PAYMENT("u"),
    APPLY_DISCOUNT_PLACEHOLDER("v"),
    BACK_TO_PRE_INVOICE("w"),
    BACK_TO_PLAN_DETAILS("x"),
    BACK_TO_PLAN_CATALOG("y"),
    VIEW_PAYMENT("z"),
    REFRESH_PAYMENT_STATUS("0"),
    REQUEST_CANCEL_PAYMENT("1"),
    UPLOAD_MANUAL_RECEIPT("2"),
    LIST_RENEWABLE_SERVICES("3"),
    RENEWABLE_SERVICES_PAGE("4"),
    SHOW_RENEWAL_TARGET("5"),
    LIST_RENEWAL_PLANS("6"),
    RENEWAL_PLANS_PAGE("7"),
    SELECT_RENEWAL_PLAN("8"),
    SHOW_RENEWAL_PRE_INVOICE("9"),
    CONFIRM_RENEWAL_ORDER("aa"),
    BACK_TO_RENEWAL_TARGET("ab"),
    BACK_TO_RENEWABLE_SERVICES("ac");

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
