# Phase 3 Plan Module Acceptance

## Acceptance Checklist

- [x] Plan aggregate complete.
- [x] Plan lifecycle enforced.
- [x] Plan code unique and immutable.
- [x] Money uses integer Toman representation.
- [x] Traffic uses bytes.
- [x] Admin create works.
- [x] Admin read works.
- [x] Admin update works.
- [x] Admin activate/deactivate/archive works.
- [x] No physical Plan delete endpoint exists.
- [x] Only `ACTIVE` Plans appear in catalog.
- [x] Hidden Plans behave as not found.
- [x] User can select an `ACTIVE` Plan.
- [x] Same selection is idempotent.
- [x] Different selection replaces the prior `ACTIVE` selection.
- [x] Exactly one `ACTIVE` selection exists per User.
- [x] Snapshot remains stable after Plan update.
- [x] New selection after clearing uses updated Plan values.
- [x] Expiration works at the exact boundary.
- [x] Ineligible Users cannot select.
- [x] Concurrent Plan creation is safe.
- [x] Concurrent selection of the same Plan is safe.
- [x] Concurrent selection of different Plans is safe.
- [x] Concurrent replacement of an existing selection is safe.
- [x] Flyway passes.
- [x] Hibernate validation passes.
- [x] Unit tests pass.
- [x] Integration tests pass.
- [x] Controller tests pass.
- [x] Concurrency tests pass.
- [x] Architecture checks pass.
- [x] JaCoCo report is generated.
- [x] No Order implemented.
- [x] No Payment implemented.
- [x] No Subscription implemented.
- [x] No Telegram handler implemented.
- [x] No 3x-ui integration implemented.
- [x] Documentation updated.
- [x] Full build passes.

## Verified Phase 3 Flow

The quality gate verifies:

1. Admin creates a `DRAFT` Plan.
2. Catalog does not expose the draft Plan.
3. Admin activates the Plan.
4. Catalog exposes the active Plan.
5. User selects the active Plan.
6. Selection stores a snapshot.
7. Admin updates Plan name, price, duration, traffic, and max devices.
8. Catalog shows updated Plan values.
9. Existing selection still shows old snapshot values.
10. User clears the selection.
11. User selects the updated Plan.
12. New selection stores updated snapshot values.
13. Admin deactivates the Plan.
14. Catalog hides the Plan.
15. New selection attempt fails as not found.
16. Existing selection remains readable until expiration or clear.
17. Admin archives the Plan.
18. Admin lookup still sees archived Plan.
19. Archived Plan cannot be edited.
20. No deferred operational tables for orders, payments, subscriptions, or VPN clients are created.

## Verification Commands

Verified using:

```bash
JAVA_HOME=/home/kamali/.sdkman/candidates/java/21.0.1-oracle ./gradlew clean test --no-daemon
JAVA_HOME=/home/kamali/.sdkman/candidates/java/21.0.1-oracle ./gradlew jacocoTestReport --no-daemon
JAVA_HOME=/home/kamali/.sdkman/candidates/java/21.0.1-oracle ./gradlew check --no-daemon
JAVA_HOME=/home/kamali/.sdkman/candidates/java/21.0.1-oracle ./gradlew build --no-daemon
```

There is no separate Gradle `integrationTest` task in the current build.

## Deferred Scope

Phase 3 intentionally does not include:

- Order creation
- Payment creation
- Subscription creation
- Telegram handlers
- 3x-ui integration
- VPN provisioning
- admin dashboard
- cron cleanup
- discounts
- coupons
- trial plans
- localized plan content
