# Wallet Ledger

`wallet_transactions` is the immutable audit ledger for wallet balance changes.

## Rules

- Every posted balance change has one ledger row.
- Ledger rows are append-only from business flows.
- `balance_before` and `balance_after` are captured at posting time.
- Rejected debits and idempotency conflicts do not create ledger rows.
- Corrections must be represented by compensating entries in a later task, not by editing history.

```mermaid
flowchart LR
    Command[Credit or debit command] --> Lock[Lock wallet row]
    Lock --> Check[Validate active wallet and idempotency]
    Check --> Entry[Insert WalletTransaction]
    Entry --> Update[Update Wallet.balance]
    Update --> Commit[Commit atomically]
```

## Constraints

PostgreSQL enforces non-negative balances, positive transaction amounts, one idempotency key per wallet, and credit/debit balance equations.

## Task 50 PURCHASE Debits

Wallet purchases are recorded as `WalletTransactionType.PURCHASE` with direction `DEBIT`, reference type `ORDER`, and reference id equal to the paid Order id. The ledger idempotency key is `wallet-purchase:{orderId}`.
## Gift Code Credits

Gift-code redemption credits the wallet through the immutable ledger using `WalletTransactionType.GIFT_CODE`. The ledger reference points to the promotion redemption record, not raw code text.
