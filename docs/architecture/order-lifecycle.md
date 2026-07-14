# Order Lifecycle

Orders are the payment source of truth.

New subscription:

```mermaid
flowchart TD
    A[PlanSelection NEW_SUBSCRIPTION] --> B[OrderType.NEW_SUBSCRIPTION]
    B --> C[Payment]
    C --> D[Approved]
    D --> E[Provisioning outbox]
    E --> F[Subscription created after provisioning]
```

Renewal after Task 46:

```mermaid
flowchart TD
    A[PlanSelection RENEWAL] --> B[OrderType.RENEWAL]
    B --> C[Payment]
    C --> D[Approved]
    D --> E[Order RENEWAL_PENDING]
    E --> F[Renewal outbox PENDING]
    F --> G[Task 46 stop before remote renewal]
```

Dispatch rule:

```text
OrderType.NEW_SUBSCRIPTION -> existing new-service provisioning
OrderType.RENEWAL -> renewal outbox only
```

Renewal orders must never enter the new-subscription provisioning worker. A renewal in `RENEWAL_REVIEW_REQUIRED` means payment is approved but the target failed safety validation and needs operator review.

After Task 47, a paid Renewal Order moves from `RENEWAL_PENDING` to `COMPLETED` only when the existing remote client is updated and verified. Unsafe execution states move to `RENEWAL_REVIEW_REQUIRED`; approved payment remains auditable.
