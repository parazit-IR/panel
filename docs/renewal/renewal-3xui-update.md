# Renewal 3x-ui Update

Renewal updates the existing remote client only. It preserves client UUID, email, inbound, protocol, subscription token, and config identity.

No client creation, migration, or recreation is part of Task 47.

The worker reads the existing remote client, verifies identity against `XuiClientProvision`, sends absolute expiry and traffic values, then verifies the final remote state before local data is updated.
