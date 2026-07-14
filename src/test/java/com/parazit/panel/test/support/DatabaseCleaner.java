package com.parazit.panel.test.support;

import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;

public final class DatabaseCleaner {

    private static final String TELEGRAM_TABLES = "telegram_sensitive_actions, telegram_purchase_sessions, telegram_processed_updates, telegram_polling_state, ";
    private static final String WALLET_TOP_UP_TABLES = "wallet_top_up_requests, ";
    private static final String PROMOTION_TABLES = "promotion_redemptions, discount_codes, gift_codes, ";

    private DatabaseCleaner() {
    }

    public static void cleanUserModuleTables(JdbcTemplate jdbcTemplate) {
        Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null")
                .execute("TRUNCATE TABLE " + TELEGRAM_TABLES + WALLET_TOP_UP_TABLES + PROMOTION_TABLES + "wallet_transactions, wallets, subscription_renewal_history, renewal_outbox, subscriptions, provisioning_outbox, manual_payment_reviews, manual_payment_receipts, manual_card_payment_instructions, zarinpal_payment_attempts, payment_operations, payments, orders, xui_client_operations, xui_client_provisions, plan_selections, referrals, user_settings, users RESTART IDENTITY");
    }

    public static void cleanPlanTables(JdbcTemplate jdbcTemplate) {
        Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null")
                .execute("TRUNCATE TABLE " + TELEGRAM_TABLES + WALLET_TOP_UP_TABLES + PROMOTION_TABLES + "wallet_transactions, wallets, subscription_renewal_history, renewal_outbox, subscriptions, provisioning_outbox, manual_payment_reviews, manual_payment_receipts, manual_card_payment_instructions, zarinpal_payment_attempts, payment_operations, payments, orders, xui_client_operations, xui_client_provisions, plan_selections, plans RESTART IDENTITY");
    }

    public static void cleanPlanSelectionTables(JdbcTemplate jdbcTemplate) {
        Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null")
                .execute("TRUNCATE TABLE " + TELEGRAM_TABLES + WALLET_TOP_UP_TABLES + PROMOTION_TABLES + "wallet_transactions, wallets, subscription_renewal_history, renewal_outbox, subscriptions, provisioning_outbox, manual_payment_reviews, manual_payment_receipts, manual_card_payment_instructions, zarinpal_payment_attempts, payment_operations, payments, orders, xui_client_operations, xui_client_provisions, plan_selections, referrals, user_settings, users, plans RESTART IDENTITY");
    }

    public static void cleanPaymentTables(JdbcTemplate jdbcTemplate) {
        Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null")
                .execute("TRUNCATE TABLE " + TELEGRAM_TABLES + WALLET_TOP_UP_TABLES + PROMOTION_TABLES + "wallet_transactions, wallets, subscription_renewal_history, renewal_outbox, subscriptions, provisioning_outbox, manual_payment_reviews, manual_payment_receipts, manual_card_payment_instructions, zarinpal_payment_attempts, payment_operations, payments, orders, xui_client_operations, xui_client_provisions, plan_selections, referrals, user_settings, users RESTART IDENTITY");
    }

    public static void cleanTestPersistenceTables(JdbcTemplate jdbcTemplate) {
        Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null")
                .execute("TRUNCATE TABLE test_persistence_entities RESTART IDENTITY");
    }
}
