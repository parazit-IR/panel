# Renewal Worker

Task 47 processes `renewal_outbox` rows created after approved renewal payments.

```mermaid
flowchart TD
  A[Pending Renewal Outbox] --> B[Claim with SKIP LOCKED]
  B --> C[Deserialize trusted payload]
  C --> D[Calculate or load deterministic target]
  D --> E[Update existing 3x-ui client]
  E --> F[Verify remote state]
  F --> G[Commit local subscription/provision/order/history]
  G --> H[Mark outbox processed]
```

The scheduler is controlled by `app.renewal.worker.*`. It claims rows in short transactions and never holds a database lock while calling 3x-ui.
