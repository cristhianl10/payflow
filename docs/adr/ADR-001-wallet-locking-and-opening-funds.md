# ADR-001 — Wallet locking and opening funds

Status: Accepted  
Date: 2026-09-09

## Context

Concurrent transfers must not spend the same balance twice. Transfers in opposite directions must not acquire wallet locks in opposite orders. Registration also changes balances: granting the configured opening funds without ledger entries would prevent reconciliation from the start.

## Decision

Implement synchronous transfers with PostgreSQL pessimistic row locks. The transfer use case must acquire **both wallets in ascending UUID order** using an explicit JPA query equivalent to `SELECT ... FOR UPDATE`, recheck ownership, account/wallet status, currency, and available balance under the locks, then commit balances, transaction, ledger entries, and the successful idempotency result together.

A wallet starts at zero in the schema. Registration must post a separate `SANDBOX_GRANT` journal operation: debit a system issuance ledger account and credit the user's wallet ledger account. The issuance account is not a user wallet and is outside the user-wallet non-negative balance constraint. Updating the materialized balance and posting the grant must be atomic with registration. This is a prerequisite for releasing registration, not a later reconciliation feature.

For internal transfers, debit and credit must balance within USD. No API should allow directly editing a wallet balance.

## Alternatives and consequences

- Optimistic locking plus retries reduces waiting when contention is rare, but introduces additional retry/idempotency coordination. Keep the version field for future evolution; use pessimistic locking for the first transfer implementation.
- Locking only the sender does not by itself establish safe ordering for concurrent receiver updates. Locking both wallets simplifies the invariant and lock ordering.
- Setting a nonzero default wallet balance is simpler but leaves money without a ledger origin. Use a zero default and a balanced grant.
- Balance reconstruction on every read adds query cost. Keep the materialized wallet balance and test reconciliation against ledger entries.

## Verification gates

Before implementing transfers, add PostgreSQL integration tests for simultaneous overspending, opposing transfers, lost HTTP responses/retries, duplicate idempotency requests, changed payload conflicts, and full rollback after an injected failure. Registration must have a grant/reconciliation test.

The foundation currently tests money arithmetic, SQL constraints, HTTP security, and database rollback. This ADR records the transfer/grant design; the locking query and financial ledger are not implemented yet.
