# Wallet Payment Idempotency

Wallet order payment uses layered idempotency:

- Wallet debit: `wallet-purchase:{orderId}` on `(wallet_id, idempotency_key)`.
- Payment: one approved `WALLET` payment per Order through the existing approved-payment uniqueness rule.
- Dispatch: existing provisioning and renewal outbox uniqueness.
- Telegram: signed, expiring, user-bound callback plus one-time sensitive confirmation.

```mermaid
flowchart TD
  A[Confirm wallet payment] --> B{Approved payment exists?}
  B -- yes --> C[Return already paid]
  B -- no --> D[Debit wallet by idempotency key]
  D --> E{Ledger exists?}
  E -- same request --> C
  E -- conflict --> F[Reject]
  E -- new --> G[Create approved wallet payment]
  G --> H[Dispatch paid order]
```

Replays from Telegram retries, duplicate button clicks, app restart, or repeated dispatcher calls must not create a second debit or second downstream outbox.
