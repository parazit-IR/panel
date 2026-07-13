# Audit Progress

Last fully audited task: 32
Current phase: final verification complete

Last completed defect:
- `DEF-P1-004` — XUI operation/lifecycle race scenarios are covered by deterministic PostgreSQL tests.

Last completed Task remediation:
- Task 25/26 remediation for `T25-R003` and `T26-R003`; partial Task 32 remediation for XUI concurrency coverage.

Last requirement ID:
- `T32-R004`

Commands executed in final verification:
- `./gradlew tasks --all` — FAIL, inherited `JAVA_HOME` points to Java 11; Gradle requires JVM 17+.
- `JAVA_HOME= ./gradlew tasks --all` — PASS.
- `JAVA_HOME= ./gradlew clean test` — PASS, 467 tests, 0 failures, 0 errors, 0 skipped, 0 disabled.
- `JAVA_HOME= ./gradlew check` — PASS.
- `JAVA_HOME= ./gradlew build` — PASS.
- `JAVA_HOME= ./gradlew jacocoTestReport jacocoTestCoverageVerification` — PASS.
- `JAVA_HOME= ./gradlew test --tests '*Concurrent*' --tests '*Concurrency*'` — PASS after rerun; an earlier parallel Gradle invocation collided with coverage output and is not test evidence.
- `JAVA_HOME= ./gradlew test --tests '*Architecture*'` — PASS.
- `JAVA_HOME= ./gradlew test --tests '*Context*' --tests '*Configuration*'` — PASS.
- `JAVA_HOME= ./gradlew test --tests '*ControllerTest'` — PASS.
- `JAVA_HOME= ./gradlew test --tests '*IntegrationTest'` — PASS.
- `JAVA_HOME= ./gradlew test --rerun-tasks` — PASS, 467 tests, 0 failures.
- `grep -RIn "CARD_TO_CARD_MANUAL" docs/tasks/tasks src/main src/test src/main/resources` — PASS, no matches.
- `grep -RIn "Thread\.sleep" src/test/java src/main/java` — only production retry backoff in `XuiRetryExecutor`, no test sleeps.
- tracked-file secret scan — no high-confidence active tracked token/private key; dev defaults/placeholders and test fixtures remain noisy.

Tests currently passing:
- Full unit/integration/controller/concurrency/architecture/context test suite.
- Build, check, Jacoco report, and Jacoco coverage verification.

Tests currently failing:
- None.

Current failures:
- Overall final verification result remains FAIL due open P2/P3 gaps.
- Inherited `JAVA_HOME=/usr/lib/jvm/default-java` points to Java 11 and blocks Gradle unless Java 21 is selected or `JAVA_HOME=` is used.

Specification conflicts:
- No missing task files. Tasks 1 through 32 exist exactly once.
- No `CARD_TO_CARD_MANUAL` occurrence found in task specs or code. Canonical `CARD_TO_CARD` is implemented.
- Later XUI tasks supersede Task 21's temporary oversized `XuiClient` allowance, but legacy `XuiClient` remains.
- Task 11 profile language update was superseded by Task 12 dedicated language management; implementation follows Task 12.
- Later Spring Security dependency changed endpoint behavior relative to early no-auth task assumptions.
- Order/payment behavior appears before Task 31 despite Task 31 describing order finalization.
- Docker/local default credentials are documented development defaults but conflict with strict no-hardcoded-credential guidance.
- XUI configuration properties are split between `config.properties` and `infrastructure.xui.config`.

Files created:
- `.editorconfig`
- `.gitattributes`
- `CONTRIBUTING.md`
- `LICENSE`
- `src/test/java/com/parazit/panel/integration/provisioning/ConcurrentProvisioningOutboxClaimIntegrationTest.java`
- `src/test/java/com/parazit/panel/integration/payment/ConcurrentPaymentApprovalIntegrationTest.java`
- `src/test/java/com/parazit/panel/integration/xui/ConcurrentXuiClientOperationIntegrationTest.java`
- `docs/tasks/audit/FINAL-VERIFICATION.md`

Files modified:
- `.gitignore`
- `README.md`
- `src/main/java/com/parazit/panel/domain/provisioning/outbox/repository/ProvisioningOutboxRepository.java`
- `src/main/java/com/parazit/panel/domain/order/repository/OrderRepository.java`
- `src/main/java/com/parazit/panel/domain/payment/manual/review/repository/ManualPaymentReviewRepository.java`
- `src/main/java/com/parazit/panel/infrastructure/persistence/order/OrderRepositoryAdapter.java`
- `src/main/java/com/parazit/panel/infrastructure/persistence/order/SpringDataOrderRepository.java`
- `src/main/java/com/parazit/panel/application/payment/PaymentApprovalService.java`
- `src/main/java/com/parazit/panel/application/payment/manual/review/ApproveManualPaymentReviewService.java`
- `src/main/java/com/parazit/panel/application/payment/manual/review/RejectManualPaymentReviewService.java`
- `src/main/java/com/parazit/panel/infrastructure/persistence/payment/manual/review/ManualPaymentReviewRepositoryAdapter.java`
- `src/main/java/com/parazit/panel/infrastructure/persistence/payment/manual/review/SpringDataManualPaymentReviewRepository.java`
- `src/main/java/com/parazit/panel/domain/xui/provisioning/repository/XuiClientProvisionRepository.java`
- `src/main/java/com/parazit/panel/infrastructure/persistence/xui/provisioning/XuiClientProvisionRepositoryAdapter.java`
- `src/main/java/com/parazit/panel/infrastructure/persistence/xui/provisioning/SpringDataXuiClientProvisionRepository.java`
- `src/main/java/com/parazit/panel/application/xui/client/XuiClientOperationTransaction.java`
- `src/main/java/com/parazit/panel/application/xui/client/XuiClientLifecycleTransaction.java`
- `src/main/java/com/parazit/panel/infrastructure/persistence/provisioning/outbox/ProvisioningOutboxRepositoryAdapter.java`
- `src/main/java/com/parazit/panel/application/provisioning/outbox/ClaimProvisioningOutboxTransaction.java`
- `docs/tasks/audit/FULL-AUDIT.md`
- `docs/tasks/audit/FULL-AUDIT.json`
- `docs/tasks/audit/TASK-TRACEABILITY-MATRIX.md`
- `docs/tasks/audit/IDEMPOTENCY-MATRIX.md`
- `docs/tasks/audit/SPECIFICATION-CONFLICTS.md`
- `docs/tasks/audit/REMEDIATION-PLAN.md`
- `docs/tasks/audit/AUDIT-PROGRESS.md`

Migrations added:
- None.

Next exact action:
- Fix `DEF-P2-001` by removing or quarantining the legacy broad `XuiClient` port and adapter usage, then add an architecture test that only capability-specific XUI ports remain.
