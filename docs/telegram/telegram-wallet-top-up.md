# Telegram Wallet Top-Up

Telegram flow:

```mermaid
flowchart TD
    Wallet[Wallet] --> Start[Increase balance]
    Start --> Amount[Enter amount]
    Amount --> Invoice[Top-up pre-invoice]
    Invoice --> Manual[Manual card payment]
    Invoice --> Online[Zarinpal payment]
    Manual --> Review[Receipt approval]
    Online --> Verify[Provider verification]
    Review --> Credit[Wallet credited]
    Verify --> Credit
    Credit --> Success[Wallet balance/status]
```

Callback payloads carry only signed, user-bound request identifiers. Amount, payment method availability, wallet ownership, and credit amount are reloaded from server-side state.

Top-up status shows customer-facing state only and does not expose provider authorities, internal IDs, idempotency keys, card details in logs, or raw enum names.
