# Foundation implementation

## Delivered boundary

PayFlow is now deployed: the React frontend runs on Vercel, the Dockerized Spring Boot backend runs on Render, and PostgreSQL is managed by Render. The production environment remains a simulated financial sandbox; no real money is processed.

- Java 21 target, Spring Boot 3.5.16, Maven Wrapper 3.9.16.
- Flyway V1–V6 creates users, roles, wallets, ledger accounts and entries, sessions, refresh-token hashes, idempotency requests and audit logs in PostgreSQL 16.
- Framework-independent Money and account/role status enums.
- Spring Security uses bearer JWTs for authenticated API routes; no generated development user, HTTP Basic, or form login is enabled.
- Account self-service includes password change, active-session listing and session revocation, with security audit events.
- CSRF remains enabled. CORS accepts only configured explicit origins with credentials.
- API error structure and server-generated correlation IDs are shared infrastructure.
- React welcome, authentication, dashboard, wallet, transfer, history, receipt, CSV export and active-session screens, router, QueryClient, Tailwind semantic colors, linting, formatting, tests, and production build.
- GitHub Actions checks backend integration tests and frontend validation.
- Dockerfile builds the backend with Java 21. Compose currently provides PostgreSQL only.

Registration atomically creates an account, active USD wallet and balanced sandbox grant. Login issues a short-lived access JWT and an HttpOnly refresh cookie; refresh tokens are stored only as hashes, rotate on use and revoke their session on replay. Transfers lock both wallets in a stable order, post a balanced immutable journal operation, update materialized balances and store the idempotent response. There is no bypass account or unsecured financial API.

## Module ownership

- auth: authentication, token lifecycle, roles.
- user: identity and account status.
- wallet: wallet ownership, state, and materialized balances.
- transfer: synchronous transfer orchestration and idempotency.
- transaction: immutable financial operation records and history.
- ledger: balanced journal posting and reconciliation.
- shared: Money and common HTTP infrastructure; no cross-module business orchestration.
- configuration: dependency assembly and framework configuration.

Only modules with implemented code have domain/presentation subpackages. Avoid empty abstractions or framework dependencies in Money.

## Money and persistence

Money normalizes scale to the currency's minor unit, rejects negative values and implicit rounding, and refuses cross-currency arithmetic. Supporting JPY/KWD arithmetic in unit tests does not enable those currencies in the product.

The MVP database uses NUMERIC(19,4), with USD cent precision, non-negative balance, and finite-value constraints. Its maximum whole-number precision is 15 digits. Future request validation must reject excess precision before persistence; PostgreSQL NUMERIC type conversion itself may round incoming values. Monetary values must be serialized as decimal strings over HTTP, not floating-point JSON numbers.

Emails are trimmed and lowercased by future application code; the database rejects non-normalized and duplicate values. Email verification defaults false and will not block MVP authentication. Wallets are unique per user. UTC instants are stored in TIMESTAMPTZ; the database session timezone must not define business-day rules.

## Frontend contract

The approved design source is the PayFlow master document and confirmed MVP decisions: custom Tailwind UI, light surfaces, restrained navy, English copy, and responsive layout. Semantic CSS variables feed Tailwind utilities. No custom fonts or external images are fetched.

Use native links and headings, a skip link, visible keyboard focus, and layouts that stack on narrow viewports. Current CSS uses ordinary flex/grid and viewport media queries; no container queries, nesting, or experimental browser APIs are required. Initial support targets modern evergreen browsers; a formal supported-version matrix is deferred to public release. The production JavaScript target is ES2020.

The root screen is explicitly a preview of the planned experience, without a fabricated wallet or active registration action. The current page has no animation; reduced-motion rules provide the default for future additions. Queries use short-lived caching; mutation retries are disabled until each financial operation explicitly manages its idempotency key.

## Sources consulted

Verified 2026-09-09:

- [Spring Boot 3.5 requirements](https://docs.spring.io/spring-boot/3.5/system-requirements.html): Java/build compatibility for the selected Spring Boot line.
- [Apache Maven Wrapper](https://maven.apache.org/tools/wrapper/): official only-script wrapper generation.
- [Testcontainers container runtime requirements](https://java.testcontainers.org/supported_docker_environment/): Docker-compatible API and Podman configuration.
- npm registry metadata and package peer dependencies: Vite 8.2.2, React plugin 6.1.1, Vitest 5, ESLint 10; resolved versions live in package-lock.json.

## Next increment

Complete email verification and password recovery through a provider-backed email flow, then consider a minimal administrator surface, MFA, notifications and other production hardening. Real-money processing is explicitly outside the project scope.
