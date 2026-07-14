# Telegram Renewal Completion

After Task 47, customers receive a success message only after the remote 3x-ui update is verified and local state is committed.

Failure messages say that payment was approved but the renewal needs review. They do not expose provider errors, HTTP status, outbox attempts, or internal IDs.

Status refresh maps internal states to customer-facing Persian labels such as `در حال اعمال تمدید`, `تمدید انجام شد`, and `نیازمند بررسی`.
