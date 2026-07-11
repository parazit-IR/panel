package com.parazit.panel.test.support;

import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;

public final class DatabaseCleaner {

    private DatabaseCleaner() {
    }

    public static void cleanUserModuleTables(JdbcTemplate jdbcTemplate) {
        Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null")
                .execute("TRUNCATE TABLE referrals, user_settings, users RESTART IDENTITY");
    }

    public static void cleanPlanTables(JdbcTemplate jdbcTemplate) {
        Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null")
                .execute("TRUNCATE TABLE plans RESTART IDENTITY");
    }

    public static void cleanTestPersistenceTables(JdbcTemplate jdbcTemplate) {
        Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null")
                .execute("TRUNCATE TABLE test_persistence_entities RESTART IDENTITY");
    }
}
