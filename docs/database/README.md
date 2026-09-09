# Database foundation

Flyway is the only schema owner. Never modify a migration already applied in a shared environment; add a new version instead.

V1 includes:

- users: UUID identity, unique public ID and normalized email, hashed password field, lifecycle status, UTC timestamps.
- roles and user_roles: explicit ROLE_USER / ROLE_ADMIN assignments.
- wallets: one per user, USD only, non-negative cent-precision NUMERIC(19,4) balance, lifecycle status, optimistic version field.

The migration seeds role definitions only. It creates no demo credentials or accounts. Wallets default to zero until a balanced sandbox grant is posted by registration.

Run `./mvnw verify` inside backend to apply and validate the migration against an isolated Testcontainers PostgreSQL instance and exercise uniqueness, referential integrity, currency, balance, precision, enum values, and transaction rollback.
