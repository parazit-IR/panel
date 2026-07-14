# Promotion Concurrency

Promotion writes rely on database locking and uniqueness.

```mermaid
sequenceDiagram
  participant U1 as User A
  participant U2 as User B
  participant DB as PostgreSQL
  U1->>DB: lock code
  U2->>DB: wait/skip until lock released
  U1->>DB: reserve usage and redemption
  U2->>DB: recheck used count
```

Concurrency guarantees:

- Usage limits are checked while the code row is locked.
- Order discount changes are made while the order row is locked.
- Wallet credit uses the existing wallet row lock and ledger uniqueness.
- No JVM synchronization is used for financial correctness.

