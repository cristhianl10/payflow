# PAYFLOW

## Documento Maestro de Análisis, Diseño y Construcción de Software

**Tipo de proyecto:** Plataforma fintech sandbox Full-Stack  
**Propósito:** Proyecto académico y de portafolio profesional  
**Naturaleza:** Simulación financiera. No procesa dinero real.  
**Backend principal:** Java 21 + Spring Boot  
**Frontend:** React + TypeScript  
**Base de datos:** PostgreSQL  
**Arquitectura inicial:** Monolito modular con principios de Clean Architecture / Hexagonal Architecture  
**Objetivo técnico:** Construir un proyecto empresarial suficientemente sólido para demostrar conocimientos de backend, arquitectura de software, bases de datos, seguridad, testing, APIs, frontend, DevOps y diseño de sistemas.

---

# 1. CONTEXTO GENERAL DEL PROYECTO

PayFlow es una plataforma financiera simulada que permite a usuarios registrados administrar una billetera digital ficticia, realizar transferencias internas entre usuarios, consultar su historial de movimientos, administrar beneficiarios, visualizar estadísticas financieras y operar con diferentes monedas mediante información obtenida desde APIs externas.

PayFlow NO es una plataforma bancaria real.

PayFlow NO debe:

- procesar dinero real;
- conectarse directamente a cuentas bancarias;
- almacenar tarjetas de crédito;
- almacenar información bancaria real;
- permitir retiros reales;
- realizar transferencias bancarias;
- presentarse como institución financiera.

Todo saldo almacenado y procesado por PayFlow representa únicamente dinero ficticio utilizado dentro de un entorno sandbox.

La interfaz debe comunicar claramente esta condición mediante indicadores como:

**Sandbox Environment**

y:

**No real money is processed by PayFlow.**

El proyecto debe sentirse técnicamente similar a una plataforma financiera moderna, pero sin implicaciones regulatorias relacionadas con operaciones monetarias reales.

---

# 2. PROPÓSITO DEL PROYECTO

El proyecto tiene dos propósitos simultáneos.

## 2.1 Propósito funcional

Crear una aplicación web moderna donde una persona pueda experimentar las operaciones principales de una billetera digital:

- registro;
- autenticación;
- administración de perfil;
- billetera;
- consulta de saldo;
- transferencias;
- beneficiarios;
- movimientos;
- estadísticas;
- conversión entre monedas;
- notificaciones;
- alertas;
- administración.

## 2.2 Propósito académico y profesional

El proyecto debe demostrar conocimientos reales de ingeniería de software.

Debe ser suficientemente profundo como para permitir explicar durante una entrevista:

- diseño de APIs REST;
- Spring Boot;
- Spring Security;
- JWT;
- PostgreSQL;
- JPA/Hibernate;
- transacciones ACID;
- concurrencia;
- locking;
- idempotencia;
- arquitectura modular;
- Clean Architecture;
- Domain-Driven Design ligero;
- patrones de diseño;
- auditoría;
- motores de reglas;
- seguridad;
- integración con APIs externas;
- procesamiento asíncrono;
- caché;
- testing unitario;
- testing de integración;
- Testcontainers;
- Docker;
- CI/CD;
- observabilidad;
- diseño frontend;
- integración Full-Stack.

El objetivo NO es crear simplemente un CRUD.

---

# 3. VISIÓN DEL PRODUCTO

PayFlow debe sentirse como un producto tecnológico real.

El usuario debería poder registrarse y disponer inmediatamente de una cuenta sandbox con saldo ficticio.

Ejemplo:

```text
Welcome to PayFlow

Sandbox balance

$10,000.00 USD
```

Desde allí podrá realizar transferencias a otros usuarios registrados.

Ejemplo:

```text
Send money

Recipient
jeremy@example.com

Amount
$250.00

Description
Dinner payment
```

Después de confirmar:

```text
Transfer completed

$250.00

To
Jeremy Márquez

Transaction
TX-PF-20260909-A82C91
```

Todas estas operaciones ocurren exclusivamente dentro de la base de datos de PayFlow.

---

# 4. IDENTIDAD DEL PRODUCTO

## Nombre

**PayFlow**

Nombre secundario opcional:

**PayFlow Sandbox**

## Posicionamiento

Simulated digital payments and financial operations platform.

## Personalidad visual

La interfaz debe transmitir:

- confianza;
- estabilidad;
- seguridad;
- modernidad;
- tecnología;
- claridad;
- precisión.

Debe evitar parecer:

- una aplicación de criptomonedas;
- un casino;
- un proyecto estudiantil básico;
- un dashboard genérico;
- una copia exacta de PayPal;
- una copia exacta de Revolut;
- una copia exacta de cualquier banco.

---

# 5. ACTORES DEL SISTEMA

## 5.1 Usuario no autenticado

Puede:

- visitar landing page;
- consultar características;
- registrarse;
- iniciar sesión;
- recuperar contraseña;
- verificar email.

No puede acceder a información financiera.

## 5.2 Usuario autenticado

Rol:

```text
ROLE_USER
```

Puede:

- consultar wallet;
- consultar saldo;
- realizar transferencias;
- consultar movimientos;
- administrar beneficiarios;
- consultar estadísticas;
- actualizar ciertos datos de perfil;
- administrar sesiones/dispositivos;
- consultar notificaciones.

## 5.3 Administrador

Rol:

```text
ROLE_ADMIN
```

Puede:

- consultar usuarios;
- bloquear usuarios;
- activar usuarios;
- consultar transferencias;
- consultar alertas de riesgo;
- consultar métricas generales;
- consultar eventos de auditoría;
- investigar operaciones;
- administrar determinadas configuraciones del sandbox.

Nunca debe poder conocer contraseñas.

---

# 6. ALCANCE

## 6.1 Incluido

El alcance inicial comprende:

### Usuarios

Registro, autenticación, autorización y administración del perfil.

### Wallets

Creación automática de una wallet principal.

### Transferencias internas

Transferencias entre usuarios PayFlow.

### Ledger

Registro contable de todos los movimientos.

### Transacciones

Historial detallado y estados.

### Beneficiarios

Administración de destinatarios frecuentes.

### Risk Engine

Motor configurable de evaluación de riesgo.

### Administración

Dashboard administrativo.

### Conversión de moneda

Consulta de tipos de cambio mediante API externa.

### Notificaciones

Notificaciones internas y posteriormente email.

### Auditoría

Registro de eventos importantes.

### Seguridad

JWT, refresh tokens, roles y mecanismos complementarios.

### Analytics

Estadísticas básicas para usuarios y administradores.

---

# 7. FUERA DEL ALCANCE

No implementar:

- dinero real;
- integración bancaria;
- ACH;
- SWIFT;
- tarjetas reales;
- PCI DSS real;
- depósitos reales;
- retiros;
- criptomonedas;
- inversiones;
- préstamos;
- apuestas;
- comercio de activos;
- procesamiento real de pagos;
- KYC real;
- AML legal;
- documentos de identidad reales;
- biometría real.

Se pueden simular conceptos relacionados únicamente cuando resulten útiles para demostrar arquitectura.

---

# 8. REQUISITOS FUNCIONALES

# RF-001 Registro de usuarios

El sistema debe permitir registrar un usuario mediante:

- nombre;
- apellido;
- email;
- contraseña.

El email debe ser único.

La contraseña nunca debe almacenarse en texto plano.

El backend debe almacenar únicamente su hash seguro.

Después del registro:

1. se crea el usuario;
2. se crea su wallet principal;
3. se asigna la moneda principal;
4. se asigna saldo ficticio inicial;
5. se asigna ROLE_USER;
6. se registra el evento en auditoría.

Saldo sandbox recomendado:

```text
$10,000 USD
```

Debe ser configurable mediante variables de entorno o configuración del sistema.

---

# RF-002 Inicio de sesión

El usuario inicia sesión mediante:

```text
email
password
```

El backend devuelve:

```text
accessToken
refreshToken
user
```

No almacenar tokens sensibles de forma insegura en frontend.

La implementación deberá priorizar cookies HttpOnly/Secure para refresh tokens cuando corresponda.

---

# RF-003 Refresh token

El sistema debe permitir solicitar un nuevo access token utilizando un refresh token válido.

Los refresh tokens deben:

- poder revocarse;
- tener expiración;
- asociarse al usuario;
- opcionalmente asociarse a dispositivo/sesión.

---

# RF-004 Logout

Cerrar sesión debe invalidar o revocar el refresh token correspondiente.

---

# RF-005 Wallet

Cada usuario deberá disponer inicialmente de una wallet principal.

La wallet debe contener como mínimo:

```text
id
publicId
userId
currency
availableBalance
status
createdAt
updatedAt
version
```

Status:

```text
ACTIVE
BLOCKED
CLOSED
```

---

# RF-006 Consulta de saldo

El usuario autenticado puede consultar:

```text
availableBalance
currency
walletStatus
```

Nunca debe poder consultar wallets pertenecientes a otros usuarios directamente mediante manipulación de IDs.

---

# RF-007 Transferencias

El usuario puede transferir saldo ficticio a otro usuario PayFlow.

El destinatario puede identificarse por:

- email;
- username futuro;
- wallet publicId.

La transferencia debe comprobar:

1. autenticación;
2. wallet de origen;
3. estado de wallet;
4. destinatario existente;
5. imposibilidad de transferirse a sí mismo;
6. monto superior a cero;
7. moneda;
8. saldo suficiente;
9. reglas de riesgo;
10. idempotencia;
11. concurrencia;
12. persistencia atómica.

---

# RF-008 Transferencia atómica

Una transferencia debe comportarse como una única operación.

Si cualquiera de sus pasos falla:

```text
ROLLBACK
```

No debe existir ningún estado parcial.

La operación deberá ejecutarse mediante una transacción de base de datos gestionada por Spring.

---

# RF-009 Identificador público de transacción

Toda transferencia debe recibir un identificador público no secuencial.

Ejemplo:

```text
PF-TX-20260909-82A19F
```

No exponer IDs internos incrementales como identificadores principales al usuario.

---

# RF-010 Estados de transferencia

Estados sugeridos:

```text
PENDING
PROCESSING
COMPLETED
REJECTED
BLOCKED
FAILED
CANCELLED
```

No todos deben utilizarse inicialmente.

---

# RF-011 Historial

El usuario debe consultar sus movimientos.

Debe soportar:

- paginación;
- ordenamiento;
- búsqueda;
- filtros.

Filtros:

```text
dateFrom
dateTo
type
status
minAmount
maxAmount
recipient
currency
```

---

# RF-012 Detalle de transacción

El usuario debe consultar:

- ID público;
- fecha;
- hora;
- cantidad;
- moneda;
- origen;
- destino;
- descripción;
- estado;
- tipo;
- conversión utilizada si aplica.

No exponer información privada innecesaria del otro usuario.

---

# RF-013 Beneficiarios

El usuario puede:

- agregar beneficiario;
- consultar beneficiarios;
- editar alias;
- eliminar beneficiario.

Entidad conceptual:

```text
Beneficiary
```

Campos:

```text
id
ownerUserId
beneficiaryUserId
alias
createdAt
```

---

# RF-014 Ledger

Toda operación que modifique balances debe producir entradas en el ledger.

Una transferencia de 100 USD debe producir como mínimo:

```text
DEBIT  sender   100
CREDIT receiver 100
```

Debe cumplirse:

```text
total debit = total credit
```

El ledger debe ser inmutable desde operaciones normales.

No implementar:

```text
UPDATE ledger_entry
```

para corregir movimientos históricos.

Las correcciones deberán representarse con entradas compensatorias/reversas cuando esta funcionalidad sea agregada.

---

# RF-015 Risk Engine

Antes de completar una transferencia, el sistema debe calcular un nivel de riesgo.

Ejemplo de reglas:

```text
LargeAmountRule
NewBeneficiaryRule
TransactionVelocityRule
DailyAmountRule
NewDeviceRule
```

Cada regla retorna una puntuación.

Ejemplo:

```text
Large amount              +30
New beneficiary           +15
New device                +15
Velocity anomaly          +30

Risk Score                90
```

---

# RF-016 Clasificación de riesgo

Ejemplo:

```text
0-29    LOW
30-59   MEDIUM
60-79   HIGH
80-100  CRITICAL
```

La configuración debería poder modificarse posteriormente.

---

# RF-017 Decisión de riesgo

Ejemplo:

```text
LOW       APPROVE
MEDIUM    APPROVE
HIGH      REVIEW / BLOCK depending rule
CRITICAL  BLOCK
```

Para las primeras versiones se permite:

```text
score >= 80 → BLOCK
```

---

# RF-018 Risk Alert

Una transferencia bloqueada deberá producir una alerta.

Debe almacenar:

```text
transactionId
userId
score
riskLevel
reasons
createdAt
status
```

Estados posibles:

```text
OPEN
UNDER_REVIEW
RESOLVED
DISMISSED
```

---

# RF-019 Dashboard del usuario

Debe mostrar al menos:

- saldo;
- ingresos simulados;
- egresos simulados;
- movimientos recientes;
- actividad mensual;
- accesos rápidos;
- principales beneficiarios.

---

# RF-020 Analytics

El usuario debe consultar estadísticas.

Ejemplos:

```text
Money sent this month
Money received this month
Transactions this month
Average transfer
Most frequent beneficiary
```

---

# RF-021 API de monedas

PayFlow debe consumir una API externa de tipos de cambio.

Crear una interfaz interna:

```java
ExchangeRateProvider
```

Esto permitirá reemplazar proveedores externos sin afectar el dominio.

No acoplar lógica del negocio directamente al SDK/API de un proveedor concreto.

---

# RF-022 Conversión

Para una transferencia con conversión:

```text
Source amount
Exchange rate
Destination amount
Rate timestamp
```

deben almacenarse dentro de la transacción.

Nunca recalcular posteriormente una transferencia histórica usando el tipo de cambio actual.

---

# RF-023 Caché de tipos de cambio

Las tasas externas pueden almacenarse temporalmente usando Redis.

Ejemplo:

```text
USD:EUR → TTL 5 min
```

La aplicación debe seguir políticas razonables frente a indisponibilidad de API.

---

# RF-024 Notificaciones

Eventos:

```text
TRANSFER_RECEIVED
TRANSFER_SENT
TRANSFER_BLOCKED
LOGIN_NEW_DEVICE
PASSWORD_CHANGED
```

Inicialmente podrán mostrarse dentro de PayFlow.

Posteriormente:

- email;
- WebSocket opcional.

---

# RF-025 Auditoría

Registrar eventos relevantes.

Ejemplos:

```text
USER_REGISTERED
LOGIN_SUCCESS
LOGIN_FAILED
PASSWORD_CHANGED
TRANSFER_CREATED
TRANSFER_COMPLETED
TRANSFER_BLOCKED
USER_BLOCKED
```

Campos:

```text
event
actorUserId
entityType
entityId
ipAddress
userAgent
timestamp
metadata
```

Evitar guardar información sensible innecesaria.

---

# RF-026 Panel administrativo

Debe incluir:

```text
Users
Transactions
Risk Alerts
Audit Logs
System Analytics
```

---

# RF-027 Administración de usuarios

ADMIN puede:

```text
search
filter
view
block
unblock
```

Bloquear un usuario debe impedir nuevas operaciones financieras.

---

# RF-028 Dashboard administrativo

Métricas sugeridas:

```text
Total users
Active users
Transfers today
Volume today
Blocked transfers
Risk alerts
Average transfer
```

---

# RF-029 Seed/demo data

La aplicación debe permitir generar datos ficticios para demostraciones.

Nunca mezclar seed de desarrollo con producción.

Usar perfiles:

```text
dev
test
prod
```

---

# 9. REQUISITOS NO FUNCIONALES

# RNF-001 Seguridad

Contraseñas mediante:

```text
BCrypt o Argon2
```

Nunca:

```text
MD5
SHA1
plain text
```

---

# RNF-002 Rendimiento

Objetivo inicial:

```text
p95 API response < 500ms
```

exceptuando dependencias externas.

---

# RNF-003 Consistencia

Operaciones monetarias ficticias deben mantener consistencia incluso ante solicitudes concurrentes.

---

# RNF-004 Disponibilidad

La aplicación debe manejar fallos externos sin destruir operaciones internas.

---

# RNF-005 Escalabilidad

La arquitectura debe permitir extraer determinados módulos posteriormente sin requerir reescribir todo el sistema.

---

# RNF-006 Mantenibilidad

Evitar:

```text
controllers gigantes
services de 1000 líneas
entidades expuestas directamente
repositorios con lógica de negocio
código duplicado
```

---

# RNF-007 Testabilidad

El dominio no debe depender innecesariamente de frameworks externos.

---

# RNF-008 Observabilidad

Proporcionar:

```text
health
metrics
logs
```

Spring Boot Actuator será utilizado.

---

# RNF-009 Accesibilidad

Frontend compatible al menos conceptualmente con WCAG 2.1 AA.

Debe incluir:

- contraste suficiente;
- navegación por teclado;
- labels;
- estados focus;
- mensajes comprensibles;
- no depender únicamente del color.

---

# RNF-010 Responsive

Debe funcionar correctamente en:

```text
desktop
tablet
mobile
```

Diseñar inicialmente desktop-first para dashboards, pero garantizar adaptación móvil.

---

# 10. REGLAS DE NEGOCIO

## RB-001

Un usuario posee inicialmente una wallet principal.

## RB-002

El email es único.

## RB-003

Un usuario no puede transferirse dinero a sí mismo.

## RB-004

Monto:

```text
amount > 0
```

## RB-005

Una wallet bloqueada no puede enviar dinero.

## RB-006

Una wallet debe poseer saldo suficiente.

## RB-007

No debe permitirse saldo negativo.

Debe existir también protección a nivel de base de datos.

Ejemplo conceptual:

```sql
CHECK (available_balance >= 0)
```

## RB-008

Una transferencia completada no puede eliminarse.

## RB-009

Las entradas del ledger son inmutables.

## RB-010

Una transferencia bloqueada por Risk Engine no modifica balances.

## RB-011

Todas las operaciones financieras deben ser idempotentes cuando corresponda.

## RB-012

Un mismo `Idempotency-Key` no debe procesar dos veces una transferencia.

## RB-013

Una transacción debe almacenar el tipo de cambio utilizado al momento de ejecutarse.

## RB-014

No utilizar `double` o `float` para dinero.

Java:

```java
BigDecimal
```

PostgreSQL:

```text
NUMERIC(19,4)
```

o precisión debidamente elegida.

## RB-015

Las fechas deberán almacenarse en UTC.

Frontend convierte a timezone correspondiente.

---

# 11. ARQUITECTURA GENERAL

Inicialmente utilizar:

```text
React
   |
 HTTPS
   |
Spring Boot API
   |
PostgreSQL
```

Servicios complementarios:

```text
Redis
RabbitMQ
Exchange Rate API
Email Provider
```

Diagrama conceptual:

```text
                 ┌─────────────────────────┐
                 │     React Frontend      │
                 │      TypeScript         │
                 └────────────┬────────────┘
                              │
                           HTTPS/REST
                              │
                 ┌────────────▼────────────┐
                 │      Spring Boot        │
                 │                         │
                 │ Auth                    │
                 │ Users                   │
                 │ Wallets                 │
                 │ Transfers               │
                 │ Ledger                  │
                 │ Risk                    │
                 │ Notifications           │
                 │ Analytics               │
                 │ Administration          │
                 └───────┬─────────┬───────┘
                         │         │
                    PostgreSQL    Redis
                         │
                         │
                  ┌──────▼──────┐
                  │ RabbitMQ    │
                  └─────────────┘

External:
Exchange Rate Provider
Email Provider
```

---

# 12. NO EMPEZAR CON MICROSERVICIOS

PayFlow debe empezar como un:

**Modular Monolith**

Motivos:

- menor complejidad operativa;
- desarrollo más rápido;
- transacciones más simples;
- testing más sencillo;
- despliegue simple;
- arquitectura igualmente demostrable.

NO crear microservicios únicamente para parecer más avanzado.

Solo extraer componentes cuando exista una justificación arquitectónica.

Ejemplos futuros razonables:

```text
Notification Service
Fraud Service
Analytics Service
```

---

# 13. ESTRUCTURA MODULAR

Backend conceptual:

```text
com.payflow

├── auth
├── user
├── wallet
├── transfer
├── transaction
├── ledger
├── beneficiary
├── risk
├── notification
├── exchange
├── audit
├── analytics
├── admin
├── shared
└── configuration
```

Cada módulo deberá controlar claramente su responsabilidad.

---

# 14. CLEAN / HEXAGONAL ARCHITECTURE

Dentro de módulos importantes utilizar:

```text
domain
application
infrastructure
presentation
```

Ejemplo:

```text
transfer/

├── domain/
│   ├── model/
│   ├── service/
│   ├── repository/
│   └── event/
│
├── application/
│   ├── usecase/
│   ├── command/
│   ├── query/
│   └── dto/
│
├── infrastructure/
│   ├── persistence/
│   ├── messaging/
│   └── external/
│
└── presentation/
    └── rest/
```

No exagerar arquitectura para operaciones triviales.

---

# 15. PRINCIPIO FUNDAMENTAL

Controllers:

```text
HTTP concerns
```

Application services:

```text
orchestration
```

Domain:

```text
business rules
```

Repositories:

```text
persistence abstraction
```

Infrastructure:

```text
technical implementation
```

---

# 16. PATRONES DE DISEÑO

Aplicarlos únicamente con justificación.

## Repository Pattern

Persistencia.

```java
WalletRepository
TransactionRepository
```

## Strategy Pattern

Reglas de riesgo.

```java
RiskRule
```

Implementaciones:

```text
LargeAmountRiskRule
VelocityRiskRule
NewBeneficiaryRiskRule
```

## Factory

Cuando crear objetos complejos del dominio requiera invariantes.

```java
TransactionFactory
```

## Observer / Events

Eventos del dominio.

```text
TransferCompletedEvent
```

## Adapter

Integraciones externas.

```java
ExchangeRateProvider
```

Implementación:

```java
ExternalExchangeRateApiAdapter
```

## Specification

Filtros o reglas de negocio combinables cuando resulte conveniente.

---

# 17. MODELO DE DOMINIO

Entidades principales:

```text
User
Wallet
Transaction
LedgerEntry
Beneficiary
RiskAssessment
RiskAlert
Notification
RefreshToken
AuditLog
DeviceSession
ExchangeRateSnapshot
```

---

# 18. MODELO DE BASE DE DATOS

## users

```text
id UUID PK
public_id VARCHAR UNIQUE
first_name
last_name
email UNIQUE
password_hash
status
email_verified
created_at
updated_at
```

Status:

```text
ACTIVE
BLOCKED
SUSPENDED
CLOSED
```

---

## roles

```text
id
name
```

Ejemplo:

```text
ROLE_USER
ROLE_ADMIN
```

---

## user_roles

```text
user_id
role_id
```

---

## wallets

```text
id UUID
public_id
user_id FK
currency CHAR(3)
available_balance NUMERIC
status
version
created_at
updated_at
```

`version` podrá utilizarse para optimistic locking.

---

## transactions

```text
id UUID
public_id
sender_wallet_id
receiver_wallet_id
type
status
source_amount
source_currency
destination_amount
destination_currency
exchange_rate
description
risk_score
risk_level
created_at
completed_at
version
```

---

## ledger_entries

```text
id UUID
transaction_id
wallet_id
entry_type
amount
currency
created_at
```

Entry:

```text
DEBIT
CREDIT
```

---

## beneficiaries

```text
id
owner_user_id
beneficiary_user_id
alias
created_at
```

Constraint:

```text
UNIQUE(owner_user_id, beneficiary_user_id)
```

---

## risk_assessments

```text
id
transaction_id
total_score
risk_level
decision
created_at
```

---

## risk_rule_results

```text
id
risk_assessment_id
rule_name
score
reason
```

---

## risk_alerts

```text
id
transaction_id
risk_assessment_id
status
assigned_admin_id NULL
created_at
resolved_at NULL
```

---

## notifications

```text
id
user_id
type
title
message
read
created_at
```

---

## audit_logs

```text
id
event_type
actor_user_id
entity_type
entity_id
ip_address
user_agent
metadata JSONB
created_at
```

---

## refresh_tokens

```text
id
user_id
token_hash
expires_at
revoked_at
created_at
```

Preferiblemente no almacenar refresh tokens completos en texto plano.

---

# 19. IDENTIFICADORES

Usar UUID internamente.

Ejemplo:

```text
UUID
```

Para exposición pública utilizar identificadores independientes o UUID seguros.

Ejemplo visible:

```text
PF-TX-82A91823
```

Nunca asumir seguridad únicamente porque un ID sea UUID.

Siempre comprobar autorización.

---

# 20. MONEY VALUE OBJECT

Considerar un Value Object:

```java
Money
```

Conceptualmente:

```java
public record Money(
    BigDecimal amount,
    Currency currency
) {}
```

Debe controlar:

- precisión;
- currency;
- suma;
- resta;
- comparación;
- validación.

---

# 21. API REST

Base:

```text
/api/v1
```

---

# 22. AUTH ENDPOINTS

```http
POST /api/v1/auth/register
POST /api/v1/auth/login
POST /api/v1/auth/refresh
POST /api/v1/auth/logout
POST /api/v1/auth/verify-email
POST /api/v1/auth/forgot-password
POST /api/v1/auth/reset-password
```

---

# 23. USER ENDPOINTS

```http
GET   /api/v1/users/me
PATCH /api/v1/users/me
GET   /api/v1/users/me/sessions
DELETE /api/v1/users/me/sessions/{id}
```

---

# 24. WALLET ENDPOINTS

```http
GET /api/v1/wallets
GET /api/v1/wallets/{publicId}
GET /api/v1/wallets/{publicId}/balance
```

---

# 25. TRANSFER ENDPOINTS

```http
POST /api/v1/transfers
GET  /api/v1/transfers/{transactionPublicId}
```

Request:

```json
{
  "recipient": "jeremy@example.com",
  "amount": "250.00",
  "currency": "USD",
  "description": "Dinner"
}
```

Header:

```text
Idempotency-Key: UUID
```

Response:

```json
{
  "transactionId": "PF-TX-82A918",
  "status": "COMPLETED",
  "amount": "250.00",
  "currency": "USD",
  "recipient": {
    "displayName": "Jeremy M."
  },
  "createdAt": "2026-09-09T16:30:00Z"
}
```

---

# 26. TRANSACTION ENDPOINTS

```http
GET /api/v1/transactions
GET /api/v1/transactions/{publicId}
```

Query:

```text
?page=0
&size=20
&status=COMPLETED
&type=TRANSFER
&dateFrom=
&dateTo=
```

---

# 27. BENEFICIARY ENDPOINTS

```http
GET    /api/v1/beneficiaries
POST   /api/v1/beneficiaries
PATCH  /api/v1/beneficiaries/{id}
DELETE /api/v1/beneficiaries/{id}
```

---

# 28. ANALYTICS ENDPOINTS

```http
GET /api/v1/analytics/summary
GET /api/v1/analytics/activity
GET /api/v1/analytics/beneficiaries
```

---

# 29. NOTIFICATIONS

```http
GET   /api/v1/notifications
PATCH /api/v1/notifications/{id}/read
PATCH /api/v1/notifications/read-all
```

---

# 30. ADMIN ENDPOINTS

```http
GET   /api/v1/admin/users
GET   /api/v1/admin/users/{id}
PATCH /api/v1/admin/users/{id}/status

GET /api/v1/admin/transactions
GET /api/v1/admin/transactions/{id}

GET /api/v1/admin/risk-alerts
GET /api/v1/admin/risk-alerts/{id}
PATCH /api/v1/admin/risk-alerts/{id}

GET /api/v1/admin/audit-logs
GET /api/v1/admin/analytics
```

Todos requieren:

```text
ROLE_ADMIN
```

---

# 31. RESPUESTAS DE ERROR

Crear formato consistente.

Ejemplo:

```json
{
  "timestamp": "2026-09-09T16:00:00Z",
  "status": 400,
  "code": "INSUFFICIENT_FUNDS",
  "message": "The wallet does not have sufficient funds.",
  "path": "/api/v1/transfers",
  "traceId": "8acd91"
}
```

Nunca devolver:

```text
stack trace
SQL
password
internal class name
```

al cliente.

---

# 32. GLOBAL EXCEPTION HANDLER

Utilizar:

```java
@RestControllerAdvice
```

Errores conceptuales:

```text
ResourceNotFoundException
InsufficientFundsException
WalletBlockedException
SelfTransferException
DuplicateTransactionException
RiskRejectedException
InvalidCurrencyException
UnauthorizedOperationException
```

---

# 33. TRANSACCIONES ACID

El caso crítico es:

```java
@Transactional
TransferResult transfer(...)
```

La transferencia deberá ejecutar atómicamente:

```text
validate
lock wallet
check balance
evaluate risk
debit
credit
create transaction
create ledger entries
persist
publish eventual event after commit
```

---

# 34. CONCURRENCIA

Caso:

```text
Balance: 1000

Request A → 800
Request B → 700
```

Ambas llegan simultáneamente.

El sistema debe impedir:

```text
balance = -500
```

Investigar e implementar conscientemente:

```text
Pessimistic Locking
```

o:

```text
Optimistic Locking + retry strategy
```

Para una primera implementación financiera, puede utilizarse bloqueo de fila:

```text
SELECT ... FOR UPDATE
```

mediante JPA.

Documentar la decisión en:

```text
ADR
```

Architecture Decision Record.

---

# 35. IDEMPOTENCIA

Problema:

El usuario pulsa dos veces "Send".

O:

El frontend pierde conexión y reintenta.

Sin idempotencia:

```text
$200
$200
```

pueden ejecutarse dos veces.

Solución:

```text
Idempotency-Key
```

El backend registra:

```text
key
user
requestHash
response
status
expiration
```

Si llega nuevamente la misma key:

```text
return previous result
```

No ejecutar una nueva transferencia.

---

# 36. SECURITY ARCHITECTURE

Usar:

```text
Spring Security
JWT
Refresh Tokens
RBAC
```

---

# 37. JWT

Access token corto.

Ejemplo:

```text
15 min
```

Refresh token:

```text
7 days
```

Configurables mediante variables de entorno.

---

# 38. AUTORIZACIÓN

No confiar en IDs enviados por frontend.

Ejemplo incorrecto:

```http
GET /wallet/123
```

y simplemente devolverlo.

Primero:

```text
authenticated user
        ↓
owns wallet?
        ↓
yes / forbidden
```

---

# 39. PASSWORD POLICY

Ejemplo mínimo:

```text
8+ characters
uppercase/lowercase
number
```

No imponer reglas absurdamente complejas.

Preparar arquitectura para futuras mejoras.

---

# 40. RATE LIMITING

Endpoints críticos:

```text
/login
/register
/forgot-password
/transfers
```

Aplicar rate limiting.

Redis puede utilizarse en versiones avanzadas.

---

# 41. CORS

Configurar explícitamente dominios autorizados.

Nunca:

```text
* production
```

con credenciales.

---

# 42. CSRF

Si se utilizan cookies para autenticación relevante, analizar y configurar protección CSRF apropiadamente.

No desactivar mecanismos de seguridad sin documentar razones.

---

# 43. HEADERS

Aplicar headers de seguridad.

Frontend deployment debería incluir:

```text
Content-Security-Policy
X-Content-Type-Options
Referrer-Policy
```

entre otros según infraestructura.

---

# 44. SECRET MANAGEMENT

Nunca subir:

```text
JWT_SECRET
DB_PASSWORD
API_KEYS
SMTP_PASSWORD
```

al repositorio.

Usar variables de entorno.

Añadir `.env.example`.

---

# 45. RISK ENGINE — DISEÑO INTERNO

Interfaz:

```java
public interface RiskRule {
    RiskRuleResult evaluate(RiskContext context);
}
```

Context:

```text
sender
receiver
amount
time
historicalTransactions
device
beneficiaryRelationship
```

Resultado:

```text
rule
score
reason
triggered
```

Risk Engine:

```java
for each rule:
    result = rule.evaluate(context)

totalScore = sum(results)
riskLevel = classifier(totalScore)
decision = policy.decide(riskLevel)
```

---

# 46. REGLAS DE RIESGO INICIALES

### LargeAmountRiskRule

```text
amount > 1000 → +20
amount > 3000 → +40
```

### NewBeneficiaryRiskRule

```text
recipient never used → +15
```

### VelocityRiskRule

```text
> 5 transfers / 10 minutes → +30
```

### DailyVolumeRiskRule

```text
daily sent amount > threshold → +20
```

Los valores deben ser configurables.

No hardcodear números directamente dentro de servicios donde resulte evitable.

---

# 47. EVENT-DRIVEN DESIGN

Primera versión:

Spring Application Events.

Posteriormente:

RabbitMQ.

Eventos conceptuales:

```text
UserRegistered
TransferCreated
TransferCompleted
TransferBlocked
PasswordChanged
```

Consumidores:

```text
NotificationListener
AuditListener
AnalyticsListener
```

---

# 48. EVENTOS Y TRANSACCIONES

No enviar eventos externos antes de confirmar la transacción de base de datos.

Considerar:

```text
TransactionalEventListener
```

y posteriormente estudiar:

```text
Transactional Outbox Pattern
```

para garantizar consistencia entre base de datos y message broker.

Implementar Outbox Pattern solo en fase avanzada.

---

# 49. RABBITMQ

Cuando se introduzca:

```text
Transfer completed
       |
       v
RabbitMQ Exchange
       |
 ┌─────┼──────────────┐
 v     v              v
Email  Analytics      Audit
```

No introducir RabbitMQ antes de que el flujo principal funcione.

---

# 50. REDIS

Casos razonables:

```text
Exchange-rate cache
Rate limiting
Temporary session data
Potential idempotency cache
```

La información financiera crítica NO debe depender únicamente de Redis.

PostgreSQL continúa siendo source of truth.

---

# 51. POSTGRESQL COMO SOURCE OF TRUTH

Toda información crítica:

```text
users
wallets
transactions
ledger
risk
audit
```

vive en PostgreSQL.

---

# 52. MIGRACIONES

Usar Flyway.

Ejemplo:

```text
V1__create_users.sql
V2__create_wallets.sql
V3__create_transactions.sql
V4__create_ledger.sql
V5__create_beneficiaries.sql
```

Nunca depender de:

```text
hibernate ddl-auto=create
```

en producción.

---

# 53. JPA

Evitar problemas comunes:

```text
N+1 queries
EAGER por defecto indiscriminadamente
exposición directa de entidades
bidirectional relationships innecesarias
```

Usar:

```text
DTOs
projections
fetch joins
pagination
```

cuando sea necesario.

---

# 54. MAPEO

Puede utilizarse:

```text
MapStruct
```

para conversiones DTO ↔ application model cuando aporte valor.

No sobreutilizarlo.

---

# 55. DOCUMENTACIÓN API

Utilizar:

```text
Springdoc OpenAPI
Swagger UI
```

Documentar:

- request;
- response;
- auth;
- códigos HTTP;
- errores;
- ejemplos.

---

# 56. FRONTEND

Stack recomendado:

```text
React
TypeScript
Vite
React Router
TanStack Query
React Hook Form
Zod
Axios o fetch wrapper
```

UI:

```text
Tailwind CSS
```

o solución equivalente mantenible.

Charts:

```text
Recharts
```

---

# 57. ARQUITECTURA FRONTEND

```text
src/

app/
features/
components/
layouts/
pages/
services/
hooks/
types/
utils/
assets/
```

Preferible feature-based:

```text
features/auth
features/wallet
features/transfers
features/transactions
features/beneficiaries
features/admin
```

---

# 58. ESTADO DEL SERVIDOR

No almacenar manualmente cada respuesta REST en Context API.

Usar:

```text
TanStack Query
```

para:

```text
queries
mutations
cache
loading
retry
invalidations
```

---

# 59. AUTENTICACIÓN FRONTEND

Debe soportar:

```text
ProtectedRoute
RoleBasedRoute
token refresh
session expiration
logout
```

Nunca considerar el control visual como seguridad real.

Backend debe validar todos los permisos.

---

# 60. DISEÑO VISUAL

PayFlow debe tener estética fintech moderna y profesional.

### Dirección

```text
clean
minimal
financial
enterprise
high trust
```

### Layout

Desktop:

```text
┌───────────────────────────────────────────┐
│ Sidebar │ Main Content                    │
│         │                                 │
│ Logo    │ Header                          │
│ Home    │                                 │
│ Wallet  │ Cards / Graphs / Data           │
│ Send    │                                 │
│ History │                                 │
│ ...     │                                 │
└───────────────────────────────────────────┘
```

---

# 61. PALETA

Evitar colores exageradamente saturados.

Base sugerida:

```text
Background: off-white / near white
Surface: white
Primary: deep navy / royal blue
Success: restrained green
Danger: restrained red
Warning: amber
Text primary: near black
Text secondary: neutral gray
Borders: subtle gray
```

Dark mode puede añadirse posteriormente.

No usar gradientes excesivos.

---

# 62. TIPOGRAFÍA

Recomendada:

```text
Inter
```

o:

```text
Manrope
```

Jerarquía limpia.

Saldo principal debe ser visualmente protagonista.

---

# 63. DASHBOARD USER

Header:

```text
Good morning, Cristhian
Wednesday, September 9
```

Card principal:

```text
Total Balance
$9,420.50 USD

Sandbox funds
```

Acciones:

```text
Send
Receive
Add beneficiary
```

Analytics:

```text
Monthly activity
```

Movimientos:

```text
Recent transactions
```

---

# 64. COMPONENTE BALANCE CARD

Debe incluir:

```text
balance
currency
wallet status
visibility toggle
```

Ejemplo:

```text
Available balance

$9,420.50

••••••••

Wallet active
```

---

# 65. SEND MONEY FLOW

No hacer un formulario gigante.

Utilizar flujo claro.

### Paso 1

```text
Who are you sending to?
```

Buscar destinatario.

### Paso 2

```text
How much?
```

### Paso 3

```text
Review transfer
```

Mostrar:

```text
Recipient
Amount
Exchange rate
Description
Final amount
```

### Paso 4

Confirmación.

### Resultado

```text
Transfer successful
```

con receipt.

---

# 66. UX DE TRANSFERENCIA

Evitar ejecutar transferencia accidentalmente.

Botón:

```text
Review transfer
```

antes de:

```text
Confirm and send
```

Mientras se procesa:

deshabilitar botón para evitar doble envío.

Igualmente implementar idempotencia en backend.

---

# 67. HISTORIAL

Diseño desktop:

```text
Search                                  Filters

All | Sent | Received | Blocked

Date        Description      Status     Amount
------------------------------------------------
Sep 09      Jeremy           Complete   -$250
Sep 08      Valeria          Complete   +$100
Sep 08      Unknown          Blocked    $2,800
```

Mobile:

cards compactas.

---

# 68. STATUS BADGES

Ejemplos:

```text
COMPLETED
PENDING
BLOCKED
FAILED
```

No depender únicamente del color.

Mostrar texto/iconografía.

---

# 69. TRANSACTION DETAILS

Drawer o página:

```text
Transaction details

$250.00

Completed

Recipient
Jeremy Márquez

Reference
PF-TX-A82193

Date
Sep 9, 2026

Description
Dinner

Download receipt
```

PDF receipt puede agregarse posteriormente.

---

# 70. BENEFICIARIES

Mostrar:

```text
Avatar
Name
Alias
Last transfer
```

Acción rápida:

```text
Send
```

---

# 71. SETTINGS

Secciones:

```text
Profile
Security
Sessions
Notifications
Preferences
```

---

# 72. ADMIN UI

Debe diferenciarse claramente.

Sidebar:

```text
Overview
Users
Transactions
Risk Alerts
Audit Logs
System
```

Dashboard:

```text
Total Users          12,849
Transfers Today       1,402
Volume               $182,490
Blocked                  32
```

---

# 73. ADMIN RISK DASHBOARD

Tabla:

```text
Transaction
User
Amount
Score
Risk
Status
Date
```

Detalle:

```text
Risk score

92 / 100

Critical

Triggered rules
Large amount           +40
New beneficiary        +15
High velocity          +30
```

---

# 74. EMPTY STATES

Diseñarlos explícitamente.

Ejemplo:

```text
No transactions yet.

Your completed transfers will appear here.

[Send your first transfer]
```

No dejar tablas vacías sin explicación.

---

# 75. LOADING

Utilizar skeletons en lugar de spinners indiscriminados.

---

# 76. ERRORES UX

No mostrar:

```text
HTTP 500
```

Mostrar:

```text
We couldn't complete this transfer.
Your balance was not changed.
Please try again.
```

---

# 77. RESPONSIVE

Desktop:

sidebar fija.

Tablet:

sidebar colapsable.

Mobile:

bottom navigation o drawer.

Priorizar:

```text
Home
Send
Activity
Profile
```

---

# 78. TESTING

La aplicación debe tener una estrategia seria de testing.

Pirámide:

```text
Unit tests
Integration tests
API tests
E2E tests
```

---

# 79. UNIT TESTS

JUnit 5.

Mockito cuando sea necesario.

Casos del dominio:

```text
cannot transfer zero
cannot transfer negative
cannot transfer to self
insufficient balance
blocked wallet
risk calculation
money arithmetic
```

---

# 80. INTEGRATION TESTS

Testcontainers.

Levantar:

```text
PostgreSQL
```

real durante tests.

No confiar únicamente en H2 porque su comportamiento puede diferir de PostgreSQL.

---

# 81. TRANSFER INTEGRATION TEST

Caso crítico:

Given:

```text
Alice = $1000
Bob = $500
```

When:

```text
Alice sends $250
```

Then:

```text
Alice = $750
Bob = $750

transaction exists
two ledger entries exist
debit = credit
```

---

# 82. ROLLBACK TEST

Forzar fallo durante operación.

Resultado:

```text
Alice balance unchanged
Bob balance unchanged
no partial ledger
```

---

# 83. CONCURRENCY TEST

Ejecutar varias solicitudes concurrentes contra la misma wallet.

Validar:

```text
balance never negative
no duplicated operation
ledger remains balanced
```

Este test debe destacarse en el proyecto.

---

# 84. SECURITY TESTS

Verificar:

```text
unauthenticated → 401
wrong role → 403
user cannot access other wallet
blocked user cannot transfer
expired token rejected
```

---

# 85. FRONTEND TESTS

Herramientas:

```text
Vitest
React Testing Library
```

E2E futuro:

```text
Playwright
```

---

# 86. DOCKER

Repositorio:

```text
docker-compose.yml
```

Servicios:

```text
frontend
backend
postgres
redis
rabbitmq
```

No todos desde primera fase.

---

# 87. LOCAL DEVELOPMENT

Objetivo futuro:

```bash
docker compose up
```

y PayFlow queda operativo.

---

# 88. HEALTH CHECK

Backend:

```text
/actuator/health
```

Docker healthcheck correspondiente.

---

# 89. CI/CD

GitHub Actions.

Pipeline:

```text
checkout
      ↓
backend tests
      ↓
frontend tests
      ↓
build backend
      ↓
build frontend
      ↓
docker build
```

Posteriormente:

```text
deploy
```

---

# 90. CODE QUALITY

Integrar eventualmente:

```text
Checkstyle
SpotBugs
SonarCloud
ESLint
Prettier
```

No bloquear el inicio por configurar veinte herramientas.

---

# 91. LOGGING

Utilizar logging estructurado.

Nunca:

```text
System.out.println()
```

para logging real.

Logs:

```text
INFO
WARN
ERROR
```

No registrar:

```text
passwords
JWT
refresh tokens
personal secrets
```

---

# 92. CORRELATION ID

Cada request importante debería poder identificarse.

Ejemplo:

```text
X-Correlation-ID
```

Permite seguir una transferencia en logs.

---

# 93. OBSERVABILIDAD

Spring Boot Actuator.

Posteriormente:

```text
Prometheus
Grafana
```

Métricas:

```text
transfer.count
transfer.failure.count
transfer.blocked.count
transfer.processing.time
```

---

# 94. DOCUMENTACIÓN DEL REPOSITORIO

Root:

```text
README.md
```

Debe explicar:

```text
What is PayFlow?
Architecture
Features
Tech stack
Screenshots
Database
Security
Testing
Getting Started
Docker
API documentation
Roadmap
Disclaimer
```

Disclaimer visible:

```text
PayFlow is an educational sandbox project.
No real money or financial transactions are processed.
```

---

# 95. DOCUMENTACIÓN TÉCNICA

```text
docs/

architecture/
database/
api/
security/
adr/
diagrams/
```

---

# 96. ARCHITECTURE DECISION RECORDS

Crear ADRs para decisiones relevantes.

Ejemplo:

```text
ADR-001 Modular Monolith
ADR-002 PostgreSQL
ADR-003 JWT authentication
ADR-004 BigDecimal for monetary values
ADR-005 Pessimistic locking
ADR-006 Ledger architecture
ADR-007 RabbitMQ introduction
```

Formato:

```text
Context
Decision
Alternatives
Consequences
```

Esto aporta muchísimo valor profesional.

---

# 97. GIT

Branches:

```text
main
develop opcional
feature/*
fix/*
refactor/*
```

Commits claros.

Ejemplo:

```text
feat: implement wallet transfer use case
fix: prevent concurrent balance overspending
test: add transfer concurrency integration test
```

---

# 98. REPOSITORIO

Monorepo recomendado:

```text
payflow/

├── backend/
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile
│
├── frontend/
│   ├── src/
│   ├── package.json
│   └── Dockerfile
│
├── docs/
│   ├── architecture/
│   ├── database/
│   ├── api/
│   ├── adr/
│   └── screenshots/
│
├── infrastructure/
│
├── docker-compose.yml
├── .env.example
├── README.md
└── LICENSE
```

---

# 99. DEPENDENCIAS BACKEND INICIALES

No instalar todas de inmediato.

Primera etapa:

```text
Spring Web
Spring Data JPA
Validation
PostgreSQL Driver
Flyway
Lombok opcional
Spring Boot Test
Testcontainers
```

Segunda:

```text
Spring Security
JWT library
```

Después:

```text
Spring Cache
Redis
AMQP
Actuator
Springdoc
```

---

# 100. JAVA

Usar:

```text
Java 21
```

Aprovechar:

```text
records
switch expressions
sealed types cuando tengan sentido
modern Java APIs
```

No utilizar características modernas únicamente para demostrar que existen.

---

# 101. DTO

Nunca enviar directamente entidades JPA.

Ejemplo:

```text
TransferRequest
TransferResponse
TransactionSummaryResponse
TransactionDetailResponse
```

---

# 102. VALIDATION

Bean Validation.

Ejemplo:

```java
@NotBlank
@Email
@Positive
@Size
```

Validaciones complejas pertenecen al dominio/use cases.

---

# 103. CÓDIGOS HTTP

Usarlos correctamente.

```text
200 OK
201 Created
204 No Content
400 Bad Request
401 Unauthorized
403 Forbidden
404 Not Found
409 Conflict
422 opcional
429 Too Many Requests
500 Internal Server Error
```

---

# 104. PAGINACIÓN

Nunca devolver miles de movimientos.

Ejemplo:

```text
page
size
sort
```

Respuesta:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 150,
  "totalPages": 8
}
```

---

# 105. DATABASE INDEXING

Agregar índices guiados por consultas.

Ejemplos:

```text
users.email
transactions.sender_wallet_id
transactions.receiver_wallet_id
transactions.created_at
ledger_entries.transaction_id
risk_alerts.status
```

No crear índices indiscriminadamente.

---

# 106. DATABASE CONSTRAINTS

La integridad no debe depender únicamente de Java.

Utilizar:

```text
NOT NULL
UNIQUE
CHECK
FOREIGN KEY
```

cuando corresponda.

---

# 107. SOFT DELETE

No usar soft delete automáticamente en todas las entidades.

Para datos financieros:

```text
transactions
ledger entries
audit
```

no eliminar.

Para beneficiarios se puede eliminar o marcar inactive dependiendo de decisión posterior.

---

# 108. DATOS DEMO

Crear usuarios demo.

```text
cristhian@example.com
jeremy@example.com
valeria@example.com
admin@payflow.dev
```

Contraseñas únicamente documentadas para entorno demo.

Nunca credenciales reales.

---

# 109. SEEDING

Development:

```text
DemoDataInitializer
```

o migrations específicas.

Production:

desactivado.

---

# 110. ROADMAP

## FASE 0 — Foundation

Objetivo:

crear base profesional.

Entregables:

```text
repository
Spring Boot
React
PostgreSQL
Docker PostgreSQL
Flyway
basic architecture
README
```

---

## FASE 1 — Users + Wallet

Implementar:

```text
User
Wallet
registration
wallet creation
balance
```

Sin JWT inicialmente puede probarse mediante endpoints controlados, pero autenticación debe introducirse rápidamente.

---

## FASE 2 — Authentication

```text
Spring Security
JWT
Refresh token
Roles
Login
Logout
```

Frontend:

```text
Login
Register
Protected Routes
```

---

## FASE 3 — Transfer Core

```text
Transfer
Transaction
Balance validation
@Transactional
History
```

Este es el primer MVP funcional real.

---

## FASE 4 — Ledger

```text
Double-entry ledger
Debit
Credit
Integrity checks
```

Añadir tests exhaustivos.

---

## FASE 5 — Concurrency + Idempotency

Implementar:

```text
locking
Idempotency-Key
race-condition tests
```

Esta fase debe considerarse uno de los principales diferenciadores del proyecto.

---

## FASE 6 — Full User Interface

Crear:

```text
Dashboard
Send
History
Transaction details
Beneficiaries
Settings
```

---

## FASE 7 — Risk Engine

Implementar:

```text
RiskRule strategy
Risk score
Risk assessment
Risk alerts
Blocked transfers
```

---

## FASE 8 — Admin

```text
Admin dashboard
Users
Transactions
Alerts
Audit
```

---

## FASE 9 — External API

```text
ExchangeRateProvider
external adapter
caching
timeouts
fallback/error handling
```

---

## FASE 10 — Async Processing

Primero:

```text
Spring Events
```

Posteriormente:

```text
RabbitMQ
```

---

## FASE 11 — Advanced Testing

```text
Testcontainers
Concurrency
Security
Integration
Playwright
```

---

## FASE 12 — DevOps

```text
Docker
Compose
GitHub Actions
deployment
```

---

## FASE 13 — Observability

```text
Actuator
metrics
Prometheus
Grafana
structured logs
```

---

# 111. MVP

El MVP debe contener únicamente:

```text
Registration
Login
Wallet
Sandbox balance
Internal transfers
Transaction history
Ledger
Basic dashboard
PostgreSQL
React
Spring Boot
Basic tests
Docker
```

Risk Engine, RabbitMQ, Redis y analytics avanzados NO son necesarios para declarar MVP.

---

# 112. DEFINITION OF DONE

Una feature no está terminada solo porque "funciona".

Para considerarla finalizada deberá tener, según relevancia:

```text
business rules
backend
validation
error handling
authorization
database migration
tests
frontend
loading state
error state
responsive behaviour
documentation
```

---

# 113. CRITERIOS DE ACEPTACIÓN — TRANSFERENCIA

Dado:

```text
Alice balance = 1000 USD
Bob balance = 500 USD
```

Cuando Alice envía:

```text
250 USD
```

Entonces:

```text
Alice = 750
Bob = 750
```

Debe existir:

```text
1 transaction
1 DEBIT ledger entry
1 CREDIT ledger entry
```

Y:

```text
debits = credits
```

---

Si Alice intenta transferir:

```text
1500 USD
```

Entonces:

```text
transaction not completed
balances unchanged
no financial ledger mutation
error = INSUFFICIENT_FUNDS
```

---

Si dos operaciones concurrentes superan el saldo total:

solamente las operaciones financieramente válidas podrán completarse.

Nunca debe quedar:

```text
balance < 0
```

---

# 114. CASOS DE USO IMPORTANTES

## UC-001 Register

Actor:

Visitor.

Resultado:

```text
User
Wallet
Initial sandbox balance
```

---

## UC-002 Login

Actor:

User.

Resultado:

sesión autenticada.

---

## UC-003 Send Money

Actor:

User.

Precondiciones:

```text
authenticated
wallet active
recipient exists
```

Flujo:

```text
Select recipient
Enter amount
Review
Risk evaluation
Transaction
Ledger
Notification
Success
```

---

## UC-004 View Transaction History

Actor:

User.

Resultado:

movimientos paginados.

---

## UC-005 Investigate Risk Alert

Actor:

Admin.

Resultado:

visualiza operación y reglas activadas.

---

# 115. CASOS BORDE

El sistema debe considerar explícitamente:

```text
recipient does not exist
recipient equals sender
amount = 0
amount < 0
amount exceeds balance
wallet blocked
user blocked
duplicate request
concurrent requests
unsupported currency
external API unavailable
expired token
invalid token
deleted beneficiary
transaction not owned by user
database failure
message broker unavailable
```

---

# 116. DISPONIBILIDAD DE API EXTERNA

Una falla del exchange-rate provider no debe:

```text
corrupt balances
partially execute transaction
```

Si una operación requiere conversión y no se puede obtener una tasa válida:

```text
reject operation safely
```

---

# 117. TIMEOUTS

Toda llamada HTTP externa debe definir:

```text
connection timeout
read timeout
```

Nunca esperar indefinidamente.

---

# 118. RETRIES

No reintentar operaciones indiscriminadamente.

Especial cuidado con acciones que podrían duplicarse.

Para GETs idempotentes externos puede utilizarse retry limitado.

---

# 119. RESILIENCE

Futuro:

```text
Resilience4j
```

Puede utilizarse para:

```text
Circuit Breaker
Retry
Timeout
```

en integraciones externas.

No necesario en MVP.

---

# 120. MODELO DE AMENAZAS

Considerar al menos:

```text
credential stuffing
brute force
broken access control
IDOR
SQL injection
XSS
CSRF
token theft
replay
mass assignment
rate abuse
concurrent overspending
duplicate transfers
```

Documentar mitigaciones.

---

# 121. OWASP

Revisar arquitectura contra principios OWASP Top 10.

No afirmar cumplimiento/certificación formal.

Utilizar OWASP como referencia de desarrollo seguro.

---

# 122. PRIVACIDAD

Usar únicamente información ficticia durante demostraciones públicas.

No subir a GitHub datos personales reales.

---

# 123. ARQUITECTURA DE TRANSFERENCIA

Flujo objetivo:

```text
Client
  |
  | POST /transfers
  v
TransferController
  |
  v
TransferUseCase
  |
  +--> IdempotencyService
  |
  +--> WalletRepository
  |
  +--> RiskEngine
  |
  +--> TransactionFactory
  |
  +--> LedgerService
  |
  +--> TransactionRepository
  |
  +--> Domain Event
```

---

# 124. SECUENCIA

```text
React
 |
 | Transfer request
 v
Controller
 |
 v
Transfer Application Service
 |
 | lock sender wallet
 v
Database
 |
 | balance OK
 v
Risk Engine
 |
 | approved
 v
Ledger
 |
 | debit + credit
 v
Transaction Repository
 |
 | commit
 v
Event
 |
 +--> Notification
 +--> Analytics
 +--> Audit
```

---

# 125. PRINCIPIO DE RESPONSABILIDAD

TransferService no debe:

```text
send email directly
generate HTML
make UI decisions
construct SQL
validate JWT
call everything indiscriminately
```

Cada responsabilidad debe permanecer en su capa/módulo.

---

# 126. DDD LIGERO

No convertir el proyecto en una implementación académicamente rígida de DDD.

Aplicar conceptos útiles:

```text
Aggregates
Entities
Value Objects
Domain Events
Repositories
Business invariants
Ubiquitous Language
```

---

# 127. UBIQUITOUS LANGUAGE

Términos oficiales:

```text
User
Wallet
Transfer
Transaction
Ledger Entry
Beneficiary
Risk Assessment
Risk Alert
Sandbox Balance
Exchange Rate
```

Usar los mismos términos:

- backend;
- frontend;
- documentación;
- base de datos.

---

# 128. DIFERENCIA ENTRE TRANSFER Y TRANSACTION

Definir claramente.

**Transfer**

Proceso/intención de mover fondos entre wallets.

**Transaction**

Registro financiero resultante.

Se puede simplificar el modelo si durante la implementación se determina que separar ambas entidades no aporta valor.

Documentar cualquier cambio.

---

# 129. LEDGER COMO SOURCE OF TRUTH — DECISIÓN AVANZADA

Existen dos diseños posibles.

### Opción inicial

Wallet mantiene:

```text
availableBalance
```

y ledger registra movimientos.

Ventaja:

lecturas rápidas.

Requiere garantizar consistencia.

### Opción avanzada

Balance derivable desde ledger.

Para PayFlow se recomienda inicialmente mantener:

```text
wallet balance + immutable ledger
```

con tests de reconciliación.

No recalcular el ledger en cada consulta normal.

---

# 130. RECONCILIACIÓN

Crear posteriormente un proceso administrativo:

```text
calculated ledger balance
vs
wallet balance
```

Si existen diferencias:

```text
reconciliation alert
```

Esta funcionalidad puede convertirse en un excelente diferenciador técnico.

---

# 131. FRONTEND COMPONENT SYSTEM

Componentes reutilizables:

```text
Button
Input
Select
Modal
Drawer
Table
Badge
Card
MetricCard
MoneyDisplay
UserAvatar
TransactionRow
EmptyState
Skeleton
Toast
```

---

# 132. FORMULARIOS

React Hook Form + Zod.

Validación frontend mejora UX.

Backend continúa siendo autoridad final.

---

# 133. FEEDBACK

Toda acción debe indicar:

```text
loading
success
failure
```

Ejemplo:

```text
Processing transfer...
```

Luego:

```text
Transfer completed
```

Nunca hacer cambios silenciosos.

---

# 134. CONFIRMACIÓN

Operaciones sensibles requieren confirmación contextual.

No abusar de modales.

---

# 135. ICONOS

Usar una librería consistente.

Ejemplo:

```text
Lucide
```

No mezclar múltiples estilos.

---

# 136. IMÁGENES

La aplicación no necesita grandes fotografías.

Debe apoyarse principalmente en:

```text
layout
typography
icons
charts
data
microinteractions
```

---

# 137. ANIMACIONES

Sutiles:

```text
150-250ms
```

No crear animaciones decorativas excesivas.

---

# 138. URL FRONTEND

Ejemplo:

```text
/
 /login
 /register
 /app
 /app/send
 /app/transactions
 /app/transactions/:id
 /app/beneficiaries
 /app/settings

 /admin
 /admin/users
 /admin/transactions
 /admin/risk
 /admin/audit
```

---

# 139. LANDING PAGE

Debe incluir:

```text
Hero
Product overview
Features
Security-oriented messaging
Sandbox disclaimer
CTA
```

Texto conceptual:

```text
Payments infrastructure,
built to be explored.

Experience secure digital transfers,
risk analysis and financial operations
inside a complete sandbox environment.
```

---

# 140. README — ELEMENTOS DIFERENCIADORES

Destacar:

```text
Double-entry ledger
Transaction consistency
Concurrency control
Idempotent transfers
Risk engine
Modular architecture
Integration testing
External APIs
Event-driven processing
Docker
CI/CD
```

No promocionar únicamente:

```text
Spring Boot + React
```

Eso no diferencia el proyecto.

---

# 141. DIAGRAMAS PARA GITHUB

Crear:

```text
System Architecture
Database ERD
Transfer Sequence Diagram
Authentication Flow
Risk Engine Flow
Deployment Diagram
```

Preferiblemente mediante Mermaid cuando sea viable.

---

# 142. DEMO

Crear ambiente demo.

Usuarios predefinidos.

El reclutador debería poder:

```text
login
send money
see transaction
trigger risk alert
open admin panel
```

en pocos minutos.

---

# 143. FEATURE DEMOSTRATIVA

Crear un botón limitado a entorno demo:

```text
Reset demo account
```

Permite volver a saldo inicial.

NO disponible en producción real si la naturaleza del proyecto cambiara.

---

# 144. PORTFOLIO DESCRIPTION

Resumen futuro:

> PayFlow is a full-stack financial sandbox built with Java, Spring Boot, React and PostgreSQL. It simulates secure wallet transfers through transactional processing, double-entry ledger accounting, concurrency controls, idempotency, risk scoring, audit logging and event-driven workflows.

---

# 145. INTERVIEW TALKING POINTS

El proyecto debe permitir responder preguntas como:

```text
Why PostgreSQL?
How did you prevent double spending?
Why BigDecimal?
How does @Transactional work here?
What happens if two transfers happen simultaneously?
How does idempotency work?
Why modular monolith?
Why not microservices?
How does your ledger work?
How do you secure your endpoints?
How do you test concurrent transfers?
What happens when the external API is down?
What would you change at scale?
```

La arquitectura debe construirse de manera que estas respuestas sean reales, no memorizadas.

---

# 146. NO SOBREINGENIERÍA

Evitar comenzar con:

```text
Kubernetes
20 microservices
Kafka
CQRS
Event Sourcing
GraphQL
Service Mesh
```

sin necesidad.

La complejidad debe introducirse progresivamente.

---

# 147. POSIBLES EXTENSIONES FUTURAS

Cuando el sistema principal esté maduro:

```text
Scheduled transfers
Recurring payments
Spending categories
Virtual cards simulated
Multi-wallet
Multiple currencies
PDF receipts
WebSocket notifications
2FA
Device management
Reconciliation engine
Fraud analytics
Outbox Pattern
Service extraction
Kubernetes
Cloud deployment
```

No implementar hasta que el core esté terminado.

---

# 148. PRIORIDADES DE IMPLEMENTACIÓN

Cuando exista conflicto:

```text
correctness
security
data integrity
maintainability
testing
performance
visual polish
extra features
```

En una plataforma financiera simulada, integridad de datos es más importante que cantidad de funcionalidades.

---

# 149. REGLA PARA EL AGENTE DE IA

El agente NO debe implementar automáticamente todo este documento en una sola iteración.

Debe desarrollar PayFlow incrementalmente.

Antes de cada módulo:

1. entender alcance;
2. proponer estructura;
3. implementar;
4. ejecutar compilación;
5. ejecutar tests;
6. corregir;
7. verificar integración;
8. documentar decisiones importantes.

No crear código ficticio, pseudocódigo o archivos sin utilizar si se está realizando implementación real.

---

# 150. REGLAS DE CALIDAD PARA EL AGENTE

El agente debe:

- preservar arquitectura existente;
- evitar duplicación;
- evitar clases gigantes;
- escribir nombres descriptivos;
- utilizar inglés para código;
- utilizar nombres consistentes;
- manejar errores correctamente;
- crear migrations;
- mantener tests;
- no guardar secretos;
- no introducir dependencias sin necesidad;
- documentar decisiones relevantes;
- ejecutar tests después de cambios importantes;
- no deshabilitar seguridad para "hacer que funcione";
- no saltarse problemas de concurrencia;
- no usar `double` para dinero;
- no exponer entidades JPA;
- no usar IDs del frontend como prueba de autorización;
- no asumir que validación frontend es suficiente;
- no convertir todo en microservicios.

---

# 151. CONVENCIÓN DE CÓDIGO

Código:

```text
English
```

UI:

preferiblemente inglés para una versión internacional de portfolio.

Documentación puede mantenerse en inglés para GitHub.

El desarrollador puede trabajar internamente en español, pero nombres técnicos deben ser consistentes.

Ejemplos:

```java
TransferService
WalletRepository
RiskAssessment
```

No:

```java
ServicioTransferencia
WalletRepository
EvaluacionRisk
```

---

# 152. ESTÁNDAR DE NOMENCLATURA

Java:

```text
PascalCase classes
camelCase variables
UPPER_SNAKE_CASE constants
```

Database:

```text
snake_case
```

REST:

```text
plural nouns
kebab-case when required
```

---

# 153. BUILD TOOL

Preferencia:

```text
Maven
```

Archivo:

```text
pom.xml
```

Wrapper:

```text
mvnw
```

Debe incluirse.

---

# 154. ENTORNOS

Config:

```text
application.yml
application-dev.yml
application-test.yml
application-prod.yml
```

Nunca poner credenciales productivas en estos archivos.

---

# 155. ENVIRONMENT VARIABLES

Ejemplo:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
JWT_SECRET
JWT_ACCESS_EXPIRATION
JWT_REFRESH_EXPIRATION
EXCHANGE_API_KEY
REDIS_HOST
RABBITMQ_HOST
```

---

# 156. DEV EXPERIENCE

Debe ser fácil levantar el proyecto.

Ideal:

```bash
git clone ...
cp .env.example .env
docker compose up -d
./mvnw spring-boot:run
npm install
npm run dev
```

Posteriormente simplificar mediante Docker Compose completo.

---

# 157. API VERSIONING

Usar:

```text
/api/v1
```

desde el inicio.

---

# 158. OPENAPI

Swagger debe estar disponible en desarrollo.

En producción pública considerar restricciones según necesidad.

---

# 159. DESIGN SYSTEM

Crear tokens frontend:

```text
spacing
radius
shadow
typography
colors
```

Evitar valores aleatorios diferentes en cada pantalla.

---

# 160. BORDER RADIUS

Moderado.

No convertir cada elemento en píldora.

Cards:

```text
10-16px aprox.
```

según sistema visual final.

---

# 161. SHADOWS

Sutiles.

Preferir borders y hierarchy sobre sombras exageradas.

---

# 162. TABLES

Headers claros.

Hover discreto.

Filtrado.

Paginación.

Responsive mediante cards o scroll bien diseñado.

---

# 163. CHARTS

No llenar dashboard de gráficas innecesarias.

Una gráfica principal:

```text
Money movement over time
```

Puede mostrar:

```text
sent
received
```

---

# 164. USER TRUST

Siempre mostrar:

```text
transaction status
date
recipient
reference ID
```

Después de una transferencia.

---

# 165. SANDBOX INDICATOR

Persistente pero discreto.

Ejemplo en header:

```text
SANDBOX
```

Tooltip:

```text
PayFlow uses simulated funds only.
```

---

# 166. ACCESIBILIDAD DEL DINERO

Mostrar:

```text
$1,250.50 USD
```

No depender únicamente del símbolo `$`, porque múltiples países utilizanlo.

---

# 167. LOCALIZACIÓN

Arquitectura futura compatible con:

```text
i18n
```

pero no implementar traducción completa inicialmente.

---

# 168. DECIMALES

Currency-aware.

No asumir siempre dos decimales a nivel conceptual.

Para MVP USD/EUR pueden mostrarse dos.

Dominio debe ser más cuidadoso.

---

# 169. FECHAS

Backend:

```text
ISO-8601 UTC
```

Frontend:

localización.

---

# 170. DEPLOYMENT FUTURO

Posibilidades:

Frontend:

```text
Vercel
CloudFront/S3
```

Backend:

```text
AWS ECS
AWS EC2
Render/Railway inicialmente
```

Database:

```text
AWS RDS PostgreSQL
```

Redis:

```text
ElastiCache
```

No es obligatorio usar AWS desde primera publicación.

---

# 171. INFRAESTRUCTURA COMO CÓDIGO

Fase muy avanzada:

```text
Terraform
```

No necesaria para MVP.

---

# 172. LICENCIA

Para portfolio puede utilizarse:

```text
MIT
```

si el autor desea permitir reutilización.

---

# 173. BRANCH PROTECTION

Cuando se configure GitHub:

```text
main protected
tests required
PR review optional personal project
```

---

# 174. ISSUES

Utilizar GitHub Issues para features importantes.

Ejemplo:

```text
Implement transfer concurrency control
Add double-entry ledger
Create risk engine
```

Esto ayuda a mostrar proceso de ingeniería.

---

# 175. PROJECT BOARD

Opcional:

```text
Backlog
Ready
In Progress
Review
Done
```

---

# 176. PR TEMPLATE

Puede incluir:

```text
What changed?
Why?
How was it tested?
Screenshots
Risks
```

---

# 177. SECURITY DISCLAIMER

README:

> PayFlow is an educational software engineering project intended to simulate financial workflows. It does not process, store, transfer, or represent real-world funds and should not be used as financial infrastructure.

---

# 178. SUCCESS CRITERIA

PayFlow será considerado exitoso cuando:

```text
un usuario pueda registrarse
obtener wallet
transferir fondos simulados
ver movimiento
ledger permanezca consistente
concurrency attacks do not overspend
duplicate requests do not duplicate transfers
risk engine pueda bloquear una operación
admin pueda investigarla
frontend sea profesional
tests cubran core
proyecto pueda levantarse con Docker
API esté documentada
repositorio tenga arquitectura y documentación claras
```

---

# 179. PRINCIPAL DIFERENCIADOR DEL PROYECTO

La profundidad técnica debe estar concentrada especialmente en:

### 1. Double-entry ledger

No solo actualizar balance.

### 2. Transaction integrity

ACID y rollback.

### 3. Concurrency control

Evitar double spending.

### 4. Idempotency

Evitar transferencias duplicadas.

### 5. Risk Engine

Diseño extensible mediante estrategias.

### 6. Security

Spring Security y autorización real.

### 7. Testing

Especialmente integración y concurrencia.

### 8. Architecture

Monolito modular defendible.

Estos elementos son más importantes que agregar decenas de CRUD.

---

# 180. PRINCIPIO FINAL

PayFlow no debe intentar impresionar por cantidad de tecnologías.

Debe impresionar porque cada decisión tiene sentido.

El objetivo es poder mostrar un sistema y decir:

> Aquí existía un problema real de consistencia.

> Esta fue la solución.

> Estas fueron las alternativas.

> Esta es la prueba automatizada que demuestra que funciona.

Ese enfoque convierte PayFlow de un proyecto académico ordinario en un proyecto serio de ingeniería de software.

---

# 181. INSTRUCCIÓN FINAL PARA EL AGENTE DESARROLLADOR

Construye PayFlow como un producto real de ingeniería, aunque opere únicamente con fondos ficticios.

Prioriza:

```text
domain correctness
financial consistency
security
clean architecture
testing
maintainability
professional UX
```

No priorices:

```text
feature quantity
unnecessary frameworks
premature microservices
visual gimmicks
overengineering
```

Antes de implementar una decisión arquitectónica importante, analiza:

```text
Problem
Options
Decision
Trade-offs
```

Mantén el sistema ejecutable después de cada fase.

No implementes características avanzadas hasta que las fases fundamentales sean estables.

El resultado final debe ser suficientemente sólido como para funcionar como:

**proyecto Full-Stack académico + proyecto de portfolio + demostración técnica de Java/Spring Boot + caso de estudio de arquitectura de software.**