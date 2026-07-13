# Renewal Eligibility

`RenewableSubscriptionPolicy` decides whether a customer-owned subscription can enter renewal. It never trusts Telegram callback data for ownership, status, expiry, traffic, or amount.

Eligible by default:

- Active subscription with successful local provision.
- Expired subscription with successful local provision and remote client reference.

Rejected by default:

- Renewal sales disabled.
- Ownership mismatch.
- Provisioning, failed, revoked, invalid, suspended, or missing provision state.
- Missing remote client reference.
- Existing active renewal order unless the caller is explicitly reusing it.

```mermaid
flowchart TD
    A[Load User-owned Subscription] --> B{Renewal enabled?}
    B -- No --> X[Reject]
    B -- Yes --> C{Owner matches?}
    C -- No --> X
    C -- Yes --> D{Active renewal order?}
    D -- Yes --> X
    D -- No --> E{Status allowed?}
    E -- No --> X
    E -- Yes --> F{Provision active?}
    F -- No --> X
    F -- Yes --> G{Remote client ref present?}
    G -- No --> X
    G -- Yes --> OK[Renewable]
```

Messages use localization keys. Internal enum names are not displayed to customers.
