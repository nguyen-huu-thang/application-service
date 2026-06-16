# Application Service - Cơ chế đồng bộ

[English](../en/sync.md)

## Tại sao cần đồng bộ

APPLICATION subject không dùng JWT. Resource service (data-service và các service khác) cần biết hai thứ về mỗi APPLICATION lúc xử lý request:

1. **Trạng thái** - app có đang `ACTIVE` không? Từ chối request từ app `SUSPENDED` hoặc `DISABLED`.
2. **System Permission** - app có được phép thực hiện thao tác này không?

Gọi Application Service mỗi request sẽ tạo ra runtime coupling vi phạm nguyên tắc cách ly lỗi của platform. Thay vào đó, resource service duy trì một subject cache cục bộ và cập nhật qua hai kênh: push (Kafka) là cơ chế chủ yếu và pull (gRPC) là dự phòng.

---

## Kênh chính - Kafka Push

### Topic

```
application.subject.changed
```

### Payload event

```json
{
  "event_id": "01HX...",
  "event_type": "APPLICATION_STATUS_CHANGED | APPLICATION_PERMISSION_CHANGED",
  "identity_id": "<hex 48 ký tự, 24 byte>",
  "status": "ACTIVE",
  "permissions": ["DATA_CREATE_OBJECT", "DATA_READ_OBJECT"],
  "state_version": 5,
  "change_sequence": 1042,
  "tenant_id": null,
  "occurred_at": "2026-06-12T10:00:00Z"
}
```

Payload là **snapshot** toàn bộ trạng thái ứng dụng, không phải diff. Consumer thay thế toàn bộ entry cache, nên không cần xử lý event theo thứ tự hay xử lý partial update.

### Trigger

`SubjectChangedEvent` được ghi vào bảng outbox trong cùng transaction database với việc lưu thay đổi trạng thái ứng dụng. Outbox scheduler publish lên Kafka.

### Các event nào tạo Kafka message

| Domain Event | Tạo Kafka? | Lý do |
|---|---|---|
| `ApplicationRegisteredEvent` | Không | App vẫn ở `PENDING_REVIEW`; resource service chưa cần |
| `ApplicationActivatedEvent` | Có | App bắt đầu hoạt động được |
| `ApplicationSuspendedEvent` | Có | Resource service phải bắt đầu từ chối request |
| `ApplicationReactivatedEvent` | Có | Resource service phải bắt đầu chấp nhận request trở lại |
| `ApplicationDisabledEvent` | Có | Resource service từ chối tất cả request mãi mãi |
| `ApplicationRetiredEvent` | Có | App không còn tồn tại |
| `SystemPermissionGrantedEvent` | Có | Tập quyền thay đổi |
| `SystemPermissionRevokedEvent` | Có | Tập quyền thay đổi |

---

## Transactional Outbox

Cách đơn giản là publish trực tiếp lên Kafka sau khi save có race condition: database transaction có thể commit thành công, nhưng Kafka publish có thể thất bại, để lại event bị mất vĩnh viễn.

Pattern Transactional Outbox loại bỏ điều này:

1. **Trong business transaction**: lưu trạng thái ứng dụng + insert event vào `outbox_events` - một thao tác ghi nguyên tử.
2. **Outbox scheduler** (tiến trình riêng): poll `outbox_events WHERE published = false`, publish lên Kafka, đánh dấu `published = true`.

```sql
CREATE TABLE outbox_events (
    id           BIGSERIAL    NOT NULL,
    topic        VARCHAR(128) NOT NULL,
    payload      JSONB        NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL,
    published    BOOLEAN      NOT NULL DEFAULT FALSE,
    published_at TIMESTAMPTZ,
    CONSTRAINT pk_outbox PRIMARY KEY (id)
);
CREATE INDEX idx_outbox_unpublished ON outbox_events (published, created_at)
    WHERE published = FALSE;
```

Scheduler đọc các row chưa publish theo thứ tự tạo ra và publish chúng. Khi Kafka thất bại thì retry; event không bao giờ bị mất miễn là database còn hoạt động.

---

## Kênh dự phòng - gRPC Pull

Resource service gọi `PollChangedApplications` mỗi ~5 phút làm mạng lưới an toàn. Xử lý các trường hợp:

- Kafka consumer bị trễ hoặc ngừng thoáng qua
- Resource service mới khởi động (cần hydrate cache từ đầu)
- Event đã được publish lên Kafka nhưng chưa được consume (consumer restart, rebalance)

### Vòng lặp pull

```
khởi động:
    cursor = đọc last_change_sequence từ local store (0 nếu lần đầu chạy)

mỗi 5 phút (và khi khởi động):
    vòng lặp:
        response = PollChangedApplications(after_sequence=cursor, limit=200)
        với mỗi app trong response.applications:
            upsert vào subject_cache cục bộ
        cursor = response.max_sequence
        nếu không has_more: thoát vòng lặp
    lưu cursor vào local store
```

### Tại sao dùng change_sequence thay vì updated_at

| Vấn đề | updated_at | change_sequence |
|---|---|---|
| Clock skew giữa các node | Có thể - hai node có thể không đồng hồ | Không thể - sequence chỉ tính phía server |
| Ghi không theo thứ tự | Có - concurrent update có thể xếp sai thứ tự | Không bao giờ - BIGSERIAL tăng monotonic |
| Phân trang chính xác | Không - hai event cùng milli giây có thể bị bỏ sót | Có - sequence là unique, phân trang chính xác |
| Reset khi khởi động lại | Không - nhưng vẫn có thể mất nhất quán | Không bao giờ - BIGSERIAL không bao giờ reset |

`change_sequence` là cột `BIGSERIAL` cập nhật cùng với `state_version` mỗi khi trạng thái hoặc quyền thay đổi. Query của Pull API:

```sql
SELECT * FROM applications
WHERE change_sequence > :cursor
ORDER BY change_sequence ASC
LIMIT :limit
```

---

## Mô hình cache trên Resource Service

Resource service được kỳ vọng duy trì hai cấu trúc cache cho mỗi APPLICATION:

```
subject_cache (theo identity_id):
    identity_id    bytes
    subject_type   string  -- "APPLICATION"
    status         string
    state_version  long
    tenant_id      string

subject_permissions (theo identity_id):
    identity_id    bytes
    permission     string
```

Khi có request từ APPLICATION (xác định qua cert SAN):
1. Tra cứu `subject_cache` theo `identity_id`
2. Nếu miss: gọi `GetSubjectInfo`, nạp cache
3. Kiểm tra `status == ACTIVE`; từ chối nếu không
4. Tra cứu `subject_permissions`; kiểm tra quyền cần thiết có trong đó

Trường `state_version` cho phép cache phát hiện entry cũ: nếu push event đến với `state_version` thấp hơn entry đang cache, bỏ qua nó như duplicate.

---

## Đảm bảo độ trễ

| Sự kiện | Thời gian truyền tải dự kiến |
|---|---|
| Thay đổi trạng thái/quyền được commit vào DB | 0 ms |
| Row outbox được ghi (cùng transaction) | 0 ms |
| Kafka event được publish bởi scheduler | < 5 giây (khoảng scheduler) |
| Kafka consumer xử lý event | < 5 giây (consumer lag) |
| **End-to-end: thay đổi hiển thị trong resource service** | **< 30 giây trong điều kiện bình thường** |
| Pull fallback bắt kịp | < 10 phút (poll 5 phút + xử lý) |

Tắt một app (trạng thái `DISABLED`) có hiệu lực trong resource service trong vòng 30 giây. Đây là chấp nhận được vì bối cảnh vận hành (hành động của admin, không phải tự động).
