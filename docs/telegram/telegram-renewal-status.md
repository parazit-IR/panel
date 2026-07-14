# Telegram Renewal Status

After a renewal payment is approved and the outbox is created, customers see:

```text
✅ پرداخت تمدید شما تأیید شد.

♻️ تمدید سرویس در حال انجام است.
```

The bot must not say the service was renewed until Task 47 applies the remote update and subscription state changes.

Customer-facing states:

- در انتظار پرداخت
- پرداخت تأیید شد
- تمدید در صف اجرا
- نیازمند بررسی
- پرداخت لغوشده
- پرداخت منقضی‌شده

The refresh callback is signed, user-bound, and reloads local trusted order/payment/outbox state only. It does not read the outbox payload, call 3x-ui, or mutate subscription state.
