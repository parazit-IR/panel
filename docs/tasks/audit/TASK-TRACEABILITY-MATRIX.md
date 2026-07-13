# Task Traceability Matrix

Status values are limited to: IMPLEMENTED_AND_TESTED, IMPLEMENTED_NOT_TESTED, PARTIALLY_IMPLEMENTED, MISSING, IMPLEMENTED_DIFFERENTLY_BUT_VALID, CONFLICTING_SPECIFICATION, NOT_APPLICABLE, BLOCKED.

| ID | Requirement group | Status | Production / DB / test evidence |
|---|---|---|---|
| T01-R001 | Repository identity, README, under-development status | PARTIALLY_IMPLEMENTED | `README.md` exists but is minimal and lacks required sections. |
| T01-R002 | MIT license file | IMPLEMENTED_AND_TESTED | `LICENSE` contains standard MIT text with 2026 `parazit-IR` owner from repository metadata; `README.md` references MIT License; file audit command passed. |
| T01-R003 | ignore/editor/gitattributes/governance docs | IMPLEMENTED_AND_TESTED | `.gitignore`, `.editorconfig`, `.gitattributes`, and `CONTRIBUTING.md` exist; file audit passed; `JAVA_HOME= ./gradlew check` passed. |
| T01-R004 | no committed secrets/generated files | PARTIALLY_IMPLEMENTED | No tracked real secrets found; `docker/docker-compose.yml` and `application-local.yml` contain dev defaults. |
| T01-R005 | initial commit/repo hygiene | IMPLEMENTED_AND_TESTED | `.gitignore` no longer ignores Gradle wrapper scripts; `.env` remains ignored; `.env.example` remains trackable; file audit passed. |
| T02-R001 | Java/Spring Boot Gradle skeleton | IMPLEMENTED_AND_TESTED | `build.gradle.kts`, `settings.gradle.kts`, `PanelApplication`; `JAVA_HOME= ./gradlew build` PASS. |
| T02-R002 | package skeleton without business behavior | IMPLEMENTED_DIFFERENTLY_BUT_VALID | Later tasks added business modules; early skeleton is superseded. |
| T02-R003 | Java 21 toolchain | IMPLEMENTED_AND_TESTED | Gradle toolchain Java 21; build passes when stale `JAVA_HOME` is unset. |
| T03-R001 | Docker Compose PostgreSQL/Redis/Adminer | IMPLEMENTED_NOT_TESTED | `docker/docker-compose.yml`; not started during audit. |
| T03-R002 | localhost binding and named volumes | IMPLEMENTED_NOT_TESTED | Compose ports bind `127.0.0.1`; named volumes exist. |
| T03-R003 | safe placeholder credentials | PARTIALLY_IMPLEMENTED | Dev defaults are hardcoded; acceptable for local but conflict with strict no-hardcoded credential language. |
| T04-R001 | application configuration profiles | IMPLEMENTED_AND_TESTED | `application.yml`, `application-local.yml`, `application-dev.yml`, `application-prod.yml`; context tests pass. |
| T04-R002 | configuration properties and validation | IMPLEMENTED_AND_TESTED | `config/properties/*Properties`; property tests pass. |
| T04-R003 | secret handling in config | PARTIALLY_IMPLEMENTED | Env placeholders used; local defaults include dev panel/db credentials. |
| T05-R001 | structured logging and trace IDs | IMPLEMENTED_AND_TESTED | `TraceIdFilter`; `TraceIdFilterTest`; logback config. |
| T05-R002 | global exception handling with safe error body | IMPLEMENTED_AND_TESTED | `GlobalExceptionHandler`; `GlobalExceptionHandlerTest`; controller tests assert traceId. |
| T05-R003 | no stack traces/SQL in API errors | IMPLEMENTED_AND_TESTED | exception handler maps known exceptions; controller tests check no `DataIntegrityViolationException`. |
| T06-R001 | BaseEntity UUID/auditing | IMPLEMENTED_AND_TESTED | `BaseEntity`, `JpaAuditingConfiguration`; `BaseEntityPersistenceTest`. |
| T06-R002 | Flyway baseline and PostgreSQL Testcontainers | IMPLEMENTED_AND_TESTED | `V1__create_database_extensions.sql`; Testcontainers support; all tests pass. |
| T06-R003 | Hibernate schema validation | IMPLEMENTED_AND_TESTED | `spring.jpa.hibernate.ddl-auto=validate`; tests/build pass against PostgreSQL. |
| T07-R001 | domain repository abstractions | IMPLEMENTED_AND_TESTED | `BaseRepository`, `UuidRepository`; architecture tests. |
| T07-R002 | Spring Data adapters confined to infrastructure | IMPLEMENTED_AND_TESTED | `JpaRepositoryAdapter`, `SpringDataUuidRepository`; `JpaRepositoryAdapterIntegrationTest`. |
| T07-R003 | no Spring Data leaks to domain/application | IMPLEMENTED_AND_TESTED | `ArchitectureRulesTest#applicationDoesNotDependOnInfrastructureOrSpringDataRepositories`. |
| T08-R001 | constructor DI and clock port | IMPLEMENTED_AND_TESTED | `SystemClockPort`, `ClockConfiguration`, `SystemInfoServiceTest`. |
| T08-R002 | verification endpoint | IMPLEMENTED_AND_TESTED | `DependencyInjectionVerificationController`; controller test. |
| T08-R003 | no field injection/service locator | IMPLEMENTED_AND_TESTED | grep plus `ArchitectureRulesTest#noFieldInjectionOrApplicationContextServiceLocatorUsage`. |
| T09-R001 | User aggregate and invariants | IMPLEMENTED_AND_TESTED | `User`; `UserTest`. |
| T09-R002 | users table constraints | IMPLEMENTED_AND_TESTED | `V2__create_users_table.sql`; unique telegram ID and check constraints; repository integration tests. |
| T09-R003 | user repository adapter | IMPLEMENTED_AND_TESTED | `UserRepositoryAdapter`, `SpringDataUserRepository`; integration tests. |
| T10-R001 | idempotent registration service | IMPLEMENTED_AND_TESTED | `RegisterUserService#register`; `RegisterUserServiceTest`, `RegisterUserIntegrationTest`, concurrent test. |
| T10-R002 | language resolver default semantics | IMPLEMENTED_AND_TESTED | `UserLanguageResolver`; resolver tests. |
| T10-R003 | no Telegram SDK/API integration | IMPLEMENTED_AND_TESTED | command/result records only; architecture scan. |
| T11-R001 | profile read/update use cases | IMPLEMENTED_AND_TESTED | `GetUserProfileService`, `UpdateUserProfileService`; unit/integration/controller tests. |
| T11-R002 | immutable telegram ID/username/status/blocked | IMPLEMENTED_AND_TESTED | update command excludes immutable fields; tests. |
| T11-R003 | profile docs | IMPLEMENTED_NOT_TESTED | `docs/use-cases/user-profile.md`. |
| T12-R001 | dedicated language get/change use cases | IMPLEMENTED_AND_TESTED | `GetUserLanguageService`, `ChangeUserLanguageService`; tests. |
| T12-R002 | strict explicit language validation | IMPLEMENTED_AND_TESTED | `UserLanguageResolver#resolveRequired`; controller/integration tests. |
| T12-R003 | Task 11 language overlap resolved | IMPLEMENTED_DIFFERENTLY_BUT_VALID | profile update command excludes language; dedicated flow owns change. |
| T13-R001 | user settings aggregate/defaults | IMPLEMENTED_AND_TESTED | `UserSettings`, `UserSettingsDefaultsService`; tests. |
| T13-R002 | one settings row per user | IMPLEMENTED_AND_TESTED | `V3__create_user_settings_table.sql` unique `user_id`; concurrent settings test. |
| T13-R003 | settings API and docs | IMPLEMENTED_AND_TESTED | `UserSettingsController`; controller tests; docs file. |
| T14-R001 | referral code and assignment model | IMPLEMENTED_AND_TESTED | `Referral`, `EnsureUserReferralCodeService`, `AssignReferralService`; tests. |
| T14-R002 | unique referral code/one referred assignment | IMPLEMENTED_AND_TESTED | `V4__add_referral_foundation.sql` unique constraints; integration/concurrent tests. |
| T14-R003 | no plan/payment side effects | IMPLEMENTED_AND_TESTED | service boundaries; tests verify language/settings unaffected. |
| T15-R001 | user module quality gate | IMPLEMENTED_AND_TESTED | user integration/controller/concurrency tests; build PASS. |
| T15-R002 | phase docs | IMPLEMENTED_NOT_TESTED | `docs/tasks/phase-2-user-module-acceptance.md`. |
| T16-R001 | Plan aggregate and constraints | IMPLEMENTED_AND_TESTED | `Plan`; `PlanTest`; `V5__create_plans_table.sql`. |
| T16-R002 | plan repository ordering/query methods | IMPLEMENTED_AND_TESTED | `PlanRepositoryAdapter`; repository tests. |
| T16-R003 | no order/payment relations in Plan | IMPLEMENTED_AND_TESTED | architecture test `planTaskDoesNotAddPublicApiSurfaceOrEntityRelationships`. |
| T17-R001 | admin plan CRUD/status transitions | IMPLEMENTED_AND_TESTED | admin services/controllers; unit/integration/controller tests. |
| T17-R002 | code uniqueness and archive semantics | IMPLEMENTED_AND_TESTED | `uq_plans_code`, service tests. |
| T17-R003 | internal API only | IMPLEMENTED_AND_TESTED | `/internal/plans`; controller depends on use cases. |
| T18-R001 | active plan catalog | IMPLEMENTED_AND_TESTED | catalog services/controllers; integration/controller tests. |
| T18-R002 | no inactive/draft/archived exposure | IMPLEMENTED_AND_TESTED | `AvailablePlanRepositoryIntegrationTest`; catalog tests. |
| T18-R003 | no displayOrder exposure | IMPLEMENTED_AND_TESTED | catalog response DTO and tests. |
| T19-R001 | plan selection lifecycle | IMPLEMENTED_AND_TESTED | `PlanSelection`, select/get/clear services; integration tests. |
| T19-R002 | one active selection per user | IMPLEMENTED_AND_TESTED | `V6` partial unique index; concurrent plan selection test. |
| T19-R003 | snapshot plan data | IMPLEMENTED_AND_TESTED | `PlanSelectionSnapshotIntegrationTest`. |
| T20-R001 | plan module quality gate | IMPLEMENTED_AND_TESTED | phase 3 end-to-end tests; `JAVA_HOME= ./gradlew test` PASS. |
| T20-R002 | no payment/order/subscription calls | IMPLEMENTED_DIFFERENTLY_BUT_VALID | Later payment/order modules now exist; plan module remains separated. |
| T21-R001 | 3x-ui foundation config/client/session | IMPLEMENTED_AND_TESTED | `XuiProperties`, `RestClientXuiClient`, `XuiSessionStore`; tests. |
| T21-R002 | broad temporary `XuiClient` methods | CONFLICTING_SPECIFICATION | Later capability ports exist, but legacy broad `XuiClient` remains. |
| T21-R003 | no business provisioning | IMPLEMENTED_DIFFERENTLY_BUT_VALID | Later tasks add provisioning; Task 21 superseded. |
| T22-R001 | authentication/session management | IMPLEMENTED_AND_TESTED | `XuiAuthenticationManager`, `AuthenticatedRequestExecutor`; tests. |
| T22-R002 | no cookie logging | IMPLEMENTED_AND_TESTED | logging interceptor logs method/url/status only; tests. |
| T22-R003 | concurrent login collapse | IMPLEMENTED_AND_TESTED | `XuiAuthenticationManagerTest` uses latches/executor. |
| T23-R001 | inbound discovery and mapping | IMPLEMENTED_AND_TESTED | `XuiInboundClient`, `RestClientXuiInboundClient`; integration tests. |
| T23-R002 | safe inbound DTOs/no secrets | IMPLEMENTED_AND_TESTED | application snapshots and mapper tests. |
| T23-R003 | XUI port refinement | IMPLEMENTED_AND_TESTED | capability ports added; legacy broad port remains under T21 conflict. |
| T24-R001 | XUI client provisioning aggregate/service | IMPLEMENTED_AND_TESTED | `XuiClientProvision`, `CreateVpnClientService`; unit/repository/integration tests. |
| T24-R002 | remote call outside transaction | IMPLEMENTED_AND_TESTED | `PrepareXuiProvisionTransaction` and status transactions separate from `managementClient.createClient`. |
| T24-R003 | multi-instance create safety | IMPLEMENTED_NOT_TESTED | DB uniqueness exists; no named concurrent XUI provisioning integration test. |
| T25-R001 | disable/delete lifecycle | IMPLEMENTED_AND_TESTED | `DisableVpnClientService`, `DeleteVpnClientService`; service/controller/REST client tests. |
| T25-R002 | remote calls outside DB transactions | IMPLEMENTED_AND_TESTED | lifecycle service invokes transaction helpers around remote client calls. |
| T25-R003 | update/delete race tests | IMPLEMENTED_AND_TESTED | `ConcurrentXuiClientOperationIntegrationTest#deleteAndDisableAreRejectedWhileUpdateOperationIsInProgress`; lifecycle prepare checks in-progress operations and uses locked provision reads. |
| T26-R001 | renew/update/enable/reset operations | IMPLEMENTED_AND_TESTED | update command services, `XuiClientOperation`, REST client tests. |
| T26-R002 | operation idempotency table | IMPLEMENTED_AND_TESTED | `V9__create_xui_client_operations.sql`, unique operation ID and in-progress provision index. |
| T26-R003 | exhaustive concurrent operation tests | IMPLEMENTED_AND_TESTED | `ConcurrentXuiClientOperationIntegrationTest#concurrentResetAndRenewPrepareLeaveOnlyOneInProgressOperation`; `#succeededOperationReplaysAndConflictingReplayIsRejected`; operation prepare locks provision rows and preserves replay/fingerprint behavior. |
| T27-R001 | payment method/status/order/payment foundations | IMPLEMENTED_AND_TESTED | `Order`, `Payment`, `PaymentService`; `V10`; tests. |
| T27-R002 | strategy registry | IMPLEMENTED_AND_TESTED | `PaymentProcessor`, `PaymentService#processorFor`; tests. |
| T27-R003 | payment operation history | PARTIALLY_IMPLEMENTED | `PaymentOperation` exists; history semantics remain ambiguous. |
| T28-R001 | Zarinpal request/callback/verify | IMPLEMENTED_AND_TESTED | Zarinpal services/controllers/adapters; integration tests. |
| T28-R002 | callback does not trust amount/order/user/payment ID | IMPLEMENTED_AND_TESTED | callback command uses authority/status; prepare transaction loads local payment/attempt/order. |
| T28-R003 | gateway IO outside transaction | IMPLEMENTED_AND_TESTED | `VerifyZarinpalPaymentService` calls prepare tx, remote gateway, complete tx separately. |
| T28-R004 | one approved payment per order | IMPLEMENTED_AND_TESTED | `uq_payments_one_approved_per_order`; repository tests. |
| T29-R001 | manual CARD_TO_CARD instruction | IMPLEMENTED_AND_TESTED | `ManualCardPaymentProcessor`, instruction services; `V12`; tests. |
| T29-R002 | active amount/payment uniqueness | IMPLEMENTED_AND_TESTED | `uq_manual_card_active_payment`, `uq_manual_card_active_amount`; repository/integration tests. |
| T29-R003 | no full card/CVV persistence | IMPLEMENTED_AND_TESTED | masked snapshot only in migration/domain; `ManualPaymentProperties#toString` redacts. |
| T30-R001 | receipt upload and review queue | IMPLEMENTED_AND_TESTED | receipt services/controllers/storage; `V13`; integration tests. |
| T30-R002 | file IO outside DB transaction | IMPLEMENTED_AND_TESTED | `SubmitManualPaymentReceiptService` inspect/store occurs between prepare/complete transaction beans. |
| T30-R003 | duplicate receipt upload concurrency | IMPLEMENTED_AND_TESTED | `ConcurrentManualPaymentReceiptSubmissionIntegrationTest`. |
| T31-R001 | manual review claim/approve/reject/release | IMPLEMENTED_AND_TESTED | review services/controllers; `V14`; domain tests. |
| T31-R002 | centralized payment approval | IMPLEMENTED_AND_TESTED | `PaymentApprovalService` called by manual and Zarinpal completion. |
| T31-R003 | approval creates durable outbox | IMPLEMENTED_AND_TESTED | `PaymentApprovalService#createOutbox`; `provisioning_outbox` unique order/type. |
| T31-R004 | multi-instance outbox worker safety | IMPLEMENTED_AND_TESTED | `ProvisioningOutboxRepository#claimAvailableByEventId`; native PostgreSQL `FOR UPDATE SKIP LOCKED` claim in `ProvisioningOutboxRepositoryAdapter`; `ConcurrentProvisioningOutboxClaimIntegrationTest#onlyOneWorkerClaimsOutboxEventWhenTransactionsOverlap`. |
| T31-R005 | deterministic approval/rejection race tests | IMPLEMENTED_AND_TESTED | `ConcurrentPaymentApprovalIntegrationTest#twoOperatorsClaimingSameManualReviewLeaveOneClaimOwner`; `#concurrentApproveAndRejectForSameManualReviewLeaveOneTerminalDecision`; `#zarinpalAndManualApprovalForSameOrderAllowOnlyOneApprovedPayment`; `ConcurrentProvisioningOutboxClaimIntegrationTest#onlyOneWorkerClaimsOutboxEventWhenTransactionsOverlap`. |
| T32-R001 | full payment integration quality gate | IMPLEMENTED_AND_TESTED | payment/provisioning integration tests pass; required payment/review/outbox concurrency scenarios are covered by PostgreSQL integration tests. |
| T32-R002 | security scan/no secrets | PARTIALLY_IMPLEMENTED | no tracked real secrets found; dev credentials and untracked logs noted. |
| T32-R003 | build/test execution | IMPLEMENTED_AND_TESTED | 467 tests passed; `check`, `build`, Jacoco tasks passed. |
| T32-R004 | docs and remediation readiness | IMPLEMENTED_NOT_TESTED | docs exist; this audit supplies remediation plan. |

## Final Verification Addendum — 2026-07-13

The final verification reran the full suite and focused slices against the current repository state. The traceability statuses above remain current except where final runtime evidence is stronger than the original audit:
- `T25-R003`, `T26-R003`, `T31-R004`, and `T31-R005` are verified by deterministic PostgreSQL concurrency tests.
- `T32-R003` is verified by `JAVA_HOME= ./gradlew clean test`, `check`, `build`, Jacoco report, coverage verification, and final `test --rerun-tasks`.
- `T32-R002` remains `PARTIALLY_IMPLEMENTED` because no active tracked production secret was verified, but dev/default credential literals, untracked local logs, and temporary header-based operator identity remain open P2 gaps.
