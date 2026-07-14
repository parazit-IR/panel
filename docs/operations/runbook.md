# Operations Runbook

## Wallet Reconciliation

Use `ReconcileWalletBalanceUseCase` or a focused operational test/tool to compare stored wallet balance with the immutable ledger.

Expected normal state:

```text
storedBalance == ledgerCalculatedBalance
consistent == true
```

If reconciliation fails:

1. Do not edit ledger rows.
2. Preserve all existing entries for audit.
3. Investigate duplicate references, failed deployments, or manual database edits.
4. Apply a future compensating adjustment workflow when available.

Task 48 does not implement admin dashboard adjustments, refunds, wallet top-up, or wallet purchase.

## Wallet Payment Checks

For wallet order payment incidents, reconcile the Order, approved WALLET Payment, and PURCHASE wallet transaction. Do not issue automatic refunds in Task 50; investigate inconsistencies and use manual operational correction policy.
