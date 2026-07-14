# Telegram Renewal Flow

The `♻️ تمدید سرویس` main-menu action opens the renewal flow.

```mermaid
flowchart TD
    A[Main Menu] --> B[Renewable Services]
    B --> C[Renewal Target]
    C --> D[Renewal Plans]
    D --> E[Renewal Pre-invoice]
    E --> F[Payment Methods]
```

Back behavior:

- Payment Methods -> Renewal Pre-invoice
- Renewal Pre-invoice -> Renewal Plans
- Renewal Plans -> Renewal Target
- Renewal Target -> Renewable Services

```mermaid
sequenceDiagram
    participant U as User
    participant B as Telegram bot
    participant A as Renewal use cases
    U->>B: Renew Service
    B->>A: list owned renewable services
    A-->>B: safe summaries
    U->>B: select service
    B->>A: get details and list plans
    U->>B: select plan
    B->>A: create or reuse renewal selection/session
    B-->>U: trusted pre-invoice
    U->>B: confirm
    B->>A: create or reuse renewal order
    B-->>U: existing payment-method selection
```

Callbacks are signed, time-bounded, and user-bound. They do not contain price, traffic values, expiry, service username, token, XUI ID, or provider data. Plan selection uses server-side plan reload rather than trusting callback-carried financial values.

Expired selections render the localized expired pre-invoice message and require selecting the service and plan again.

Task 46 post-payment boundary:

```mermaid
flowchart TD
    A[Renewal payment approved] --> B[Renewal queued message]
    B --> C[Refresh renewal status]
    A --> D[Renewal outbox PENDING]
    D --> Stop[Stop before Task 47]
    Stop --> X[No subscription mutation]
    Stop --> Y[No 3x-ui mutation]
```

Task 47 completion boundary:

```mermaid
flowchart TD
    A[Renewal outbox PENDING] --> B[Existing 3x-ui client updated]
    B --> C[Local subscription/provision refreshed]
    C --> D[Success notification]
    B --> E[Manual review on unsafe failure]
```
