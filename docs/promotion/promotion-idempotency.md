# Promotion Idempotency

Discount idempotency:

- `(order_id, discount_code_id)` allows one active reservation or consumed redemption for an order/code pair.
- Replaying payment approval consumes the same redemption and does not alter the paid amount.
- Discount removal before payment releases the reservation and preserves audit history.

Gift-code idempotency:

- `(user_id, gift_code_id)` prevents duplicate gift redemption for the same user.
- Wallet credit uses `gift-code:{redemptionId}` as the immutable ledger idempotency key.

Telegram update idempotency remains a transport-level guard, not the financial source of truth.

