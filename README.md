# Application Service

**English** | [Tieng Viet](README-vn.md)

> Application Identity Domain Service for the Xime Base Platform - registry, metadata, lifecycle management, and system permission grants for APPLICATION subjects.

---

Application Service is the **application identity layer** of the Xime Base Platform. It is the source of truth for every APPLICATION subject - a product-level entity such as Xime Social, Xime Chat, or a dental clinic app. It does not issue tokens or sit in any authentication flow; applications are identified by mTLS certificates, not JWT.

```
Admin Tool
  | RegisterApplication (gRPC + mTLS)
  v
Application Service    <- registry, lifecycle, system permissions
  | SubjectChangedEvent (Kafka + Transactional Outbox)
  v
Resource Services      <- cache subject info, enforce permissions
(data-service, ...)
  ^ PollChangedApplications (gRPC + mTLS) -- fallback pull
```

---

## What Application Service Does

**Application Registry**
- Issue a 24-byte KSUID `identity_id` when a new application is registered
- Store `application_code` (Base62 lowercase, unique platform-wide), name, and description
- Normalize `application_code` (lowercase + trim) before any storage or lookup

**Lifecycle Management**
- Maintain application lifecycle: `PENDING_REVIEW` - `ACTIVE` - `SUSPENDED` / `DISABLED` - `RETIRED`
- Enforce state machine transitions; reject invalid transitions at the domain level
- Increment `state_version` and `change_sequence` on every status or permission change

**System Permission**
- Grant and revoke System Permissions (`DATA_CREATE_OBJECT`, `DATA_READ_OBJECT`, etc.) for APPLICATION subjects
- Act as the source of truth; resource services cache a copy and reconcile periodically

**Subject Sync**
- Publish `SubjectChangedEvent` to Kafka (via Transactional Outbox) whenever state or permissions change
- Expose a Pull API (`PollChangedApplications`) so resource services can reconcile on startup or after lag

## What Application Service Does NOT Do

- Does not issue JWT tokens - APPLICATION subjects do not use JWT
- Does not verify credentials - APPLICATION has no credential
- Does not participate in Identity Service's login flow
- Does not store which services belong to an app - Trust Service owns that via cert SAN
- Does not become a runtime dependency of other services - resource services survive weeks on cached data

---

## Key Design Decisions

### APPLICATION Identified by Certificate, Not JWT

Every call made on behalf of an APPLICATION is internal. The service's mTLS certificate is always present and constitutes proof-of-possession (stronger than a bearer token). Trust Service embeds `owner_app_identity_id` into the certificate SAN, so a service process knows which application it belongs to without any API call or hardcoded config.

This keeps Identity Service completely uninvolved in APPLICATION subjects at runtime.

### Soul-Body Identity Model

An application has two identity facets:

- **Soul** (`identity_id`): 24-byte KSUID issued by Application Service. Permanent, lives for years. Used for data ownership, permissions, and audit.
- **Body** (mTLS cert): issued by Trust Service per service process. Lives ~100 days, scales horizontally. Used for mTLS, routing, and process-level audit.
- **Link**: Trust embeds `owner_app_identity_id` into the cert SAN. A service process reads its own certificate to discover which application it belongs to - no bootstrap API needed.

The certificate only carries **immutable** binding data. Status and permissions travel via the sync channel.

### Status and Permissions Via Sync Channel

Certificates live ~100 days without revocation. Disabling an application must take effect within minutes. Therefore status and system permissions are propagated separately: Kafka push events (primary) and periodic gRPC pull (fallback/reconcile), with a staleness tolerance of a few minutes.

### application_code Normalization

`application_code` is normalized (lowercase + trim) before every storage or lookup, making it a stable human-readable identifier that can be used in URLs and config files without case ambiguity.

---

## Quick Start

> Application Service design is complete (2026-06). Code scaffold has not started.

```bash
./mvnw clean package -DskipTests
./mvnw spring-boot:run
```

REST: `8085` | gRPC: `9094`

Requires PostgreSQL at `localhost:5432/application_service` and Trust Service (for mTLS bootstrap).

---

## Architecture

Application Service follows **Hexagonal Architecture** with DDD tactical patterns, built on Spring Boot 4 / Java 25:

```
src/main/java/
+-- api/            <- Input adapters: gRPC (admin + internal subject API)
+-- application/    <- Use cases, ports, DTOs, mappers
+-- domain/         <- Pure business models: Application aggregate, state machine, permissions
+-- integration/    <- Trust Service integration (keys, certs, mTLS) - copied from user-service
+-- infrastructure/ <- JPA persistence, Kafka outbox publisher, gRPC clients, security
```

Business logic (state transitions, permission guards, invariants) lives exclusively in `domain/`. Use cases in `application/usecase/` only orchestrate: load - call domain - save - publish event.

Repository interfaces live in `application/port/out/` (not in `domain/`).

---

## Documentation

| Document | Description |
|---|---|
| [Overview](docs/en/overview.md) | Role, boundaries, position in Base Platform |
| [Architecture](docs/en/architecture.md) | Layer structure, DDD patterns, directory layout |
| [Application Identity](docs/en/application-identity.md) | Soul-body model, certificate binding, subject resolution |
| [API Reference](docs/en/api.md) | gRPC definitions - admin, permission, internal subject |
| [Sync Mechanism](docs/en/sync.md) | Kafka push + pull fallback, outbox, change_sequence |

---

## Base Platform Services

| Service | Role |
|---|---|
| `trust-service` | Trust infrastructure - CA, mTLS, JWT signing keys |
| `identity-service` | Authentication infrastructure - JWT, refresh tokens |
| `user-service` | Human Identity Domain Service |
| `agent-service` | Agent Identity Domain Service - BOT, AI_AGENT subjects |
| `application-service` | **Application Identity Domain Service - APPLICATION subjects** |
| `data-service` | Data infrastructure - object storage, permission |
| `notification-service` | Notification delivery |
| `payment-service` | Payment processing |

---

## Project Status

Application Service design is **complete** (2026-06). Code scaffold has not started. Next step: scaffold following user-service layout, then implement domain + application + infrastructure layers.

---

## License

MIT
