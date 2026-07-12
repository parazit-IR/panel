# Payment Foundation

Task 27 introduced the local payment foundation only. Task 28 added Zarinpal request/callback/verification. Task 29 adds manual card-to-card payment instructions with unique payable amounts. Manual receipt upload, operator approval, subscriptions, Telegram handlers, and VPN provisioning remain deferred.

## Architecture

Payments are modeled as a domain aggregate and accessed through application ports. Controllers depend on input ports, application services depend on domain repositories and payment processor ports, and Spring Data remains in infrastructure adapters.

```mermaid
flowchart LR
    API[Internal Payment API] --> UseCase[Payment Use Cases]
    UseCase --> PaymentRepo[PaymentRepository]
    UseCase --> OrderRepo[OrderRepository]
    UseCase --> Strategy[PaymentProcessor port]
    PaymentRepo --> JpaPayment[Spring Data adapter]
    OrderRepo --> JpaOrder[Spring Data adapter]
    Strategy -. future .-> Zarinpal[Zarinpal processor]
    Strategy -. future .-> Card[Card-to-card processor]
```

## Strategy Pattern

`PaymentProcessor` defines the future provider boundary:

- `supportedMethod()`
- `initiate(PaymentInitializationCommand)`
- `verify(PaymentVerificationCommand)`

`PaymentService` receives all processors from Spring and builds a method-to-processor map. Business logic does not switch on `PaymentMethod`; adding a provider later means adding a new processor bean.

Task 28 adds the Zarinpal processor and provider-specific attempt model without changing the generic payment aggregate.

Task 29 adds the manual card processor and provider-specific instruction model. Manual card payment uses dedicated instruction use cases because it does not have meaningful online gateway verification in this phase.

## State Machine

Payments start as `CREATED`.

```mermaid
stateDiagram-v2
    [*] --> CREATED
    CREATED --> WAITING_FOR_PAYMENT
    CREATED --> PROCESSING
    CREATED --> EXPIRED
    CREATED --> CANCELLED
    CREATED --> FAILED
    WAITING_FOR_PAYMENT --> PROCESSING
    WAITING_FOR_PAYMENT --> APPROVED
    WAITING_FOR_PAYMENT --> REJECTED
    WAITING_FOR_PAYMENT --> EXPIRED
    WAITING_FOR_PAYMENT --> CANCELLED
    WAITING_FOR_PAYMENT --> FAILED
    PROCESSING --> APPROVED
    PROCESSING --> REJECTED
    PROCESSING --> FAILED
    APPROVED --> [*]
    REJECTED --> [*]
    EXPIRED --> [*]
    FAILED --> [*]
    CANCELLED --> [*]
```

There is no generic status setter. Domain methods enforce transitions.

## Persistence

Migration `V10__create_orders_and_payments.sql` creates:

- `orders`, a minimal local aggregate used only as the payment parent.
- `payments`, with method/status stored as strings.
- `payment_operations`, an append-only operation history table.

The database enforces:

- `payable_amount >= base_amount`
- valid method and status values
- foreign keys to `orders` and `users`
- one approved payment per order through a partial unique index

## Internal API

Temporary internal endpoints:

- `POST /internal/payments`
- `GET /internal/payments/{id}`
- `GET /internal/orders/{id}/payments`

These endpoints create and read local payment records only. Provider-specific endpoints initialize Zarinpal or manual-card instructions. No endpoint in Task 29 approves a manual payment, uploads receipts, or provisions VPN clients.

## Payment Operation History

`PaymentOperation` records local payment events such as creation and future initialization/verification attempts. Messages are bounded and sanitized. Raw gateway responses are not stored.

## Future Providers

Provider-specific classes implement `PaymentProcessor` for registration, while dedicated use cases handle provider-specific flows such as Zarinpal callbacks or manual-card instructions. The application service can select processors automatically using `supportedMethod()`.
