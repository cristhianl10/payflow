# PayFlow

## Master Software Engineering Document

**Project type:** Full-stack financial sandbox  
**Purpose:** Academic project and professional portfolio case study  
**Nature:** Simulated financial environment; no real money is processed  
**Backend:** Java 21 and Spring Boot  
**Frontend:** React and TypeScript  
**Database:** PostgreSQL  
**Architecture:** Modular monolith with clean and hexagonal architecture principles  
**Deployment:** Vercel frontend, Dockerized Render backend and Render PostgreSQL

---

## 1. Project Context

PayFlow is a simulated digital wallet platform. Registered users receive fictitious USD funds and can transfer simulated funds to other PayFlow users, inspect their transaction history and manage account security.

PayFlow is not a bank, payment processor or financial institution. It must never:

- process real money;
- connect directly to real bank accounts;
- store real card or banking information;
- support real deposits or withdrawals;
- execute bank transfers;
- represent itself as a regulated financial institution.

Every balance and transaction belongs exclusively to the PayFlow sandbox.

The interface must always communicate this limitation through messages such as:

> Sandbox environment  
> No real money is processed by PayFlow.

---

## 2. Current Product Scope

The deployed MVP includes:

- user registration and login;
- short-lived access JWTs;
- rotating refresh tokens stored only as hashes;
- secure HttpOnly refresh cookies;
- one USD wallet per user;
- simulated opening funds;
- atomic, idempotent internal transfers;
- double-entry ledger postings;
- immutable transaction history;
- CSV transaction export;
- password change;
- active-session listing;
- individual session revocation;
- security audit events;
- CSRF protection;
- explicit CORS configuration;
- server-side role authorization;
- authentication rate limiting;
- responsive React interface.

The transfer flow between two sandbox accounts has been tested successfully.

---

## 3. Production Topology

```text
React frontend
      |
      | HTTPS
      v
Vercel
      |
      | HTTPS REST API
      v
Dockerized Spring Boot backend
      |
      v
Render PostgreSQL
```

Production URLs:

- Frontend: https://payflow-alpha-brown.vercel.app
- Backend: https://payflow-backend-p76b.onrender.com
- Health check: https://payflow-backend-p76b.onrender.com/actuator/health

The Render free plan may suspend the backend after inactivity. The first request after suspension can take time while the service wakes up.

---

## 4. Architecture

PayFlow is a modular monolith. The current modules are:

- `auth`: registration, login, token lifecycle and roles;
- `user`: identity and account status;
- `wallet`: wallet ownership, status and balances;
- `transfer`: transfer orchestration and idempotency;
- `transaction`: immutable operation records and history;
- `ledger`: balanced journal postings;
- `shared`: money value object and common HTTP infrastructure;
- `configuration`: dependency assembly and framework configuration.

Controllers handle HTTP concerns. Application services orchestrate use cases. Domain objects enforce business invariants. Repositories and infrastructure implement persistence and external adapters.

No microservices are required for the current scope. A modular monolith keeps deployment, testing and reasoning simple while preserving boundaries for future extraction.

---

## 5. Financial Consistency

A transfer is executed synchronously inside one database transaction.

The operation:

1. authenticates the caller;
2. resolves the source and destination wallets;
3. acquires wallet locks in a stable order;
4. validates wallet state, currency and available balance;
5. checks idempotency;
6. creates the transaction;
7. updates materialized balances;
8. writes one debit and one credit ledger entry;
9. stores the successful idempotent response;
10. commits the complete operation.

Any failure rolls back the complete operation.

Transfers must satisfy:

```text
total debits = total credits
```

Money uses `BigDecimal` and PostgreSQL `NUMERIC`. Floating-point types must never represent monetary values.

---

## 6. Authentication and Security

Spring Security is the authority for authentication and authorization.

Current security decisions:

- passwords are stored as secure hashes;
- access tokens are short-lived;
- refresh tokens rotate on use;
- refresh-token hashes are stored in PostgreSQL;
- refresh-token replay revokes the related session;
- production refresh cookies are HttpOnly and Secure;
- CSRF remains enabled;
- CORS accepts only explicit configured origins;
- roles are enforced server-side;
- frontend route guards are UX only;
- login and registration requests are rate-limited;
- important security events are audited;
- users can change their password;
- users can inspect and revoke active sessions.

Email verification, password recovery, SMTP delivery and MFA are deferred features. MVP login is not blocked by email verification.

---

## 7. Implemented API Capabilities

The API is versioned under `/api/v1`.

Implemented areas include:

- authentication and token refresh;
- current user profile;
- password change;
- active sessions and session revocation;
- wallet and balance queries;
- atomic internal transfers;
- transaction history;
- CSV export;
- health check;
- audit logging.

All protected operations require authentication. Sensitive data, SQL, stack traces and implementation details must not be exposed in API errors.

---

## 8. Database and Persistence

Flyway owns schema migrations. Hibernate validates the production schema; it does not create production tables.

The PostgreSQL schema contains entities for:

- users;
- roles;
- wallets;
- ledger accounts;
- ledger entries;
- sessions;
- refresh-token hashes;
- idempotency requests;
- transactions;
- audit logs.

UTC instants are stored as `TIMESTAMPTZ`. Wallets are unique per user. Completed financial records are immutable and must never be hard-deleted.

---

## 9. Frontend

The frontend uses React, TypeScript, Vite, React Router, TanStack Query, React Hook Form, Zod, Tailwind CSS and Vitest.

The interface provides:

- welcome screen;
- registration and login;
- dashboard;
- wallet balance;
- transfer form;
- transaction history;
- receipt/details;
- CSV export;
- account settings;
- password change;
- active-session management.

The interface is English-language, responsive and includes visible sandbox disclaimers.

---

## 10. Testing and Verification

Backend verification:

```bash
cd backend
./mvnw test
./mvnw verify
```

Frontend verification:

```bash
cd frontend
npm ci
npm run lint
npm run format:check
npm test
npm run build
```

Release-critical scenarios include:

- authorization;
- transfer rollback;
- idempotency;
- concurrent transfers;
- prevention of negative balances;
- balanced ledger entries;
- refresh-token rotation;
- session revocation;
- CSV export.

---

## 11. Local Development

Requirements:

- JDK 21;
- Node.js and npm;
- Docker Compose or Podman.

Start PostgreSQL:

```bash
cp .env.example .env
docker compose up -d postgres
```

Start the backend:

```bash
cd backend
./mvnw spring-boot:run
```

Start the frontend:

```bash
cd frontend
cp .env.example .env
npm ci
npm run dev
```

Open http://localhost:5173. The local backend health check is available at http://localhost:8080/actuator/health.

---

## 12. Environment Configuration

Production secrets must be supplied through the hosting platform:

```text
DB_HOST
DB_PORT
DB_NAME
DB_USERNAME
DB_PASSWORD
FRONTEND_URL
JWT_SECRET
```

Never commit `.env` files or production secrets. `JWT_SECRET` must be unique and contain at least 32 bytes.

---

## 13. Explicitly Out of Scope

The project does not implement:

- real-money processing;
- bank-account integration;
- ACH or SWIFT;
- real cards;
- real deposits or withdrawals;
- real payment processing;
- real KYC or AML;
- real identity documents;
- biometrics;
- cryptocurrency;
- loans or investments.

A real-money version would require an authorized financial or payment provider, private or commercial APIs, credentials, regulatory compliance, KYC/AML controls, monitoring and legal agreements. That is intentionally not part of PayFlow.

---

## 14. Deferred Features

The next possible increments are:

- email verification;
- password recovery;
- SMTP or transactional email provider;
- multifactor authentication;
- notifications;
- risk scoring and risk alerts;
- beneficiaries;
- advanced analytics;
- administrator dashboard;
- multi-currency support;
- reconciliation tooling;
- scheduled transfers;
- stronger production observability.

These features must be implemented incrementally after the current MVP remains stable.

---

## 15. Engineering Priorities

When priorities conflict, use this order:

1. correctness;
2. security;
3. data integrity;
4. maintainability;
5. testing;
6. performance;
7. visual polish;
8. additional features.

The project should demonstrate why each technical decision was made, what alternatives existed and how the implementation was verified.

PayFlow should remain a serious software-engineering case study even though all funds are fictitious.
