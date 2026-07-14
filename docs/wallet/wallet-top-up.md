# Wallet Top-Up

Task 49 enables customer wallet top-up through existing payment methods.

## Lifecycle

```mermaid
stateDiagram-v2
    [*] --> AWAITING_PAYMENT_METHOD
    AWAITING_PAYMENT_METHOD --> PENDING_PAYMENT: payment method selected
    PENDING_PAYMENT --> PAYMENT_APPROVED: verified payment
    PAYMENT_APPROVED --> CREDITED: wallet ledger credit posted
    AWAITING_PAYMENT_METHOD --> EXPIRED
    AWAITING_PAYMENT_METHOD --> CANCELLED
    PENDING_PAYMENT --> FAILED: invalid approved target
```

The requested amount is immutable after request creation. Payment amount is always loaded from `WalletTopUpRequest.requestedAmount`, never from Telegram callback data or provider callbacks.

## Scope

Task 49 implements top-up only. It does not implement wallet purchases, refunds, gift codes, referrals, discounts, cashback, transfers, withdrawal, or Task 50 behavior.
