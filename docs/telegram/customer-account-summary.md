# Customer account summary

Task 43 adds a customer-facing account page reachable through `/account`, `/profile`, and the My Services page.

The page shows trusted account data only:

- Telegram ID, when enabled by `app.telegram.customer-account.show-telegram-id`.
- Display name from the registered Telegram user profile.
- Registration date from the local `User` record.
- Service counts from persisted subscriptions.
- Successful and pending payment counts from persisted payments.

Future commercial fields such as wallet balance, referral code, phone verification, discount usage, and customer group are omitted unless an implemented module supplies trusted data. The page does not display the internal user UUID.

```mermaid
flowchart TD
    A[/account or profile button] --> B[Resolve Telegram user]
    B --> C[Load account projection]
    C --> D[Load bounded account statistics]
    D --> E[Render localized summary]
    E --> F[Inline buttons: My Services, Payments, Settings, Home]
```

Count definitions:

- Total services: persisted subscriptions for the user.
- Active services: subscriptions with `ACTIVE` status.
- Expired services: subscriptions with `EXPIRED` status.
- Successful payments: payments with `APPROVED` status.
- Pending payments: non-terminal in-progress payment statuses.

Privacy rules:

- No database UUID is shown.
- No raw phone number is shown.
- No wallet/referral/discount value is fabricated.
