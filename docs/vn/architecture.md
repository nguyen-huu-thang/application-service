# Application Service - Kien truc

[English](../en/architecture.md)

## Nguyen tac

Application Service theo **Hexagonal Architecture** voi DDD tactical patterns, phong cach giong het user-service. Quy tac quan trong nhat phan biet voi user-service: **business logic chi nam trong `domain/`, khong co trong `application/usecase/` hay `application/service/`**.

Use case chi orchestrate: load qua port - goi domain method - save qua port - publish event. State transition, invariant va kiem tra quyen nam trong domain.

---

## Quy tac phu thuoc giua cac tang

```
domain/          <- khong phu thuoc ai; Java thuan
application/     <- chi phu thuoc domain
integration/     <- phu thuoc domain va infrastructure (Trust integration)
infrastructure/  <- implement port cua application; phu thuoc domain + application
api/             <- goi use case cua application; phu thuoc application + domain DTO
config/          <- wire tat ca; phu thuoc moi tang
```

Vi pham thu tu nay la loi kien truc nghiem trong.

---

## Cau truc thu muc

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
|   |   |   +-- application/       interface use case
|   |   |   +-- permission/        interface use case
|   |   |   +-- internal/          interface use case
|   |   +-- out/
|   |       +-- application/       LoadApplicationPort, SaveApplicationPort,
|   |       |                      CheckApplicationCodeExistsPort, LoadChangedApplicationsPort
|   |       +-- event/             SaveOutboxEventPort, PublishSubjectChangedEventPort
|   +-- usecase/
|   |   +-- application/           mot class moi use case
|   |   +-- permission/
|   |   +-- internal/
|   +-- mapper/
|   +-- service/
|       +-- event/                 ApplicationDomainEventDispatcher (dieu phoi, khong chua rule)
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
|   +-- usecase/                   UseCaseConfig.java - wire tat ca use case bean
|
+-- domain/
|   +-- sharedkernel/
|   |   +-- model/                 ApplicationId, TenantId
|   |   +-- factory/               ApplicationIdFactory
|   |   +-- service/               ApplicationCodeNormalizer
|   |   +-- context/               OperationContext
|   |   +-- event/                 DomainEvent (interface marker)
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
|   +-- error/                     Catalog ErrorCode, Visibility, GrpcCode
|
+-- integration/
|   +-- trust/                     copy tu user-service
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
|   |   +-- trust/                 gRPC client toi Trust Service
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

`Application` la aggregate root. Duoc thiet ke theo **immutable style**: moi method thay doi trang thai tra ve instance `Application` moi thay vi mutate truc tiep.

```
Application
  identity_id     : ApplicationId       (KSUID 24 byte, bat bien)
  applicationCode : ApplicationCode     (value object: Base62 chu thuong, 2-64 ky tu)
  name            : ApplicationName     (value object: khong rong, toi da 255)
  description     : ApplicationDescription (value object: nullable, toi da 2000)
  status          : ApplicationStatus   (PENDING_REVIEW | ACTIVE | SUSPENDED | DISABLED | RETIRED)
  stateVersion    : long                (tang khi status/permission thay doi)
  changeSequence  : long                (con tro monotonic, cap nhat cung state_version)
  tenantId        : TenantId            (null hien tai)
  permissions     : List<SystemPermission>
  domainEvents    : List<DomainEvent>   (noi bo, use case pull sau save)
```

Aggregate load permission cung luc (khong lazy). Domain event duoc raise noi bo, use case goi `pullDomainEvents()` sau khi save.

### State Machine

```
PENDING_REVIEW --activate()--> ACTIVE
ACTIVE --suspend()--> SUSPENDED
SUSPENDED --reactivate()--> ACTIVE
ACTIVE --disable()--> DISABLED
SUSPENDED --disable()--> DISABLED
DISABLED --retire()--> RETIRED
```

Moi chuyen trang thai khac deu nem `InvalidStatusTransitionException`. Tat ca dieu kien bao ve nam trong aggregate `Application`, khong phai trong use case.

### SystemPermission

`SystemPermission` la entity trong aggregate `Application`. Giu `app_identity_id` + `PermissionCode`. Enum `PermissionCode` liet ke cac quyen he thong co the cap cho APPLICATION subject:

```java
DATA_CREATE_OBJECT
DATA_READ_OBJECT
DATA_UPDATE_OBJECT
DATA_DELETE_OBJECT
DATA_CREATE_SCHEMA
DATA_READ_SCHEMA
// mo rong khi them resource service moi
```

---

## Pattern Use Case

Moi use case deu theo cung mot pattern ba buoc:

```java
@Transactional
public void activate(ActivateApplicationCommand command) {
    // 1. Load qua output port
    Application app = loadPort.findById(command.identityId())
        .orElseThrow(() -> new ApplicationNotFoundException(command.identityId()));

    // 2. Goi domain - business logic nam trong aggregate
    Application activated = app.activate();

    // 3. Save qua output port
    savePort.save(activated);

    // 4. Publish domain event -> Kafka outbox
    activated.pullDomainEvents().forEach(eventDispatcher::dispatch);
}
```

Khong co business rule nao duoc viet trong use case. Use case chi la coordinator.

---

## Xu ly loi

Application Service dung chuẩn loi hai tang cua platform voi dai ma `030000-039999`:

| Dai ma | Visibility | Dung cho |
|---|---|---|
| 030000-033999 | Private (noi bo service) | Loi implementation noi bo |
| 034000-036999 | System (service khac doc duoc) | Vd `ApplicationNotFoundException` |
| 037000-039999 | Public (admin client doc duoc) | Vd `DuplicateApplicationCodeException` |

Loi trong `domain/error/` (catalog khong phu thuoc framework). Exception trong `common/exception/` (`AppException` + `PrivateError` / `SystemError` / `PublicError`). REST va gRPC adapter ap dung filter visibility tuong ung.

Tham chieu: `trust-service` co layout hoan chinh.

---

## Co so du lieu

Schema quan ly boi JPA `ddl-auto: update` trong giai doan phat trien. Flyway them vao truoc khi deploy production.

| Bang | Muc dich |
|---|---|
| `applications` | Registry ung dung |
| `app_system_permissions` | Quyen he thong cap cho app |
| `outbox_events` | Transactional Outbox cho Kafka event |
| `certificates` | Cert mTLS tu Trust (Trust integration) |
| `key_contexts` | Public key cua Trust de verify JWT admin |

File proto trong `src/main/proto/`, bien dich boi Maven protobuf plugin.
