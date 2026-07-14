# Wallet Order Payment

Task 50 adds Wallet as an internal `PaymentMethod` for existing `ORDER` payment targets.

```mermaid
sequenceDiagram
  participant T as Telegram
  participant W as PayOrderWithWalletService
  participant L as Wallet Ledger
  participant P as Payment
  participant D as OrderPaymentApprovedDispatcher
  T->>W: confirm wallet payment(orderId)
  W->>W: reload user/order/payments
  W->>L: debit wallet with wallet-purchase:{orderId}
  W->>P: create WALLET payment and approve
  W->>D: dispatch paid order
  D-->>W: provisioning outbox or renewal outbox
```

The trusted payable amount is always `orders.final_amount` and `orders.currency`.
Telegram callbacks never carry amount, balance, plan price, or currency.

`OrderType.NEW_SUBSCRIPTION` continues to queue the existing provisioning flow.
`OrderType.RENEWAL` continues to queue the existing renewal outbox flow.

Task 50 does not implement refunds, split payments, gift/referral/discount logic, or compensation after downstream failure.
