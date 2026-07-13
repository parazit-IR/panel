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

Renewal in Task 45:

```mermaid
flowchart TD
    A[PlanSelection RENEWAL] --> B[OrderType.RENEWAL]
    B --> C[Payment method handoff]
    C --> D[Task 45 stop]
    D --> E[No provisioning outbox]
    D --> F[No new subscription]
```

Dispatch rule:

```text
OrderType.NEW_SUBSCRIPTION -> existing new-service provisioning
OrderType.RENEWAL -> renewal path not implemented in Task 45
```
