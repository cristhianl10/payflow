# PayFlow

An educational financial sandbox built with Java 21, Spring Boot, React, and PostgreSQL. No real money is processed by PayFlow.

## Current milestone

The foundation is executable: Maven Wrapper, PostgreSQL/Flyway schema, Money value object, deny-by-default HTTP security, health endpoint, React welcome screen, Tailwind, and automated tests.

Registration, login, wallets as API resources, ledger posting, transfers, and transaction history are the next increments. The welcome screen labels them as upcoming; there are no demo accounts yet.

- [MVP decisions](docs/adr/ADR-000-mvp-technical-decisions.md)
- [Locking and opening-funds decision](docs/adr/ADR-001-wallet-locking-and-opening-funds.md)
- [Architecture and next increment](docs/architecture/foundation.md)

## Prerequisites

- JDK 21 for backend builds (set JAVA_HOME to your JDK installation).
- Node.js 24 and npm (frontend/.nvmrc is provided).
- Docker with Compose, or Podman for the alternative workflow below.
- Maven is downloaded automatically by the wrapper on first use.

All services run locally. Internet access is needed initially to download dependencies and container images.

## Start with Docker

From the repository root:

```bash
cp .env.example .env
docker compose up -d postgres
```

In a backend terminal:

```bash
cd backend
./mvnw spring-boot:run
```

In a frontend terminal:

```bash
cd frontend
cp .env.example .env
npm ci
npm run dev
```

Open http://localhost:5173. Check the backend at http://localhost:8080/actuator/health.

The dev profile defaults match .env.example. Compose reads the root .env automatically, but Spring Boot does not. If you change database credentials, host, or port, export the matching DB_URL, DB_USERNAME, and DB_PASSWORD in the backend terminal. Vite reads frontend/.env for its optional /api development proxy.

## Podman alternative

Podman is supported through its Docker-compatible API for tests. To start a persistent local development database, copy the root .env.example to .env and run from the root:

```bash
podman run -d --name payflow-postgres-dev \
  --env-file .env \
  -p 127.0.0.1:5432:5432 \
  -v payflow-postgres-dev-data:/var/lib/postgresql/data \
  docker.io/library/postgres:16-alpine
```

For later starts use `podman start payflow-postgres-dev`. Stop it with `podman stop payflow-postgres-dev`. The named volume retains development data. Adjust the published port and DB_URL together if 5432 is occupied.

## Verify

Backend unit tests, without a container runtime:

```bash
cd backend
./mvnw test
```

Full backend verification, including isolated PostgreSQL integration tests:

```bash
cd backend
./mvnw verify
```

Tests ending in Test run with Surefire; IT tests run with Failsafe during verify. Integration tests require a working container runtime and are not silently skipped if it is missing.

For rootless Podman, run this service in a separate terminal:

```bash
podman system service --time=0 unix:///tmp/payflow-podman.sock
```

Then run in backend:

```bash
DOCKER_HOST=unix:///tmp/payflow-podman.sock \
TESTCONTAINERS_RYUK_DISABLED=true ./mvnw verify
```

The JUnit-managed PostgreSQL container is stopped on normal test completion. Ryuk is disabled only for this rootless Podman invocation; Docker/CI uses the normal cleanup mechanism. Stop the temporary Podman API with Ctrl+C when finished.

Frontend:

```bash
cd frontend
npm ci
npm run lint
npm run format:check
npm test
npm run build
```

GitHub Actions runs these checks on pushes and pull requests. The workflow has not been run remotely until the repository is pushed.

## Configuration and security

The default dev profile has local-only database defaults. The prod profile requires DB_URL, DB_USERNAME, DB_PASSWORD, and FRONTEND_URL. Keep secrets in environment variables. Never commit .env files.

GET /actuator/health is public and returns only basic status. All other routes are denied until authentication is implemented. CSRF stays enabled; CORS accepts only the configured frontend origin. No default login credentials are generated.

JWT variables in .env.example reserve the upcoming auth configuration; JWT issuance and refresh cookies are not implemented in this milestone.

## Backend image

```bash
docker build -t payflow-backend:local backend
```

The multi-stage image compiles and runs with Java 21 as a non-root user. Image creation skips tests; run verify before building. Compose currently starts PostgreSQL only; application containers and public deployment come later.

## Repository

```text
backend/         Spring Boot modular monolith and PostgreSQL integration tests
frontend/        React + TypeScript, Tailwind and frontend tests
docs/            Decisions, architecture and database notes
.github/         Build/test workflow
docker-compose.yml
```
