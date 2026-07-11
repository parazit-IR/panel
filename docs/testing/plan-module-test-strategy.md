# Plan Module Test Strategy

## Purpose

This document describes the Phase 3 quality gate for the Plan module.

The module currently includes:

- Plan domain and persistence
- internal admin Plan management
- active user-facing Plan catalog
- temporary user Plan selection
- Plan selection snapshots
- lifecycle, visibility, expiration, and concurrency behavior

It does not include orders, payments, subscriptions, Telegram handlers, 3x-ui integration, VPN provisioning, cron cleanup, discounts, coupons, or localized plan content.

## Test Pyramid

### Domain Tests

Pure domain tests run without Spring:

- `PlanTest`
- `PlanSelectionTest`

They verify invariants, lifecycle transitions, validation boundaries, snapshot immutability, and expiration boundary logic.

### Application Service Tests

Service tests use fakes instead of Spring context:

- admin Plan services
- catalog services
- selection services
- eligibility policy
- result mappers where behavior is meaningful

They verify repository interactions, hidden-plan semantics, idempotency, replacement, expiration handling, eligibility checks, and fixed-clock usage.

### Repository Tests

Repository tests use real PostgreSQL through Testcontainers:

- `PlanRepositoryIntegrationTest`
- `AvailablePlanRepositoryIntegrationTest`
- `PlanSelectionRepositoryIntegrationTest`

They verify Spring Data adapters, Flyway schema, Hibernate validation, ordering, status-constrained lookups, check constraints, foreign keys, partial unique indexes, and audit fields.

### Integration Tests

Application-service integration tests cover complete module behavior:

- `AdminPlanManagementIntegrationTest`
- `AvailablePlanCatalogIntegrationTest`
- `PlanSelectionIntegrationTest`
- `PlanSelectionSnapshotIntegrationTest`
- `PlanSelectionExpirationIntegrationTest`
- `Phase3PlanModuleEndToEndIntegrationTest`

The end-to-end test verifies the Phase 3 flow from admin Plan creation through catalog visibility, selection, snapshot stability, replacement, hiding, archiving, and absence of deferred operational tables.

### Concurrency Tests

Real PostgreSQL concurrency tests use bounded executors, barriers, and timeouts:

- `ConcurrentPlanCreationIntegrationTest`
- `ConcurrentPlanSelectionIntegrationTest`

They do not use sleeps. They verify duplicate Plan code recovery and one active Plan selection per user under same-plan, different-plan, and replacement races.

### Controller Tests

MockMvc tests verify API contracts:

- `AdminPlanControllerTest`
- `PlanCatalogControllerTest`
- `PlanSelectionControllerTest`

They validate success status codes, request validation, hidden-resource semantics, response shape, error bodies, trace IDs, and absence of entity internals.

### Architecture Tests

`ArchitectureRulesTest` uses source scans consistent with the current repository style. It verifies:

- domain does not depend on API or infrastructure;
- application does not depend on infrastructure or Spring Data;
- controllers do not inject repositories;
- domain repositories do not extend Spring Data types;
- Plan APIs do not expose JPA entities;
- no field injection or service locator usage exists;
- deferred order/payment/subscription/Telegram/3x-ui modules are not introduced.

## Testcontainers Strategy

All database integration tests reuse `PostgreSqlContainerSupport`, which starts one PostgreSQL container for the test JVM.

Automated tests do not require:

- locally installed PostgreSQL;
- Docker Compose;
- Flyway history cleanup.

Hibernate runs with `ddl-auto=validate`, so Flyway remains the only schema-management mechanism.

## Database Cleanup

`DatabaseCleaner` truncates application tables without touching Flyway history.

Dependency order is handled by truncating dependent tables first:

- `plan_selections`
- `referrals`
- `user_settings`
- `users`
- `plans`

Plan-only cleanup also truncates `plan_selections` before `plans`.

## Fixed Clock Strategy

Tests avoid real-time sleeps and wall-clock assertions.

Clock-sensitive tests use:

- `MutableClockTestConfiguration`
- `MutableTestClock`
- deterministic `Instant` constants

Selection expiration is checked immediately before and exactly at `expiresAt`.

## Coverage Targets

The Plan module target is:

- line coverage: at least 85% for Plan domain/application classes;
- branch coverage: at least 75% where branch metrics are meaningful.

JaCoCo report generation is wired into Gradle. Coverage is reviewed through:

```bash
./gradlew jacocoTestReport
```

Coverage gaps should be documented instead of adding low-value tests.

## Verification Commands

Use Java 21:

```bash
JAVA_HOME=/home/kamali/.sdkman/candidates/java/21.0.1-oracle ./gradlew clean test --no-daemon
JAVA_HOME=/home/kamali/.sdkman/candidates/java/21.0.1-oracle ./gradlew jacocoTestReport --no-daemon
JAVA_HOME=/home/kamali/.sdkman/candidates/java/21.0.1-oracle ./gradlew check --no-daemon
JAVA_HOME=/home/kamali/.sdkman/candidates/java/21.0.1-oracle ./gradlew build --no-daemon
```

There is no separate `integrationTest` source set in this project. Integration tests run under `test`.

## Local Startup Verification

Start the local application with PostgreSQL available:

```bash
JAVA_HOME=/home/kamali/.sdkman/candidates/java/21.0.1-oracle \
SPRING_PROFILES_ACTIVE=local \
SPRING_SECURITY_USER_NAME=test \
SPRING_SECURITY_USER_PASSWORD=test \
./gradlew bootRun --no-daemon
```

Verify:

- Flyway validates and migrates through the latest version;
- Hibernate initializes with schema validation;
- Actuator health is reachable;
- admin Plan, catalog, and selection endpoints behave according to contract.

## Common Debugging Steps

- For constraint failures, flush the persistence context before assertions.
- For PostgreSQL transaction-abort behavior after expected constraint violations, isolate assertions with non-transactional test methods or separate tests.
- For selection expiration, move the mutable clock to the exact `expiresAt` instant; do not sleep.
- For concurrency tests, use barriers and bounded futures with timeouts.
- For hidden Plan behavior, query through status-constrained repository methods rather than loading the Plan and checking status in controller code.
