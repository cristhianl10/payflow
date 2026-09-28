# Product

<!-- impeccable:product-schema 1 -->

## Platform

Web

## Users

People exploring a simulated digital wallet, and reviewers evaluating this educational full-stack portfolio project.

## Product Purpose

Make registration, a funded sandbox wallet, internal transfers, transaction history and account security usable while demonstrating financial consistency, authorization and reproducible tests.

## Operating Context

PayFlow is available locally and as a deployed public demonstration. A user creates an account, receives fictitious USD funds, and transfers to another registered user's email. No bank account or real payment information is needed.

Production services:

- Frontend: Vercel
- Backend: Dockerized Spring Boot service on Render
- Database: Render PostgreSQL

## Current Capabilities

- Registration and login.
- Short-lived access tokens with rotating refresh cookies.
- One USD wallet per user with simulated opening funds.
- Atomic, idempotent internal transfers.
- Double-entry ledger and transaction history.
- CSV transaction export.
- Password change.
- Active-session listing and session revocation.
- Security audit events.
- CSRF, explicit CORS, role-based authorization and authentication rate limiting.

## Capabilities and Constraints

The approved scope is the master document narrowed by ADR-000 and ADR-001. PayFlow uses one USD wallet per user and every balance change has a balanced ledger origin. Access tokens stay in memory, refresh tokens use HttpOnly cookies, and refresh-token hashes are persisted in PostgreSQL.

PayFlow is a financial simulation. It does not process, store, transfer or represent real-world funds. Direct bank integration, payment processing, deposits, withdrawals, cards, KYC and AML are outside the project scope.

Deferred capabilities include email verification, password recovery, SMTP/email delivery, MFA, notifications, risk scoring, external currency rates, advanced analytics and a full administrator surface.

## Brand Commitments

PayFlow is an English-language fintech interface with light surfaces, restrained navy, semantic state colors and a persistent sandbox disclaimer. Preserve the established identity while adding functional screens.

## Evidence on Hand

The master product document, accepted architecture decisions, deployed backend operations, frontend production build and automated tests. Never invent user activity, external certifications, customer counts or banking integrations.

## Product Principles

- Financial integrity is required from the first operation.
- Confirm recipient and amount before sending.
- Preserve the request reference when an outcome is uncertain.
- Expose real stored data and useful empty states.
- Make the simulated-funds limitation visible.

## Accessibility & Inclusion

Responsive desktop and mobile web, visible keyboard focus, explicit form labels, accessible error messages and states distinguished by text as well as color.
