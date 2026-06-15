# Application Service

[English](README.md) | **Tieng Viet**

> Application Identity Domain Service cho Xime Base Platform - registry, metadata, quan ly vong doi va phan quyen he thong cho cac Subject loai APPLICATION.

---

Application Service la **tang dinh danh ung dung** cua Xime Base Platform. Day la nguon tin cay cho moi Subject loai APPLICATION - mot thuc the cap san pham nhu Xime Social, Xime Chat hay ung dung phong kham nha khoa. Service nay khong cap token va khong nam trong bat ky luong xac thuc nao; ung dung duoc dinh danh bang chung chi mTLS, khong phai JWT.

```
Cong cu Admin
  | RegisterApplication (gRPC + mTLS)
  v
Application Service    <- registry, vong doi, quyen he thong
  | SubjectChangedEvent (Kafka + Transactional Outbox)
  v
Resource Services      <- cache thong tin subject, kiem tra quyen
(data-service, ...)
  ^ PollChangedApplications (gRPC + mTLS) -- pull du phong
```

---

## Application Service lam gi

**Registry ung dung**
- Cap `identity_id` 24 byte (KSUID) khi dang ky ung dung moi
- Luu `application_code` (Base62 chu thuong, unique toan platform), ten va mo ta
- Chuan hoa `application_code` (lowercase + trim) truoc moi thao tac luu hoac tim kiem

**Quan ly vong doi**
- Duy tri vong doi: `PENDING_REVIEW` - `ACTIVE` - `SUSPENDED` / `DISABLED` - `RETIRED`
- Ap dung state machine; tu choi cac chuyen trang thai khong hop le ngay tai tang domain
- Tang `state_version` va `change_sequence` moi khi status hoac quyen thay doi

**Quyen he thong**
- Cap va thu hoi System Permission (`DATA_CREATE_OBJECT`, `DATA_READ_OBJECT`...) cho subject APPLICATION
- La nguon truth; resource service giu ban cache va dong bo dinh ky

**Dong bo Subject**
- Publish `SubjectChangedEvent` len Kafka (qua Transactional Outbox) moi khi state hoac quyen thay doi
- Expose Pull API (`PollChangedApplications`) de resource service co the reconcile khi khoi dong hoac bi tre

## Application Service KHONG lam gi

- Khong cap JWT token - Subject APPLICATION khong dung JWT
- Khong xac minh credential - APPLICATION khong co credential
- Khong nam trong luong dang nhap cua Identity Service
- Khong luu service nao thuoc app nao - Trust Service quan ly qua cert SAN
- Khong tro thanh runtime dependency cua service khac - resource service song hang tuan bang data da cache

---

## Quyet dinh thiet ke quan trong

### APPLICATION duoc dinh danh bang chung chi, khong phai JWT

Moi loi goi nhan danh APPLICATION deu la noi bo. Cert mTLS cua service luon co san va la bang chung so huu (manh hon bearer token). Trust Service nhung `owner_app_identity_id` vao SAN cua cert, nen tien trinh service tu biet minh thuoc app nao ma khong can goi API hay hardcode config.

Dieu nay giu cho Identity Service hoan toan khong lien quan den APPLICATION subject luc runtime.

### Mo hinh dinh danh "Hon - Xac"

Mot ung dung co hai mat dinh danh:

- **Phan hon** (`identity_id`): KSUID 24 byte do Application Service cap khi dang ky. Bat bien, song hang nam. Dung de so huu du lieu, phan quyen va audit "nhan danh ai".
- **Phan xac** (cert mTLS): do Trust Service cap theo tung tien trinh service. Song ~100 ngay, scale ngang. Dung cho mTLS, routing va audit "tien trinh nao".
- **Ket noi**: Trust nhung `owner_app_identity_id` vao SAN cua cert. Tien trinh service doc cert cua chinh no de biet minh thuoc app nao - khong can bootstrap API.

Cert chi mang du lieu **bat bien** (binding). Trang thai va quyen di qua kenh dong bo rieng.

### Trang thai va quyen qua kenh dong bo

Cert song ~100 ngay ma khong co thu hoi. Viec tat mot app phai co hieu luc trong vai phut. Vi vay trang thai va quyen he thong duoc truyen tai rieng: Kafka push event (chu yeu) va gRPC pull dinh ky (du phong/reconcile), voi do tre chap nhan duoc tinh bang phut.

### Chuan hoa application_code

`application_code` duoc chuan hoa (lowercase + trim) truoc moi thao tac luu hoac tim kiem, tao ra mot dinh danh co the doc duoc oc nguoi, dung trong URL va file config ma khong nham lan chu hoa/thuong.

---

## Chay nhanh

> Thiet ke Application Service da xong (2026-06). Chua bat dau viet code.

```bash
./mvnw clean package -DskipTests
./mvnw spring-boot:run
```

REST: `8085` | gRPC: `9094`

Yeu cau PostgreSQL tai `localhost:5432/application_service` va Trust Service (de bootstrap mTLS).

---

## Kien truc

Application Service theo **Hexagonal Architecture** voi DDD tactical patterns, xay dung tren Spring Boot 4 / Java 25:

```
src/main/java/
+-- api/            <- Input adapter: gRPC (admin + internal subject API)
+-- application/    <- Use case, port, DTO, mapper
+-- domain/         <- Business model thuan: aggregate Application, state machine, permission
+-- integration/    <- Tich hop Trust Service (key, cert, mTLS) - copy tu user-service
+-- infrastructure/ <- JPA persistence, Kafka outbox publisher, gRPC client, security
```

Business logic (chuyen trang thai, kiem tra quyen, invariant) chi nam trong `domain/`. Use case o `application/usecase/` chi orchestrate: load - goi domain - save - publish event.

Repository interface nam o `application/port/out/` (khong phai trong `domain/`).

---

## Tai lieu

| Tai lieu | Mo ta |
|---|---|
| [Tong quan](docs/vn/overview.md) | Vai tro, ranh gioi, vi tri trong Base Platform |
| [Kien truc](docs/vn/architecture.md) | Cau truc tang, DDD pattern, cay thu muc |
| [Dinh danh ung dung](docs/vn/application-identity.md) | Mo hinh hon-xac, cert binding, giai quyet subject |
| [API Reference](docs/vn/api.md) | Dinh nghia gRPC - admin, permission, internal subject |
| [Co che dong bo](docs/vn/sync.md) | Kafka push + pull du phong, outbox, change_sequence |

---

## Cac Service trong Base Platform

| Service | Vai tro |
|---|---|
| `trust-service` | Trust infrastructure - CA, mTLS, JWT signing key |
| `identity-service` | Authentication infrastructure - JWT, refresh token |
| `user-service` | Human Identity Domain Service |
| `agent-service` | Agent Identity Domain Service - Subject BOT, AI_AGENT |
| `application-service` | **Application Identity Domain Service - Subject APPLICATION** |
| `data-service` | Data infrastructure - object storage, permission |
| `notification-service` | Gui thong bao |
| `payment-service` | Thanh toan |

---

## Trang thai du an

Thiet ke Application Service da **hoan thanh** (2026-06). Chua bat dau scaffold code. Buoc tiep theo: scaffold theo layout cua user-service, sau do implement domain + application + infrastructure.

---

## Giay phep

MIT
