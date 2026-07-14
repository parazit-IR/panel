# Renewal Domain

Task 45 models renewal as an `OrderType.RENEWAL` order that targets an existing `Subscription`. It reuses the existing PlanSelection, Telegram purchase session, Order, and Payment handoff instead of creating a parallel renewal payment system.

Renewal stops at payment-method selection. Payment approval, subscription mutation, renewal outbox, and remote 3x-ui changes are Task 46+ concerns.

```mermaid
flowchart TD
    A[Renew Service] --> B[List owned renewable subscriptions]
    B --> C[Select target subscription]
    C --> D[List renewal-enabled compatible plans]
    D --> E[Select plan and create renewal selection]
    E --> F[Render trusted pre-invoice]
    F --> G[Confirm renewal order]
    G --> H[Create or reuse OrderType.RENEWAL]
    H --> I[Show existing payment methods]
    I --> J[Task 45 boundary]
```

New purchase and renewal share infrastructure but are separated by explicit type fields:

- `Order.type`: `NEW_SUBSCRIPTION` or `RENEWAL`
- `PlanSelection.selectionType`: `NEW_SUBSCRIPTION` or `RENEWAL`
- `TelegramPurchaseSession.flowType`: `NEW_SUBSCRIPTION` or `RENEWAL`

```mermaid
flowchart LR
    P[Payment approved] --> T{Order type}
    T -->|NEW_SUBSCRIPTION| N[Existing provisioning outbox]
    T -->|RENEWAL| R[Renewal path not implemented in Task 45]
R --> Stop[No subscription mutation, no XUI call]
```

Task 47 applies the paid renewal to the existing XUI client only after a Renewal Outbox row is claimed. It updates local Subscription and provision state after remote success and writes immutable renewal history.
