# Specification Conflicts

Audit date: 2026-07-13

## Task Inventory

| Check | Result | Evidence |
|---|---:|---|
| Tasks 1 through 32 exist | PASS | `docs/tasks/tasks/task1.txt` through `task32.txt` are present. |
| Duplicate task numbers | PASS | Numeric filename scan found each number once. |
| Empty task files | PASS | No zero-byte or zero-line task files. |
| Suspiciously short task files | NOT_APPLICABLE | Tasks 2-5 are short setup specs (59, 68, 87, 79 lines) but contain coherent setup requirements. |

## Conflicts And Overrides

| ID | Severity | Type | Description | Evidence | Correct Interpretation |
|---|---|---|---|---|---|
| SC-001 | Info | Payment method naming | `CARD_TO_CARD_MANUAL` was explicitly checked and not found. | `grep` over `docs/tasks/tasks`, `src/main`, `src/test`; `PaymentMethod` contains `ZARINPAL`, `CARD_TO_CARD`. | No implementation defect. `CARD_TO_CARD` is canonical. |
| SC-002 | Medium | Outdated XUI requirement | Task 21 allowed temporary unsupported methods on a broad `XuiClient`; Tasks 23-26 require capability-separated ports. The legacy broad port remains. | `application/port/out/xui/XuiClient.java`; `RestClientXuiClientIntegrationTest#unsupportedOperationsThrow`. | Keep capability ports and remove or quarantine legacy `XuiClient`. |
| SC-003 | Low | Ambiguous payment history | Task 27 requires Payment Operation History but does not fully define replay/fingerprint/history semantics. | `PaymentOperation` has append-only records and `findAllByPaymentIdOrderByOccurredAtAsc`, but no idempotency key. | Treat as audit history, not idempotency history, unless later specs clarify. |
| SC-004 | Medium | Endpoint behavior changed | Early tasks repeatedly required no authentication. Later Spring Security dependency changes all endpoints except health/Zarinpal callback to authenticated. | `SecurityConfiguration#securityFilterChain`. | Security is intentional but docs/tests must not describe internal endpoints as unauthenticated. |
| SC-005 | Low | Docker/local credentials | Specs prohibit committed real credentials; Docker/local config contains obvious development defaults. | `docker/docker-compose.yml`; `application-local.yml`. | Accept as dev placeholders only; document and keep production env-only. |
| SC-006 | Low | Property package consistency | XUI properties are under `infrastructure.xui.config`, while most properties are under `config.properties`. | `XuiProperties`, `XuiClientLifecycleProperties`, `PaymentProperties`, `DatabaseProperties`. | Consolidate configuration-property package if project convention requires it. |
| SC-007 | Low | Task ordering | Order/payment foundations are present before Task 31, while Task 31 frames order finalization as being introduced there. | `V10__create_orders_and_payments.sql`; `Order` domain; Task 28 references order validation. | Later tasks override earlier “do not implement order” prohibitions. |
| SC-008 | Info | User language ownership | Task 11 allowed profile language update; Task 12 dedicated language management superseded that. | `UpdateUserProfileCommand` has no language; `ChangeUserLanguageService` owns changes. | Implementation follows later Task 12. |
| SC-009 | Info | Migration numbering | No migration-number mismatch was found. | `V1` through `V14` exist in order. | No defect. |
| SC-010 | Medium | Temporary operator identity | Task 30/32 allow header-based operator identity temporarily; Spring Security now exists but operator identity remains header based. | `HeaderBasedCurrentOperatorProvider#currentOperatorId`; `SecurityConfiguration`. | Document as temporary and replace with authenticated principal mapping. |

## PaymentMethod Canonical Decision

Status: IMPLEMENTED_AND_TESTED

Evidence:
- Production: `src/main/java/com/parazit/panel/domain/payment/PaymentMethod.java` declares `ZARINPAL` and `CARD_TO_CARD` only.
- Database: `V10__create_orders_and_payments.sql` check constraint `chk_payments_method` allows `ZARINPAL`, `CARD_TO_CARD`.
- Manual payment production: `ManualCardPaymentProcessor#supportedMethod` returns `PaymentMethod.CARD_TO_CARD`.
- Tests: manual payment integration and repository tests create `PaymentMethod.CARD_TO_CARD` payments.

## Final Verification Addendum — 2026-07-13

Final verification repeated the `CARD_TO_CARD_MANUAL` scan across `docs/tasks/tasks`, `src/main`, `src/test`, and `src/main/resources`; no matches were found. The remaining specification-conflict count is 10, with medium-severity conflicts still open for the legacy broad XUI port, Spring Security endpoint semantics, and temporary header-based operator identity.
