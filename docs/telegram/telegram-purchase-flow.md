# Telegram Purchase Flow

```mermaid
flowchart TD
    A[Persistent main menu] --> B[Buy subscription]
    B --> C{Sales enabled?}
    C -- No --> D[Disabled sales message]
    C -- Yes --> E[Plan catalog]
    E --> F[Plan details]
    F --> G[Select plan]
    G --> H[PlanSelection snapshot]
    H --> I[Pre-invoice]
    I --> J[Pay and receive service]
    J --> K[Create or reuse Order]
    K --> L[Payment methods]
    L --> M[Select method]
    M --> N[Create or reuse Payment]
```

Task 44 uses these state boundaries:

- Plan catalog and plan details are read-only.
- Selecting a plan creates or reuses a `PlanSelection` and a Telegram purchase session.
- Pre-invoice is rendered from the `PlanSelection` snapshot.
- Continuing to payment creates or reuses one order for that selection.
- Viewing payment methods does not create a payment.
- Selecting a payment method creates or reuses a payment for that method.

```mermaid
flowchart TD
    A[Plan selected] --> B{Plan active before order?}
    B -- No --> C[Reject and return to plans]
    B -- Yes --> D[Create order from snapshot]
    D --> E{Plan price changes later?}
    E -- Yes --> F[Existing order amount unchanged]
```

Callbacks are signed and expiring. They carry only bounded server-side references.
