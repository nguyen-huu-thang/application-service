# Application Service - Kiến trúc

[English](../en/architecture.md)

## Nguyên tắc

Application Service theo **Hexagonal Architecture** với DDD tactical patterns, phong cách giống hệt user-service. Quy tắc quan trọng nhất phân biệt với user-service: **business logic chỉ nằm trong `domain/`, không có trong `application/usecase/` hay `application/service/`**.

Use case chỉ orchestrate: load qua port - gọi domain method - save qua port - publish event. State transition, invariant và kiểm tra quyền nằm trong domain.

---

## Quy tắc phụ thuộc giữa các tầng

```
domain/          <- không phụ thuộc ai; Java thuần
application/     <- chỉ phụ thuộc domain
integration/     <- phụ thuộc domain và infrastructure (Trust integration)
infrastructure/  <- implement port của application; phụ thuộc domain + application
api/             <- gọi use case của application; phụ thuộc application + domain DTO
config/          <- wire tất cả; phụ thuộc mọi tầng
```

Vi phạm thứ tự này là lỗi kiến trúc nghiêm trọng.

---

## Cấu trúc thư mục

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
|   |   +-- application/           một class mỗi use case
|   |   +-- permission/
|   |   +-- internal/
|   +-- mapper/
|   +-- service/
|       +-- event/                 ApplicationDomainEventDispatcher (điều phối, không chứa rule)
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
|   +-- usecase/                   UseCaseConfig.java - wire tất cả use case bean
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
|   +-- trust/                     copy từ user-service
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
|   |   +-- trust/                 gRPC client tới Trust Service
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

`Application` là aggregate root. Được thiết kế theo **immutable style**: mỗi method thay đổi trạng thái trả về instance `Application` mới thay vì mutate trực tiếp.

```
Application
  identity_id     : ApplicationId       (KSUID 24 byte, bất biến)
  applicationCode : ApplicationCode     (value object: Base62 chữ thường, 2-64 ký tự)
  name            : ApplicationName     (value object: không rỗng, tối đa 255)
  description     : ApplicationDescription (value object: nullable, tối đa 2000)
  status          : ApplicationStatus   (PENDING_REVIEW | ACTIVE | SUSPENDED | DISABLED | RETIRED)
  stateVersion    : long                (tăng khi status/permission thay đổi)
  changeSequence  : long                (con trỏ monotonic, cập nhật cùng state_version)
  tenantId        : TenantId            (null hiện tại)
  permissions     : List<SystemPermission>
  domainEvents    : List<DomainEvent>   (nội bộ, use case pull sau save)
```

Aggregate load permission cùng lúc (không lazy). Domain event được raise nội bộ, use case gọi `pullDomainEvents()` sau khi save.

### State Machine

```
PENDING_REVIEW --activate()--> ACTIVE
ACTIVE --suspend()--> SUSPENDED
SUSPENDED --reactivate()--> ACTIVE
ACTIVE --disable()--> DISABLED
SUSPENDED --disable()--> DISABLED
DISABLED --retire()--> RETIRED
```

Mọi chuyển trạng thái khác đều ném `InvalidStatusTransitionException`. Tất cả điều kiện bảo vệ nằm trong aggregate `Application`, không phải trong use case.

### SystemPermission

`SystemPermission` là entity trong aggregate `Application`. Giữ `app_identity_id` + `PermissionCode`. Enum `PermissionCode` liệt kê các quyền hệ thống có thể cấp cho APPLICATION subject:

```java
DATA_CREATE_OBJECT
DATA_READ_OBJECT
DATA_UPDATE_OBJECT
DATA_DELETE_OBJECT
DATA_CREATE_SCHEMA
DATA_READ_SCHEMA
// mở rộng khi thêm resource service mới
```

---

## Pattern Use Case

Mỗi use case đều theo cùng một pattern ba bước:

```java
@Transactional
public void activate(ActivateApplicationCommand command) {
    // 1. Load qua output port
    Application app = loadPort.findById(command.identityId())
        .orElseThrow(() -> new ApplicationNotFoundException(command.identityId()));

    // 2. Gọi domain - business logic nằm trong aggregate
    Application activated = app.activate();

    // 3. Save qua output port
    savePort.save(activated);

    // 4. Publish domain event -> Kafka outbox
    activated.pullDomainEvents().forEach(eventDispatcher::dispatch);
}
```

Không có business rule nào được viết trong use case. Use case chỉ là coordinator.

---

## Xử lý lỗi

Application Service dùng chuẩn lỗi hai tầng của platform với dải mã `030000-039999`:

| Dải mã | Visibility | Dùng cho |
|---|---|---|
| 030000-033999 | Private (nội bộ service) | Lỗi implementation nội bộ |
| 034000-036999 | System (service khác đọc được) | Vd `ApplicationNotFoundException` |
| 037000-039999 | Public (admin client đọc được) | Vd `DuplicateApplicationCodeException` |

Lỗi trong `domain/error/` (catalog không phụ thuộc framework). Exception trong `common/exception/` (`AppException` + `PrivateError` / `SystemError` / `PublicError`). REST và gRPC adapter áp dụng filter visibility tương ứng.

Tham chiếu: `trust-service` có layout hoàn chỉnh.

---

## Cơ sở dữ liệu

Schema quản lý bởi JPA `ddl-auto: update` trong giai đoạn phát triển. Flyway thêm vào trước khi deploy production.

| Bảng | Mục đích |
|---|---|
| `applications` | Registry ứng dụng |
| `app_system_permissions` | Quyền hệ thống cấp cho app |
| `outbox_events` | Transactional Outbox cho Kafka event |
| `certificates` | Cert mTLS từ Trust (Trust integration) |
| `key_contexts` | Public key của Trust để verify JWT admin |

File proto trong `src/main/proto/`, biên dịch bởi Maven protobuf plugin.
