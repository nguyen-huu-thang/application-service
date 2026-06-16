# Application Service

[English](README.md) | **Tiếng Việt**

> Application Identity Domain Service cho Xime Base Platform - registry, metadata, quản lý vòng đời và phân quyền hệ thống cho các Subject loại APPLICATION.

---

Application Service là **tầng định danh ứng dụng** của Xime Base Platform. Đây là nguồn tin cậy cho mọi Subject loại APPLICATION - một thực thể cấp sản phẩm như Xime Social, Xime Chat hay ứng dụng phòng khám nha khoa. Service này không cấp token và không nằm trong bất kỳ luồng xác thực nào; ứng dụng được định danh bằng chứng chỉ mTLS, không phải JWT.

```
Công cụ Admin
  | RegisterApplication (gRPC + mTLS)
  v
Application Service    <- registry, vòng đời, quyền hệ thống
  | SubjectChangedEvent (Kafka + Transactional Outbox)
  v
Resource Services      <- cache thông tin subject, kiểm tra quyền
(data-service, ...)
  ^ PollChangedApplications (gRPC + mTLS) -- pull dự phòng
```

---

## Application Service làm gì

**Registry ứng dụng**
- Cấp `identity_id` 24 byte (KSUID) khi đăng ký ứng dụng mới
- Lưu `application_code` (Base62 chữ thường, unique toàn platform), tên và mô tả
- Chuẩn hóa `application_code` (lowercase + trim) trước mọi thao tác lưu hoặc tìm kiếm

**Quản lý vòng đời**
- Duy trì vòng đời: `PENDING_REVIEW` - `ACTIVE` - `SUSPENDED` / `DISABLED` - `RETIRED`
- Áp dụng state machine; từ chối các chuyển trạng thái không hợp lệ ngay tại tầng domain
- Tăng `state_version` và `change_sequence` mỗi khi status hoặc quyền thay đổi

**Quyền hệ thống**
- Cấp và thu hồi System Permission (`DATA_CREATE_OBJECT`, `DATA_READ_OBJECT`...) cho subject APPLICATION
- Là nguồn truth; resource service giữ bản cache và đồng bộ định kỳ

**Đồng bộ Subject**
- Publish `SubjectChangedEvent` lên Kafka (qua Transactional Outbox) mỗi khi state hoặc quyền thay đổi
- Expose Pull API (`PollChangedApplications`) để resource service có thể reconcile khi khởi động hoặc bị trễ

## Application Service KHÔNG làm gì

- Không cấp JWT token - Subject APPLICATION không dùng JWT
- Không xác minh credential - APPLICATION không có credential
- Không nằm trong luồng đăng nhập của Identity Service
- Không lưu service nào thuộc app nào - Trust Service quản lý qua cert SAN
- Không trở thành runtime dependency của service khác - resource service sống hàng tuần bằng data đã cache

---

## Quyết định thiết kế quan trọng

### APPLICATION được định danh bằng chứng chỉ, không phải JWT

Mọi lời gọi nhân danh APPLICATION đều là nội bộ. Cert mTLS của service luôn có sẵn và là bằng chứng sở hữu (mạnh hơn bearer token). Trust Service nhúng `owner_app_identity_id` vào SAN của cert, nên tiến trình service tự biết mình thuộc app nào mà không cần gọi API hay hardcode config.

Điều này giữ cho Identity Service hoàn toàn không liên quan đến APPLICATION subject lúc runtime.

### Mô hình định danh "Hồn - Xác"

Một ứng dụng có hai mặt định danh:

- **Phần hồn** (`identity_id`): KSUID 24 byte do Application Service cấp khi đăng ký. Bất biến, sống hàng năm. Dùng để sở hữu dữ liệu, phân quyền và audit "nhân danh ai".
- **Phần xác** (cert mTLS): do Trust Service cấp theo từng tiến trình service. Sống ~100 ngày, scale ngang. Dùng cho mTLS, routing và audit "tiến trình nào".
- **Kết nối**: Trust nhúng `owner_app_identity_id` vào SAN của cert. Tiến trình service đọc cert của chính nó để biết mình thuộc app nào - không cần bootstrap API.

Cert chỉ mang dữ liệu **bất biến** (binding). Trạng thái và quyền đi qua kênh đồng bộ riêng.

### Trạng thái và quyền qua kênh đồng bộ

Cert sống ~100 ngày mà không có thu hồi. Việc tắt một app phải có hiệu lực trong vài phút. Vì vậy trạng thái và quyền hệ thống được truyền tải riêng: Kafka push event (chủ yếu) và gRPC pull định kỳ (dự phòng/reconcile), với độ trễ chấp nhận được tính bằng phút.

### Chuẩn hóa application_code

`application_code` được chuẩn hóa (lowercase + trim) trước mọi thao tác lưu hoặc tìm kiếm, tạo ra một định danh có thể đọc được bởi người, dùng trong URL và file config mà không nhầm lẫn chữ hoa/thường.

---

## Chạy nhanh

> Thiết kế Application Service đã xong (2026-06). Chưa bắt đầu viết code.

```bash
./mvnw clean package -DskipTests
./mvnw spring-boot:run
```

REST: `8085` | gRPC: `9094`

Yêu cầu PostgreSQL tại `localhost:5432/application_service` và Trust Service (để bootstrap mTLS).

---

## Kiến trúc

Application Service theo **Hexagonal Architecture** với DDD tactical patterns, xây dựng trên Spring Boot 4 / Java 25:

```
src/main/java/
+-- api/            <- Input adapter: gRPC (admin + internal subject API)
+-- application/    <- Use case, port, DTO, mapper
+-- domain/         <- Business model thuần: aggregate Application, state machine, permission
+-- integration/    <- Tích hợp Trust Service (key, cert, mTLS) - copy từ user-service
+-- infrastructure/ <- JPA persistence, Kafka outbox publisher, gRPC client, security
```

Business logic (chuyển trạng thái, kiểm tra quyền, invariant) chỉ nằm trong `domain/`. Use case ở `application/usecase/` chỉ orchestrate: load - gọi domain - save - publish event.

Repository interface nằm ở `application/port/out/` (không phải trong `domain/`).

---

## Tài liệu

| Tài liệu | Mô tả |
|---|---|
| [Tổng quan](docs/vn/overview.md) | Vai trò, ranh giới, vị trí trong Base Platform |
| [Kiến trúc](docs/vn/architecture.md) | Cấu trúc tầng, DDD pattern, cây thư mục |
| [Định danh ứng dụng](docs/vn/application-identity.md) | Mô hình hồn-xác, cert binding, giải quyết subject |
| [API Reference](docs/vn/api.md) | Định nghĩa gRPC - admin, permission, internal subject |
| [Cơ chế đồng bộ](docs/vn/sync.md) | Kafka push + pull dự phòng, outbox, change_sequence |

---

## Các Service trong Base Platform

| Service | Vai trò |
|---|---|
| `trust-service` | Trust infrastructure - CA, mTLS, JWT signing key |
| `identity-service` | Authentication infrastructure - JWT, refresh token |
| `user-service` | Human Identity Domain Service |
| `agent-service` | Agent Identity Domain Service - Subject BOT, AI_AGENT |
| `application-service` | **Application Identity Domain Service - Subject APPLICATION** |
| `data-service` | Data infrastructure - object storage, permission |
| `notification-service` | Gửi thông báo |
| `payment-service` | Thanh toán |

---

## Trạng thái dự án

Thiết kế Application Service đã **hoàn thành** (2026-06). Chưa bắt đầu scaffold code. Bước tiếp theo: scaffold theo layout của user-service, sau đó implement domain + application + infrastructure.

---

## Giấy phép

MIT
