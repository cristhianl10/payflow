# PayFlow MVP — Technical Decisions

**Status:** Accepted  
**Date:** 2026-09-09

## Scope

The MVP is a full-stack sandbox, runnable locally and deployed for public demonstration for registration, authentication, one USD wallet per user, synchronous internal transfers, transaction history, a double-entry ledger, and a basic dashboard. It does not process real money and must display the sandbox disclaimer.

Risk scoring, multi-currency conversion, email workflows, Redis, RabbitMQ, and advanced analytics are explicitly deferred.

## Architecture

PayFlow starts as a modular monolith. The backend uses Java 21 and Spring Boot; the frontend uses React, TypeScript, Vite, React Router, TanStack Query, React Hook Form, Zod, Tailwind CSS, and Lucide. Initial backend modules are `auth`, `user`, `wallet`, `transfer`, `transaction`, `ledger`, `shared`, and `configuration`.

Modules own their application and domain responsibilities. Controllers handle HTTP concerns, application services orchestrate use cases, domain objects enforce business rules, and infrastructure implements persistence and external adapters. No microservices are introduced before a concrete extraction need exists.

## Money and currency

The MVP supports USD only, but currency remains part of the domain model. Monetary values use a `Money` value object backed by `BigDecimal`; the domain must not depend on a `$` symbol. PostgreSQL stores amounts as an appropriate `NUMERIC` type and every wallet and transaction stores its currency (`USD`).

## Authentication and authorization

Spring Security is the authority for authentication and authorization. Access JWTs are short-lived and kept in frontend memory. Refresh tokens are sent through an HttpOnly cookie, are Secure in production, use an environment-appropriate SameSite policy, and are stored in PostgreSQL only as hashes. Refresh tokens are revocable and expire. Roles are enforced server-side; frontend route guards are UX only.

Email verification, password recovery and SMTP are deferred features; they do not block MVP login after registration. The application remains a simulated sandbox and does not process real money.

## Wallets and transactions

Each user receives one active primary wallet at registration with a configurable sandbox opening balance. A transaction represents an internal transfer in the MVP. Users may transfer to any registered user; beneficiaries are optional convenience records and never an authorization mechanism.

Opening funds are a separate balanced sandbox grant. Wallets start at zero in the schema and registration must post the grant atomically; see [ADR-001](ADR-001-wallet-locking-and-opening-funds.md).

Internal IDs are UUIDs and public identifiers are separate, non-sequential values. Completed transfers are immutable. Financial records are never hard-deleted.

## Atomicity and ledger

`POST /api/v1/transfers` completes synchronously inside one database transaction. The operation validates ownership and business invariants, locks wallets, checks the balance, creates the transaction, updates both balances, and writes exactly one `DEBIT` and one `CREDIT` ledger entry. Any failure rolls back the complete operation.

The wallet balance is a materialized read value. The immutable ledger is used for audit and reconciliation. Every completed transfer must satisfy total debits equal total credits, with database constraints complementing application checks.

## Concurrency

PostgreSQL pessimistic row locking (`SELECT ... FOR UPDATE`, exposed through an explicit JPA repository method) protects the sender balance from concurrent overspending. Wallet locks are acquired in a stable order when more than one wallet is involved, reducing deadlock risk. A Testcontainers PostgreSQL integration test must demonstrate that concurrent valid requests never produce a negative balance or unbalanced ledger.

## Idempotency

Transfers require `Idempotency-Key`. PostgreSQL persists the key, authenticated user, request hash, resulting transaction, response data/status, and expiration. A repeated key with the same request returns the original result; the same key with a different payload returns `409 Conflict`. Idempotency is enforced by a database uniqueness constraint and transaction-safe handling, not by in-memory state.

## Errors and migrations

REST APIs use `/api/v1` and a consistent error response containing timestamp, HTTP status, application code, message, path, and correlation/trace ID. Sensitive data, SQL, stack traces, and implementation details are never exposed. `@RestControllerAdvice` maps domain/application exceptions to HTTP responses.

Flyway owns schema changes. `ddl-auto` is not used as a production schema mechanism. Profiles are separated into `dev`, `test`, and `prod`; secrets and hostnames come from environment variables, with `.env.example` documenting required values.

## Testing

The testing pyramid starts with domain unit tests, application/integration tests against PostgreSQL Testcontainers, security/API tests, and frontend tests with Vitest and React Testing Library. The transfer flow, rollback, authorization, idempotency, and concurrent overspending scenarios are release-critical.

## Deferred decisions

Email verification, password recovery, SMTP, MFA, Redis, RabbitMQ, risk engine, exchange-rate provider, WebSockets, advanced analytics and multi-currency remain deferred. Public deployment is complete for the simulated sandbox through Vercel, Render and Render PostgreSQL. Real-money processing and direct bank integration are outside the project scope and would require regulated providers, private or commercial APIs, credentials and compliance controls.
