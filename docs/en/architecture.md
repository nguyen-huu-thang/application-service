# Application Service - Architecture

[Tieng Viet](../vn/architecture.md)

## Principles

Application Service follows **Hexagonal Architecture** with DDD tactical patterns, mirroring the layout of user-service. The key rule that differs from user-service: **business logic lives in `domain/`, never in `application/usecase/` or `application/service/`**.

Use cases only orchestrate: load via port - call domain method - save via port - publish event. State transitions, invariants, and permission guards are all in the domain.

---

## Layer Dependency Rules

```
domain/          <- no dependencies; pure Java
application/     <- depends on domain only
integration/     <- depends on domain and infrastructure (Trust integration)
infrastructure/  <- implements application ports; depends on domain + application
api/             <- calls application use cases; depends on application + domain DTOs
config/          <- wires everything; depends on all layers
```

Violating this dependency order is a hard architectural error.

---

## Directory Layout

```
src/main/java/vn/xime/application/
|
+-- api/
|   +-- grpc/
|   |   +-- external/
|   |   |   +-- application/       ApplicationAdminGrpcService
|   |   |   +-- permission/        ApplicationPermissionGrpcService
|   |   +-- internal/
|   |   |   +-- subject/           ApplicationSubjectGrpcService
|   |   +-- mapper/
|   +-- rest/
|       +-- internal/              actuator / health
|       +-- mapper/
|
+-- application/
|   +-- dto/
|   |   +-- application/           RegisterApplicationCommand, ActivateApplicationCommand,
|   |   |                          ApplicationResult, ApplicationSummaryResult, PageResult
|   |   +-- permission/            GrantSystemPermissionCommand, RevokeSystemPermissionCommand
|   |   +-- internal/              SubjectInfoResult, ChangedApplicationsResult
|   +-- port/
|   |   +-- in/
|   |   |   +-- application/       use case interfaces
|   |   |   +-- permission/        use case interfaces
|   |   |   +-- internal/          use case interfaces
|   |   +-- out/
|   |       +-- application/       LoadApplicationPort, SaveApplicationPort,
|   |       |                      CheckApplicationCodeExistsPort, LoadChangedApplicationsPort
|   |       +-- event/             SaveOutboxEventPort, PublishSubjectChangedEventPort
|   +-- usecase/
|   |   +-- application/           one class per use case
|   |   +-- permission/
|   |   +-- internal/
|   +-- mapper/
|   +-- service/
|       +-- event/                 ApplicationDomainEventDispatcher (orchestration only)
|
+-- common/
|   +-- annotation/
|   +-- constants/
|   +-- exception/                 ApplicationNotFoundException, InvalidStatusTransitionException,
|   |                              DuplicateApplicationCodeException, PermissionAlreadyGrantedException,
|   |                              PermissionNotGrantedException
|   +-- util/
|   +-- validation/
|
+-- config/
|   +-- cache/
|   +-- database/
|   +-- event/                     Kafka producer + Outbox scheduler config
|   +-- grpc/
|   +-- security/
|   +-- trust/
|   +-- usecase/                   UseCaseConfig.java - wires all use case beans
|
+-- domain/
|   +-- sharedkernel/
|   |   +-- model/                 ApplicationId, TenantId
|   |   +-- factory/               ApplicationIdFactory
|   |   +-- service/               ApplicationCodeNormalizer
|   |   +-- context/               OperationContext
|   |   +-- event/                 DomainEvent (marker interface)
|   +-- application/
|   |   +-- Application.java       aggregate root (immutable style)
|   |   +-- ApplicationStatus.java
|   |   +-- ApplicationCode.java
|   |   +-- ApplicationName.java
|   |   +-- ApplicationDescription.java
|   |   +-- event/                 ApplicationRegisteredEvent, ApplicationActivatedEvent,
|   |                              ApplicationSuspendedEvent, ApplicationReactivatedEvent,
|   |                              ApplicationDisabledEvent, ApplicationRetiredEvent
|   +-- permission/
|   |   +-- SystemPermission.java
|   |   +-- PermissionCode.java
|   |   +-- event/                 SystemPermissionGrantedEvent, SystemPermissionRevokedEvent
|   +-- audit/
|   +-- error/                     ErrorCode catalog, Visibility, GrpcCode
|
+-- integration/
|   +-- trust/                     copied from user-service
|       +-- key/                   VerificationKeyResolver, VerificationKeySynchronizer
|       +-- cert/                  TrustCertificateResolver, TrustCertificateSynchronizer
|       +-- model/                 Certificate, RootCertificate
|       +-- publicca/              TrustRootCertificateResolver
|       +-- ssl/                   GrpcServerSslContextProvider, TrustSslContextProvider
|       +-- resolver/
|
+-- infrastructure/
|   +-- cache/
|   +-- event/                     KafkaSubjectChangedEventPublisher
|   |                              OutboxEventPublisherScheduler
|   +-- grpc/
|   |   +-- trust/                 gRPC client to Trust Service
|   +-- persistence/
|   |   +-- entity/                ApplicationEntity, AppSystemPermissionEntity,
|   |   |                          OutboxEventEntity, CertificateEntity, KeyContextEntity
|   |   +-- mapper/                ApplicationMapper (domain <-> entity)
|   |   +-- repository/            JpaApplicationRepositoryAdapter
|   +-- scheduler/
|   +-- security/
|
+-- ApplicationServiceApplication.java
```

---

## Domain Model

### Application Aggregate

`Application` is the aggregate root. It is designed **immutable-style**: every state-changing method returns a new `Application` instance rather than mutating in place.

```
Application
  identity_id     : ApplicationId       (24-byte KSUID, permanent)
  applicationCode : ApplicationCode     (value object: Base62 lowercase, 2-64 chars)
  name            : ApplicationName     (value object: non-empty, max 255)
  description     : ApplicationDescription (value object: nullable, max 2000)
  status          : ApplicationStatus   (PENDING_REVIEW | ACTIVE | SUSPENDED | DISABLED | RETIRED)
  stateVersion    : long                (incremented on every status/permission change)
  changeSequence  : long                (monotonic cursor, updated alongside stateVersion)
  tenantId        : TenantId            (null for now)
  permissions     : List<SystemPermission>
  domainEvents    : List<DomainEvent>   (internal, pulled by use case after save)
```

The aggregate loads its permissions eagerly (no lazy loading) and raises domain events internally. Use cases pull events via `pullDomainEvents()` after saving.

### State Machine

```
PENDING_REVIEW --activate()--> ACTIVE
ACTIVE --suspend()--> SUSPENDED
SUSPENDED --reactivate()--> ACTIVE
ACTIVE --disable()--> DISABLED
SUSPENDED --disable()--> DISABLED
DISABLED --retire()--> RETIRED
```

Any other transition throws `InvalidStatusTransitionException`. All guard conditions live in the `Application` aggregate, not in use cases.

### SystemPermission

`SystemPermission` is an entity within the `Application` aggregate. It holds `app_identity_id` + `PermissionCode`. The `PermissionCode` enum lists all system-level permissions that can be granted to an APPLICATION subject:

```java
DATA_CREATE_OBJECT
DATA_READ_OBJECT
DATA_UPDATE_OBJECT
DATA_DELETE_OBJECT
DATA_CREATE_SCHEMA
DATA_READ_SCHEMA
// extended as new resource services are added
```

---

## Use Case Pattern

All use cases follow the same three-step pattern:

```java
@Transactional
public void activate(ActivateApplicationCommand command) {
    // 1. Load via output port
    Application app = loadPort.findById(command.identityId())
        .orElseThrow(() -> new ApplicationNotFoundException(command.identityId()));

    // 2. Call domain - business logic stays in the aggregate
    Application activated = app.activate();

    // 3. Save via output port
    savePort.save(activated);

    // 4. Publish domain events -> Kafka outbox
    activated.pullDomainEvents().forEach(eventDispatcher::dispatch);
}
```

No business rule is written inside the use case. The use case is only the coordinator.

---

## Error Handling

Application Service uses the platform-standard two-tier error model with error code range `030000-039999`:

| Range | Visibility | Used for |
|---|---|---|
| 030000-033999 | Private | Internal service errors, never leave the service |
| 034000-036999 | System | Readable by other services via gRPC |
| 037000-039999 | Public | Readable by browsers via REST |

Errors in `domain/error/` (framework-neutral catalog). Exceptions in `common/exception/` (`AppException` + `PrivateError` / `SystemError` / `PublicError`). REST and gRPC adapters apply the appropriate visibility filter.

Reference: `trust-service` for the complete layout implementation.

---

## Database

Schema managed by JPA `ddl-auto: update` during development. Flyway added before production deployment.

| Table | Purpose |
|---|---|
| `applications` | Application registry |
| `app_system_permissions` | System permissions granted to apps |
| `outbox_events` | Transactional Outbox for Kafka events |
| `certificates` | mTLS certificates from Trust (Trust integration) |
| `key_contexts` | Trust public keys for JWT admin verification |

Proto source files in `src/main/proto/`, compiled by the Maven protobuf plugin.
