# Application Service - Overview

[Tieng Viet](../vn/overview.md)

## Role

Application Service is the **Application Identity Domain Service** of the Xime Base Platform. Its single responsibility is to act as the source of truth for everything related to APPLICATION subjects:

- Registering applications and issuing permanent `identity_id` values
- Storing metadata (`application_code`, name, description)
- Managing the application lifecycle (state machine)
- Granting and revoking System Permissions for APPLICATION subjects
- Notifying resource services when application state or permissions change

It has no role in authentication, session management, or business logic of any individual application.

---

## Position in Base Platform

```
Trust Service        <- CA, mTLS certs, JWT signing keys
Identity Service     <- Authentication gateway, issues JWT for HUMAN/BOT/AI_AGENT
User Service         <- HUMAN subject registry
Agent Service        <- BOT, AI_AGENT subject registry
Application Service  <- APPLICATION subject registry  (this service)
Data Service         <- Data infrastructure, enforces subject permissions
Notification Service <- Notification delivery
Payment Service      <- Payment processing
```

Application Service is the counterpart to User Service and Agent Service. While User Service owns HUMAN subjects and Agent Service owns BOT/AI_AGENT subjects, Application Service owns APPLICATION subjects.

---

## Boundaries

| Application Service DOES | Application Service DOES NOT |
|---|---|
| Register apps, issue 24-byte `identity_id` | Issue or verify JWT tokens |
| Store metadata: code, name, description | Hold any credential for apps |
| Manage lifecycle (state machine + `state_version`) | Store binding service-to-app (Trust owns that) |
| Grant and revoke System Permissions | Participate in Identity login flow |
| Publish sync events for resource services | Contain business logic of individual apps |
| Expose Pull API for fallback reconciliation | Become a realtime dependency of other services |

---

## Why APPLICATION Does Not Use JWT

All calls made on behalf of an APPLICATION originate from service processes running inside the mTLS mesh. The mTLS certificate is always available and constitutes stronger proof-of-possession than a bearer token. Trust Service embeds `owner_app_identity_id` into the certificate SAN, so no bootstrap API or hardcoded config is needed.

Requiring a JWT would mean APPLICATION subjects depend on Identity Service at runtime, introducing a coupling that does not exist by design.

Detail: [Application Identity](application-identity.md).

---

## Subject Types on the Platform

```
SubjectType     Owner Service          Auth Mechanism       JWT?
HUMAN        -> user-service           credential + Identity  yes
BOT          -> agent-service          API key + Identity     yes
AI_AGENT     -> agent-service          API key + Identity     yes
APPLICATION  -> application-service    cert (Trust) + sync    no
(SERVICE     -> trust-service          cert mTLS              never a subject)
```

SERVICE is never a Subject and never appears as a data owner or permission holder.

---

## Port and Database

| Item | Value |
|---|---|
| HTTP port | 8085 |
| gRPC port | 9094 |
| Database | `application_service` |
| Root package | `vn.xime.application` |
| Main class | `ApplicationServiceApplication` |

---

## Relationship with Trust Service

Application Service is itself a Base Platform service like any other: it bootstraps its own mTLS certificate from Trust Service (the certificate does NOT carry `owner_app_identity_id`).

The `owner_app_identity_id` concept only applies to APPLICATION-Layer service processes. Trust Service's `services` registry table carries a nullable `owner_app_identity_id BYTEA(24)` column; Application Service is the authoritative source of those identity IDs.

---

## Relationship with Resource Services

Resource services (data-service, and future services) cache subject information and system permissions for APPLICATION subjects. Application Service pushes updates via Kafka and exposes a Pull API for reconciliation. Resource services can operate for extended periods without contacting Application Service - they only need fresh data to enforce permission decisions.

Detail: [Sync Mechanism](sync.md).
