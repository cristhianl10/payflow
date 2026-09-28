# PayFlow

PayFlow es un sandbox financiero educativo desplegado en producción. Utiliza Java 21, Spring Boot, React, TypeScript y PostgreSQL. Simula saldos y transferencias internas; no procesa dinero real ni se conecta a bancos.

## Alcance actual

El MVP está operativo de extremo a extremo:

- Registro e inicio de sesión.
- JWT de acceso de corta duración.
- Refresh tokens rotativos almacenados como hashes.
- Cookie HttpOnly y Secure en producción.
- Una billetera USD por cuenta con saldo inicial simulado.
- Transferencias internas atómicas e idempotentes entre usuarios.
- Ledger de doble partida e historial de operaciones.
- Exportación del historial a CSV.
- Cambio de contraseña.
- Listado y revocación de sesiones activas.
- Auditoría de eventos de seguridad.
- CSRF, CORS explícito, autorización por roles y rate limiting para autenticación.
- Interfaz responsive en React.

El flujo de transferencia entre dos cuentas sandbox fue probado exitosamente. Las cuentas representan usuarios de prueba y los fondos son completamente simulados.

## Producción

- Frontend: [payflow-alpha-brown.vercel.app](https://payflow-alpha-brown.vercel.app)
- Backend: [payflow-backend-p76b.onrender.com](https://payflow-backend-p76b.onrender.com)
- Health-check: [actuator/health](https://payflow-backend-p76b.onrender.com/actuator/health)
- Base de datos: PostgreSQL administrado por Render.
- Backend: imagen Docker desplegada en Render.
- Frontend: build de React desplegado en Vercel.

El plan gratuito de Render puede suspender el backend por inactividad. La primera solicitud después de ese periodo puede tardar mientras el servicio despierta.

## Limitaciones deliberadas

PayFlow no maneja dinero real, pagos, retiros, depósitos ni transferencias bancarias. La integración con dinero real queda fuera del alcance del proyecto porque requeriría una entidad financiera o proveedor de pagos autorizado, credenciales, cumplimiento regulatorio y una API privada o comercial.

También están pendientes:

- Verificación de correo electrónico.
- Recuperación de contraseña mediante correo.
- Configuración SMTP o proveedor de email.
- Autenticación multifactor.
- Panel administrativo completo.
- Notificaciones y controles antifraude avanzados.

## Tecnologías

- Backend: Java 21, Spring Boot 3.5, Spring Security, Spring Data JPA, Hibernate y Maven.
- Base de datos: PostgreSQL 16 y Flyway.
- Frontend: React, TypeScript, Vite, React Router, TanStack Query, React Hook Form, Zod, Tailwind CSS y Vitest.
- Infraestructura: Docker, Docker Compose, Render y Vercel.
- Arquitectura: monolito modular con módulos de auth, user, wallet, transfer, transaction, ledger, shared y configuration.

## Ejecutar localmente

### Requisitos

- JDK 21.
- Node.js y npm.
- Docker con Compose o Podman.
- Maven se descarga automáticamente mediante el wrapper.

### Backend y base de datos

Desde la raíz:

```bash
cp .env.example .env
docker compose up -d postgres
```

En otra terminal:

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

Abrir http://localhost:5173. El backend local queda disponible en http://localhost:8080 y su health-check en http://localhost:8080/actuator/health.

## Verificación

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

## Configuración y seguridad

En producción se configuran mediante variables de entorno:

- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USERNAME`
- `DB_PASSWORD`
- `FRONTEND_URL`
- `JWT_SECRET`

Nunca se deben subir archivos `.env` ni secretos al repositorio. `JWT_SECRET` debe ser único y tener al menos 32 bytes.

El health-check es público y solo expone el estado básico del servicio. Las operaciones de usuario requieren autenticación. Las transferencias validan autorización, saldo, idempotencia, bloqueo de billeteras y consistencia del ledger.

## Backend Docker

```bash
docker build -t payflow-backend:local backend
```

La imagen utiliza un build multi-stage, Java 21 y un usuario no root. Docker Compose proporciona PostgreSQL para el desarrollo local.

## Estructura

```text
backend/         Spring Boot modular monolith y pruebas de integración
frontend/        React + TypeScript, Tailwind y pruebas frontend
docs/            Decisiones, arquitectura y notas de base de datos
.github/         Workflows de build y validación
docker-compose.yml
```

## Decisiones y arquitectura

- [Decisiones técnicas del MVP](docs/adr/ADR-000-mvp-technical-decisions.md)
- [Bloqueo de billeteras y fondos iniciales](docs/adr/ADR-001-wallet-locking-and-opening-funds.md)
- [Fundamentos de arquitectura](docs/architecture/foundation.md)
