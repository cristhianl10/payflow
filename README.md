# PayFlow

PayFlow is an educational financial sandbox deployed for public demonstration. It uses Java 21, Spring Boot, React, TypeScript and PostgreSQL. It simulates balances and internal transfers; it does not process real money or connect to banks.

## Current scope

The MVP is operational end to end:

- Registration and login.
- Short-lived access JWTs.
- Rotating refresh tokens stored as hashes.
- HttpOnly and Secure cookies in production.
- One USD wallet per account with simulated opening funds.
- Atomic, idempotent internal transfers between users.
- Transfer references, persisted completion status and configurable per-operation/daily limits.
- Double-entry ledger and transaction history.
- Advanced transaction history with combinable search, date/amount filters, sorting and pagination.
- Filtered CSV transaction export.
- Email verification with expiring single-use tokens.
- Password recovery by email with expiring single-use reset tokens.
- Saved beneficiaries with ownership-safe CRUD and transfer shortcuts.
- Password change.
- Active-session listing and session revocation.
- Security audit events.
- CSRF, explicit CORS, role-based authorization and authentication rate limiting.
- Responsive React interface.

The transfer flow between two sandbox accounts was tested successfully. Accounts and balances are fictitious and exist only inside the PayFlow sandbox.

## Production

- Frontend: [payflow-alpha-brown.vercel.app](https://payflow-alpha-brown.vercel.app)
- Backend: [payflow-backend-p76b.onrender.com](https://payflow-backend-p76b.onrender.com)
- Health check: [actuator/health](https://payflow-backend-p76b.onrender.com/actuator/health)
- Database: PostgreSQL managed by Render.
- Backend: Docker image deployed on Render.
- Frontend: React production build deployed on Vercel.

The Render free plan may suspend the backend after inactivity. The first request after suspension can take time while the service wakes up.

## Explicit limitations

PayFlow does not handle real money, payments, withdrawals, deposits or bank transfers. Real-money integration is outside the project scope because it would require an authorized financial or payment provider, private or commercial APIs, credentials, regulatory compliance, KYC/AML controls and legal agreements.

Deferred capabilities include:

- Multifactor authentication.
- Complete administrator surface.
- Notifications and advanced antifraud controls.
- Risk scoring and multi-currency support.

## Technologies

- Backend: Java 21, Spring Boot 3.5, Spring Security, Spring Data JPA, Hibernate and Maven.
- Database: PostgreSQL 16 and Flyway.
- Frontend: React, TypeScript, Vite, React Router, TanStack Query, React Hook Form, Zod, Tailwind CSS and Vitest.
- Infrastructure: Docker, Docker Compose, Render and Vercel.
- Architecture: modular monolith with auth, user, wallet, transfer, transaction, ledger, shared and configuration modules.

## Run locally

### Requirements

- JDK 21.
- Node.js and npm.
- Docker with Compose or Podman.
- Maven is downloaded automatically through the wrapper.

### Backend and database

From the repository root:

```bash
cp .env.example .env
docker compose up -d postgres
```

In another terminal:

```bash
cd backend
./mvnw spring-boot:run
```

### Frontend

```bash
cd frontend
cp .env.example .env
npm ci
npm run dev
```

Open http://localhost:5173. The local backend is available at http://localhost:8080 and its health check at http://localhost:8080/actuator/health.

## Verification

Backend:

```bash
cd backend
./mvnw test
./mvnw verify
```

Frontend:

```bash
cd frontend
npm ci
npm run lint
npm run format:check
npm test
npm run build
```

## Configuration and security

Production variables are supplied through the hosting platform:

- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USERNAME`
- `DB_PASSWORD`
- `FRONTEND_URL`
- `JWT_SECRET`
- `TRANSFER_MAX_PER_OPERATION`
- `TRANSFER_DAILY_LIMIT`
- `EMAIL_DELIVERY_MODE` (`log` or `smtp`)
- `EMAIL_FROM`
- `SMTP_HOST`
- `SMTP_PORT`
- `SMTP_USERNAME`
- `SMTP_PASSWORD`
- `SMTP_AUTH`
- `SMTP_STARTTLS`

Never commit `.env` files or secrets. `JWT_SECRET` must be unique and contain at least 32 bytes. Email verification uses `log` delivery by default for local/testing environments; configure `EMAIL_DELIVERY_MODE=smtp` and the SMTP variables to send real verification emails.

The health check is public and exposes only basic service status. All user operations require authentication. Transfers validate authorization, balance, configurable limits, idempotency, wallet locking and ledger consistency.

## Backend Docker image

```bash
docker build -t payflow-backend:local backend
```

The image uses a multi-stage build, Java 21 and a non-root user. Docker Compose provides PostgreSQL for local development.

## Repository structure

```text
backend/         Spring Boot modular monolith and integration tests
frontend/        React + TypeScript, Tailwind and frontend tests
docs/            Decisions, architecture and database notes
.github/         Build and validation workflows
docker-compose.yml
```

## Decisions and architecture

- [MVP technical decisions](docs/adr/ADR-000-mvp-technical-decisions.md)
- [Wallet locking and opening funds](docs/adr/ADR-001-wallet-locking-and-opening-funds.md)
- [Architecture foundation](docs/architecture/foundation.md)
- [Master software engineering document](PayFlow%20%E2%80%94%20Master%20Software%20Engineering%20Document.md)
