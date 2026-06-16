# Application Service - Định danh ứng dụng

[English](../en/application-identity.md)

## Ba khái niệm cần phân biệt rõ

| Khái niệm | Là gì | Định danh | Ví dụ |
|---|---|---|---|
| **SERVICE** | Workload runtime, hạ tầng | Cert mTLS (`service_id`, `shard_id`) do Trust cấp | `social-post-service`, `data-service` |
| **APPLICATION** | Sản phẩm logic, Subject hạng đầu | `identity_id` 24 byte do Application Service cấp | Xime Social, app nha khoa |
| **BOT / AI_AGENT** | Tác nhân tự hành, Subject hạng đầu | `identity_id` 24 byte do Agent Service cấp | Moderation Bot, AI Assistant |

**Luật bất biến: SERVICE không bao giờ là Subject.** Nó không bao giờ xuất hiện với tư cách sở hữu dữ liệu, giữ quyền, hay trong trường `sub` của JWT. Service hành động *thay mặt* Subject.

---

## Mô hình "Hồn - Xác"

Một APPLICATION có hai mặt định danh, giống như "hồn" và "xác":

### Phần hồn - identity_id

- KSUID 24 byte, do Application Service cấp khi đăng ký
- **Bất biến vĩnh viễn** - sống theo vòng đời sản phẩm, có thể hàng năm
- Dùng cho: sở hữu dữ liệu, phân quyền, audit "nhân danh ai"
- Lưu trong bảng `applications`; là khóa shard routing cho dữ liệu thuộc app này

### Phần xác - Cert mTLS

- Do Trust Service cấp cho từng tiến trình service
- **Nhất thời** - sống khoảng 100 ngày, rotate theo deploy
- Dùng cho: bắt tay mTLS, routing service, audit "tiến trình nào"
- Một app có quan hệ 1:N với cert: một app, nhiều tiến trình service

### Kết nối - owner_app_identity_id trong SAN của cert

Bảng `services` của Trust có cột nullable:

```
services.owner_app_identity_id  BYTEA(24)  NULL
```

- `NULL` cho các service Base Platform (không thuộc APPLICATION nào)
- Được đặt bằng `identity_id` của app cho các tiến trình service thuộc Application Layer

Khi Trust cấp cert, nó nhúng giá trị `owner_app_identity_id` vào một entry Subject Alternative Name (SAN) cạnh `service_id`. Giá trị này là opaque với Trust - nó không biết app đó là gì.

Tiến trình service đọc cert của chính nó lúc khởi động để biết mình thuộc app nào. **Không cần gọi bootstrap API. Không cần hardcode app id trong config.**

---

## Tại sao cert chỉ mang dữ liệu bất biến

Cert sống ~100 ngày và không có thu hồi CRL trong thiết kế này. Nếu cert mang trạng thái hoặc quyền:

- Vô hiệu hóa một app nguy hiểm đòi hỏi thu hồi và tái cấp hàng trăm cert - không khả thi vận hành
- Trạng thái `SUSPENDED` sẽ mất 100 ngày mới có hiệu lực trong trường hợp xấu nhất

Vì vậy cert chỉ mang **binding bất biến** (service này thuộc app nào). Mọi trạng thái có thể thay đổi - trạng thái ứng dụng và System Permission - đi qua kênh đồng bộ riêng (Kafka push + pull định kỳ), với độ trễ chấp nhận được tính bằng phút.

```
Cert (Trust):                    "service này thuộc app X" - bất biến
Application Service -> resource: trạng thái app + System Permission - có thể thay đổi, qua kênh sync
```

---

## Giải quyết Subject tại resource service

Resource service xác định Subject đang hoạt động từ mỗi request đến:

```
Request có JWT              -> subject = JWT.sub                       (HUMAN / BOT / AI_AGENT)
Không JWT, cert có app id   -> subject = cert.owner_app_identity_id   (APPLICATION)
Không JWT, cert không app id -> không có subject (chỉ endpoint hạ tầng: health, sync)
```

**JWT thắng khi cả hai cùng có mặt.** Lời gọi muốn hành động nhân danh APPLICATION thì không được kèm JWT - ý định rõ ràng qua cấu trúc request.

Log audit ghi cặp: subject (hồn) + actor (`service_id` từ cert - xác).

Cạnh REST public không có client cert, nên APPLICATION subject không thể đến từ ngoài mTLS mesh.

Cả gRPC lẫn REST nội bộ đều resolve về cùng một model:

```java
AuthenticatedSubject {
    identity_id,
    subject_type,       // APPLICATION
    actor_service_id,   // từ cert
    tenant_id           // null hiện tại
}
```

---

## Thứ tự bootstrap

Thực hiện một lần bởi admin, không tự động hóa:

```
1. Đăng ký app tại Application Service  -> nhận identity_id (24 byte)
2. Đăng ký tiến trình service tại Trust,
   cung cấp owner_app_identity_id
3. Trust cấp cert                        -> app identity_id nhúng vào SAN của cert
4. Tiến trình service khởi động, đọc cert -> tự biết mình thuộc app nào
```

Chống dual-write: danh sách "app X gồm những service nào" do Trust quản lý. Application Service đọc từ Trust khi cần hiển thị thông tin này - không giữ bản sao.

---

## Tại sao APPLICATION không dùng JWT

| Yếu tố | Chi tiết |
|---|---|
| Mọi lời gọi đều là nội bộ | Không cần token bảo ngoài; mTLS đã bắt buộc trên mọi hop nội bộ |
| Cert là bằng chứng sở hữu | Mạnh hơn bearer; không thể ăn cắp và phát lại mà không có private key |
| Không phụ thuộc Identity lúc runtime | Identity Service không có vai trò ở đây; giảm coupling |
| Không lo rò token | JWT có thể bị log, cache, forward; cert không thể rời khỏi TLS context |
| Chi phí thêm gần bằng 0 | mTLS verify đã chạy trên mọi lời gọi nội bộ; phân tích SAN là không đáng kể |

---

## Tại sao không dùng service_id làm owner dữ liệu

Tiến trình service là nhất thời. Chúng được redeploy, đổi tên, scale ngang, hay ngừng hoạt động độc lập. Một `identity_id` phải:

- **Ổn định** trong toàn bộ vòng đời dữ liệu nó sở hữu
- **Có thể hash** thành vị trí shard không bao giờ thay đổi

Service ID (vd `social-post-service-shard-0`) thất bại ở cả hai tiêu chí. Application ID (`identity_id` KSUID 24 byte) đáp ứng cả hai.

---

## Quan hệ với Agent Service

Agent Service quản lý BOT và AI_AGENT subject. Các subject này vẫn dùng JWT vì agent là động (có thể hàng triệu con), không phải lúc nào cũng ở trong mTLS mesh (robot độc lập, bot ngoài), và cần token ngắn hạn vì mục đích an toàn.

APPLICATION subject là ngược lại: số lượng ít (hàng chục đến hàng trăm), luôn ở trong mesh, định danh sống lâu - nên định danh bằng cert là lựa chọn phù hợp.
