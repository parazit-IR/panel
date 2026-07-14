# Wallet Payment Concurrency

Wallet payment relies on database correctness, not JVM locks.

- The Order row is loaded with a write lock before payment.
- The Wallet row is loaded with a write lock by the existing debit use case.
- The ledger has a unique wallet/idempotency key constraint.
- Payments have a partial uniqueness rule for approved Order payments.
- Provisioning and renewal dispatch paths have their own unique outbox keys.

```mermaid
sequenceDiagram
  participant A as Click A
  participant B as Click B
  participant DB as PostgreSQL
  A->>DB: lock order
  B->>DB: wait for order lock
  A->>DB: debit wallet, approve payment, dispatch
  A-->>DB: commit
  B->>DB: reload order/payments
  B-->>B: already paid, no debit
```

If wallet payment races with an active external payment, the wallet path rejects with a safe conflict result unless an approved payment already exists.
