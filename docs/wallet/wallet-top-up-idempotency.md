# Wallet Top-Up Idempotency

Task 49 uses layered idempotency:

- request creation: `user_id + wallet-top-up-request:{requestId}`;
- payment creation: one linked payment per top-up request;
- manual instruction/Zarinpal initialization: existing provider request IDs;
- approval credit: wallet ledger key `wallet-top-up:{topUpRequestId}`;
- ledger uniqueness: `wallet_id + idempotency_key`.

```mermaid
flowchart TD
    A[Duplicate Telegram amount update] --> B[Same top-up request]
    C[Duplicate method callback] --> D[Same payment]
    E[Duplicate manual/Zarinpal approval] --> F[Same TOP_UP ledger row]
    F --> G[One balance increase]
```

Conflicting idempotency-key reuse is rejected without mutating wallet balance.
