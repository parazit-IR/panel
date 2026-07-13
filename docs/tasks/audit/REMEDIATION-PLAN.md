# Remediation Plan

## P0 — Security, Corruption, Duplicate Payment/Provisioning, Data Loss

| Defect ID | Task | Requirement | Severity | Evidence | Root cause | Proposed fix | Expected files | Migration | Tests required | Dependency order |
|---|---|---|---|---|---|---|---|---|---|---|
| _None open_ |  |  |  |  |  |  |  |  |  |  |

## Completed Defects

| Defect ID | Task | Requirement | Severity | Fix summary | Migration | Tests |
|---|---|---|---|---|---|---|
| DEF-P0-001 | 31/32 | T31-R004 | Critical | Added `ProvisioningOutboxRepository#claimAvailableByEventId`; implemented atomic PostgreSQL claim in `ProvisioningOutboxRepositoryAdapter` using `FOR UPDATE SKIP LOCKED` and `RETURNING`; updated `ClaimProvisioningOutboxTransaction` to use the atomic claim before marking the order provisioning. | No migration required; existing `idx_provisioning_outbox_status_available` supports availability lookup. | `ConcurrentProvisioningOutboxClaimIntegrationTest#onlyOneWorkerClaimsOutboxEventWhenTransactionsOverlap`; `JAVA_HOME= ./gradlew check` PASS. |
| DEF-P1-001 | 1 | T01-R002 | High | Added standard MIT `LICENSE` with 2026 `parazit-IR` owner derived from repository metadata; added README license section referencing `LICENSE`. | No migration required. | `LICENSE_FILE_CHECK_OK`; UTF-8/LF inspection; `JAVA_HOME= ./gradlew check` PASS with 461 tests. |
| DEF-P1-002 | 1 | T01-R003 | High | Added `.editorconfig`, `.gitattributes`, and `CONTRIBUTING.md`; expanded `.gitignore` to Java/Spring baseline and stopped ignoring Gradle wrapper scripts. | No migration required. | `GOVERNANCE_FILE_CHECK_OK`; `git check-ignore` confirms `.env` ignored and wrapper scripts not ignored; `JAVA_HOME= ./gradlew check` PASS. |
| DEF-P3-002 | 1 | T01-R005 | Low | Corrected `.gitignore` so `gradle/wrapper/gradle-wrapper.jar`, `gradlew`, and `gradlew.bat` are not ignored. | No migration required. | `git status --short --ignored` shows wrapper files untracked, not ignored; `JAVA_HOME= ./gradlew check` PASS. |
| DEF-P1-003 | 31/32 | T31-R005 | High | Added deterministic PostgreSQL concurrency tests for two-operator manual review claim, approve-vs-reject terminal decisions, Zarinpal-vs-manual approval for one order, and retained the outbox multi-worker claim test. Added order and review pessimistic lock repository methods for terminal decisions. | No migration required; uses row locks on existing `orders` and `manual_payment_reviews` rows. | `ConcurrentPaymentApprovalIntegrationTest`; related payment/provisioning integration tests PASS; `JAVA_HOME= ./gradlew check` PASS with 464 tests. |
| DEF-P1-004 | 25/26/32 | T25-R003, T26-R003 | High | Added `ConcurrentXuiClientOperationIntegrationTest` for reset-vs-renew prepare race, delete/disable rejection while update operation is in progress, and successful-operation replay/fingerprint conflict behavior. Added provision row locks and lifecycle in-progress checks. | No migration required; uses existing `uq_xui_client_operations_in_progress_provision` and pessimistic row locks on `xui_client_provisions`. | Focused XUI concurrency test PASS; XUI test slice PASS; `JAVA_HOME= ./gradlew check` PASS with 467 tests. |

## P1 — Missing Required Behavior

| Defect ID | Task | Requirement | Severity | Evidence | Root cause | Proposed fix | Expected files | Migration | Tests required | Dependency order |
|---|---|---|---|---|---|---|---|---|---|---|
| _None open_ |  |  |  |  |  |  |  |  |  |  |

## P2 — Missing Tests, Constraints, Or Weak Architecture

| Defect ID | Task | Requirement | Severity | Evidence | Root cause | Proposed fix | Expected files | Migration | Tests required | Dependency order |
|---|---|---|---|---|---|---|---|---|---|---|
| DEF-P2-001 | 21/23/26 | T21-R002 | Medium | Legacy `application/port/out/xui/XuiClient.java`; `Manual` unsupported-style broad methods; tests assert unsupported operations. | Temporary Task 21 interface not removed after capability ports. | Remove or quarantine legacy port and adapter if unused; keep `XuiInboundClient`, `XuiClientManagementClient`, `XuiClientStateClient`. | XUI ports/adapters/tests | No | architecture test asserting no broad unsupported port | After P1 test additions |
| DEF-P2-002 | 27 | T27-R003 | Medium | `PaymentOperation` lacks idempotency/fingerprint semantics; spec ambiguous. | Payment history under-specified. | Document semantics or add fields if operation history must support replay/fingerprint. | payment domain/repository/docs | Maybe if new columns | repository and service tests | After payment concurrency work |
| DEF-P2-003 | 30/32 | T32-R002 | Medium | Untracked `logs/` contains local generated URLs; `.gitignore` ignores logs but local security scan still finds them. | Runtime logs retained in workspace. | Add audit guidance to clean logs before releases; ensure logs never include credentials/cookies. | docs/logging or cleanup script | No | log redaction tests if needed | Any time |
| DEF-P2-004 | 30/32 | SC-010 | Medium | `HeaderBasedCurrentOperatorProvider` uses `X-OPERATOR-ID` even with Spring Security present. | Temporary operator identity remains. | Map operator ID from authenticated principal/authority; keep header only in test/dev profile if needed. | security/operator provider/controller tests | No | security tests | After P0/P1 |
| DEF-P2-005 | 8/32 | SC-004 | Medium | `SecurityConfiguration` authenticates internal endpoints; early docs/tests may imply no auth. | Later dependency changed behavior. | Document intentional security model and update controller tests to reflect auth/CSRF expectations where appropriate. | docs/security, controller tests | No | security integration tests | After operator identity |
| DEF-P2-006 | 4/21 | SC-006 | Low | XUI property classes split across packages. | Convention drift. | Consolidate property classes or document exception. | config packages/imports/tests | No | context/property tests | Low priority |
| DEF-P2-007 | 3/4 | SC-005 | Low | Docker/local hardcoded dev credentials. | Local convenience defaults. | Keep only in `.env.example`/compose docs or make compose use env defaults with explicit placeholder naming. | compose/local config/docs | No | config binding tests | Low priority |
| DEF-P2-008 | Build | T32-R003 | Medium | `JAVA_HOME=/usr/lib/jvm/default-java` points to Java 11 and blocks Gradle. | Environment mismatch. | Add developer setup note or Gradle daemon JVM criteria; ensure CI uses Java 21. | README/docs/Gradle config | No | `./gradlew javaToolchains`, build in CI | Low priority |

## P3 — Consistency, Cleanup, Documentation

| Defect ID | Task | Requirement | Severity | Evidence | Root cause | Proposed fix | Expected files | Migration | Tests required | Dependency order |
|---|---|---|---|---|---|---|---|---|---|---|
| DEF-P3-001 | 1 | T01-R001 | Low | README is minimal and missing required sections. | Task 1 README not completed. | Expand README with status, overview, planned features, requirements, structure, license. | `README.md` | No | docs review | With DEF-P1-001 |
| DEF-P3-003 | Docs | SC-007 | Low | Order/payment appear before Task 31 in code/spec chronology. | Later implementation evolved task boundaries. | Add note in audit or task docs explaining ordering override. | docs/tasks notes | No | docs review | After P1 |
| DEF-P3-004 | 32 | T32-R004 | Low | Payment docs exist but do not encode this retrospective audit. | Audit-only phase. | Link audit artifacts from task acceptance docs after review. | docs/tasks acceptance docs | No | docs review | Last |

## Final Verification Update — 2026-07-13

Final verification did not close additional defects. Open counts remain P0=0, P1=0, P2=8, P3=3.

The next remediation action remains:
`DEF-P2-001` — remove or quarantine the legacy broad `XuiClient` port and adapter usage, then add an architecture test proving only capability-specific XUI ports remain.

Verification evidence:
- `JAVA_HOME= ./gradlew test --rerun-tasks` — PASS, 467 tests, 0 failures.
- Focused concurrency, architecture, context/configuration, controller, integration, build, check, and Jacoco verification commands passed.
- `./gradlew tasks --all` still fails when inheriting the Java 11 `JAVA_HOME`; use Java 21 or clear `JAVA_HOME` until `DEF-P2-008` is fixed.
