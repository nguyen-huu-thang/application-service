# Application Service - Tổng quan

[English](../en/overview.md)

## Vai trò

Application Service là **Application Identity Domain Service** của Xime Base Platform. Nhiệm vụ duy nhất của nó là làm nguồn truth cho mọi thứ liên quan đến Subject loại APPLICATION:

- Đăng ký ứng dụng và cấp `identity_id` vĩnh viễn
- Lưu trữ metadata (`application_code`, tên, mô tả)
- Quản lý vòng đời ứng dụng (state machine)
- Cấp và thu hồi System Permission cho APPLICATION subject
- Thông báo cho resource service khi trạng thái hoặc quyền của ứng dụng thay đổi

Service này không có vai trò trong xác thực, quản lý session hay logic nghiệp vụ của bất kỳ ứng dụng nào.

---

## Vị trí trong Base Platform

```
Trust Service        <- CA, cert mTLS, JWT signing key
Identity Service     <- Authentication gateway, cấp JWT cho HUMAN/BOT/AI_AGENT
User Service         <- Registry HUMAN subject
Agent Service        <- Registry BOT, AI_AGENT subject
Application Service  <- Registry APPLICATION subject  (service này)
Data Service         <- Data infrastructure, kiểm tra quyền subject
Notification Service <- Gửi thông báo
Payment Service      <- Thanh toán
```

Application Service là đối tác của User Service và Agent Service. User Service quản lý HUMAN subject, Agent Service quản lý BOT/AI_AGENT, còn Application Service quản lý APPLICATION subject.

---

## Ranh giới

| Application Service LÀM | Application Service KHÔNG LÀM |
|---|---|
| Đăng ký app, cấp `identity_id` 24 byte | Cấp hoặc xác minh JWT token |
| Lưu metadata: code, tên, mô tả | Giữ credential cho app |
| Quản lý vòng đời (state machine + `state_version`) | Lưu binding service-to-app (Trust quản lý qua cert SAN) |
| Cấp và thu hồi System Permission | Tham gia luồng đăng nhập Identity |
| Publish sync event cho resource service | Chứa logic nghiệp vụ của ứng dụng cụ thể |
| Expose Pull API để reconcile dự phòng | Trở thành runtime dependency của service khác |

---

## Tại sao APPLICATION không dùng JWT

Mọi lời gọi nhân danh APPLICATION đều phát sinh từ tiến trình service chạy bên trong mTLS mesh. Cert mTLS luôn có sẵn và là bằng chứng sở hữu (mạnh hơn bearer token). Trust Service nhúng `owner_app_identity_id` vào SAN của cert, nên không cần bootstrap API hay hardcode config.

Nếu dùng JWT sẽ khiến APPLICATION subject phụ thuộc vào Identity Service lúc runtime - một coupling không được thiết kế sẵn.

Chi tiết: [Định danh ứng dụng](application-identity.md).

---

## Các loại Subject trên platform

```
SubjectType     Owner Service          Cơ chế xác thực       JWT?
HUMAN        -> user-service           credential + Identity  có
BOT          -> agent-service          API key + Identity     có
AI_AGENT     -> agent-service          API key + Identity     có
APPLICATION  -> application-service    cert (Trust) + sync    không
(SERVICE     -> trust-service          cert mTLS              không bao giờ là subject)
```

SERVICE không bao giờ là Subject và không bao giờ xuất hiện với tư cách sở hữu dữ liệu hay giữ quyền.

---

## Cổng và cơ sở dữ liệu

| Thông tin | Giá trị |
|---|---|
| HTTP port | 8085 |
| gRPC port | 9094 |
| Database | `application_service` |
| Package gốc | `vn.xime.application` |
| Main class | `ApplicationServiceApplication` |

---

## Quan hệ với Trust Service

Application Service chính nó là một Base Platform service bình thường: nó bootstrap cert mTLS từ Trust Service (cert của nó KHÔNG mang `owner_app_identity_id`).

Khái niệm `owner_app_identity_id` chỉ áp dụng cho các tiến trình service thuộc Application Layer. Bảng `services` của Trust có cột nullable `owner_app_identity_id BYTEA(24)`. Application Service là nguồn tự cường cung cấp giá trị của cột này.

---

## Quan hệ với Resource Service

Resource service (data-service và các service tương lai) cache thông tin subject và System Permission của APPLICATION subject. Application Service đẩy cập nhật qua Kafka và expose Pull API để reconcile. Resource service có thể hoạt động trong thời gian dài mà không cần liên hệ Application Service - chỉ cần dữ liệu tươi để kiểm tra quyền.

Chi tiết: [Cơ chế đồng bộ](sync.md).
