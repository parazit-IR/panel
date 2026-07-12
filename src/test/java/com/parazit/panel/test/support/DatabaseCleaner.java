package com.parazit.panel.test.support;

import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;

public final class DatabaseCleaner {

    private DatabaseCleaner() {
    }

    public static void cleanUserModuleTables(JdbcTemplate jdbcTemplate) {
        Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null")
                .execute("TRUNCATE TABLE zarinpal_payment_attempts, payment_operations, payments, orders, xui_client_operations, xui_client_provisions, plan_selections, referrals, user_settings, users RESTART IDENTITY");
    }

    public static void cleanPlanTables(JdbcTemplate jdbcTemplate) {
        Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null")
                .execute("TRUNCATE TABLE zarinpal_payment_attempts, payment_operations, payments, orders, xui_client_operations, xui_client_provisions, plan_selections, plans RESTART IDENTITY");
    }

    public static void cleanPlanSelectionTables(JdbcTemplate jdbcTemplate) {
        Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null")
                .execute("TRUNCATE TABLE zarinpal_payment_attempts, payment_operations, payments, orders, xui_client_operations, xui_client_provisions, plan_selections, referrals, user_settings, users, plans RESTART IDENTITY");
    }

    public static void cleanPaymentTables(JdbcTemplate jdbcTemplate) {
        Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null")
                .execute("TRUNCATE TABLE zarinpal_payment_attempts, payment_operations, payments, orders, xui_client_operations, xui_client_provisions, plan_selections, referrals, user_settings, users RESTART IDENTITY");
    }

    public static void cleanTestPersistenceTables(JdbcTemplate jdbcTemplate) {
        Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null")
                .execute("TRUNCATE TABLE test_persistence_entities RESTART IDENTITY");
    }
}
