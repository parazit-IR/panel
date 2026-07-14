# Telegram Wallet

Task 49 enables a customer wallet page, transaction history, and wallet top-up through existing payment flows.

## Flow

```mermaid
flowchart TD
    Main[Main menu] --> Wallet[Wallet summary]
    Wallet --> History[Transaction history]
    Wallet --> TopUp[Enter top-up amount]
    TopUp --> Payment[Select payment method]
    Payment --> Status[Top-up status]
    History --> Wallet
    Wallet --> Home[Main menu]
```

## Customer UI

The wallet page shows:

- current balance;
- transaction count;
- last transaction date;
- transaction history button;
- top-up button;
- home navigation.

The history page shows customer-friendly credit/debit labels, amount, balance after transaction, and occurred date. It does not expose wallet IDs, transaction IDs, references, idempotency keys, or raw enum names.

Top-up creates a `WalletTopUpRequest` and then a typed `Payment` target. The wallet is credited only after verified payment approval posts a `TOP_UP` ledger transaction.
## Gift Codes

Task 51 adds a Wallet action for gift-code redemption. The bot asks for the code, passes it to the promotion use case, and displays the credited amount and updated balance. No payment or order is created.
