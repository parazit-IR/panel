# Promotion Reconciliation

Task 51 adds read-only promotion reconciliation support.

Checks should compare:

- discount code `used_count` against reserved/consumed redemption rows;
- gift code `used_count` against applied gift redemption rows;
- order `discount_amount`/`final_amount` against the discount redemption;
- gift redemption wallet transaction against the wallet ledger row.

Task 51 does not auto-correct mismatches, refund, or reverse payments.

