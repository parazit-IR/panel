# Renewal Order

Task 45 extends the existing Order aggregate. `OrderType.RENEWAL` is used for renewal orders and references the target subscription through `target_subscription_id`.

Invariants:

- Renewal order must have `targetSubscriptionId`.
- Renewal order must have `renewalSnapshot`.
- Snapshot target subscription, source plan, amount, and currency must match the order.
- Renewal order does not require new-subscription provisioning.
- Renewal order cannot create a new subscription, XUI client, or outbox in Task 45.

```mermaid
flowchart TD
    A[Confirm renewal] --> B[Load active renewal session]
    B --> C[Load renewal selection]
    C --> D[Recheck owner, eligibility, plan compatibility]
    D --> E{Existing order for selection or target?}
    E -- Yes --> R[Reuse order]
    E -- No --> N[Create OrderType.RENEWAL]
    N --> S[Store RenewalSnapshot]
    R --> P[Attach order to session]
    S --> P
    P --> M[Show payment methods]
```

Duplicate confirmation is protected by repository lookup and database uniqueness for active renewal orders per target subscription.
