# Promotion Security

Promotion code input is not trusted.

- Telegram callbacks never carry raw code text, discount values, percentages, or final amounts.
- Code text is accepted only from a short-lived text-entry session.
- Codes are normalized with bounded length and strict `[A-Z0-9_-]` characters.
- Persistent lookup uses a deterministic hash through `PromotionCodeHasher`.
- Production deployments should set `PROMOTION_HASH_SECRET` for HMAC-SHA256 hashing.
- Logs and `toString()` methods must not expose raw codes or code hashes.

Invalid, disabled, missing, and unsafe codes use generic customer-facing messages to reduce enumeration value.

