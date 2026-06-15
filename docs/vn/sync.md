# Application Service - Co che dong bo

[English](../en/sync.md)

## Tai sao can dong bo

APPLICATION subject khong dung JWT. Resource service (data-service va cac service khac) can biet hai thu ve moi APPLICATION luc xu ly request:

1. **Trang thai** - app co dang `ACTIVE` khong? Tu choi request tu app `SUSPENDED` hoac `DISABLED`.
2. **System Permission** - app co duoc phep thuc hien thao tac nay khong?

Goi Application Service moi request se tao ra runtime coupling vi pham nguyen tac cach ly loi cua platform. Thay vao do, resource service duy tri mot subject cache cuc bo va cap nhat qua hai kenh: push (Kafka) la co che chu yeu va pull (gRPC) la du phong.

---

## Kenh chinh - Kafka Push

### Topic

```
application.subject.changed
```

### Payload event

```json
{
  "event_id": "01HX...",
  "event_type": "APPLICATION_STATUS_CHANGED | APPLICATION_PERMISSION_CHANGED",
  "identity_id": "<hex 48 ky tu, 24 byte>",
  "status": "ACTIVE",
  "permissions": ["DATA_CREATE_OBJECT", "DATA_READ_OBJECT"],
  "state_version": 5,
  "change_sequence": 1042,
  "tenant_id": null,
  "occurred_at": "2026-06-12T10:00:00Z"
}
```

Payload la **snapshot** toan bo trang thai ung dung, khong phai diff. Consumer thay the toan bo entry cache, nen khong can xu ly event theo thu tu hay xu ly partial update.

### Trigger

`SubjectChangedEvent` duoc ghi vao bang outbox trong cung transaction database voi viec luu thay doi trang thai ung dung. Outbox scheduler publish len Kafka.

### Cac event nao tao Kafka message

| Domain Event | Tao Kafka? | Ly do |
|---|---|---|
| `ApplicationRegisteredEvent` | Khong | App van o `PENDING_REVIEW`; resource service chua can |
| `ApplicationActivatedEvent` | Co | App bat dau hoat dong duoc |
| `ApplicationSuspendedEvent` | Co | Resource service phai bat dau tu choi request |
| `ApplicationReactivatedEvent` | Co | Resource service phai bat dau chap nhan request tro lai |
| `ApplicationDisabledEvent` | Co | Resource service tu choi tat ca request mai mai |
| `ApplicationRetiredEvent` | Co | App khong con ton tai |
| `SystemPermissionGrantedEvent` | Co | Tap quyen thay doi |
| `SystemPermissionRevokedEvent` | Co | Tap quyen thay doi |

---

## Transactional Outbox

Cach don gian la publish truc tiep len Kafka sau khi save co race condition: database transaction co the commit thanh cong, nhung Kafka publish co the that bai, de lai event bi mat vinh vien.

Pattern Transactional Outbox loai bo dieu nay:

1. **Trong business transaction**: luu trang thai ung dung + insert event vao `outbox_events` - mot thao tac ghi nguyen tu.
2. **Outbox scheduler** (tien trinh rieng): poll `outbox_events WHERE published = false`, publish len Kafka, danh dau `published = true`.

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

Scheduler doc cac row chua publish theo thu tu tao ra va publish chung. Khi Kafka that bai thi retry; event khong bao gio bi mat mien la database con hoat dong.

---

## Kenh du phong - gRPC Pull

Resource service goi `PollChangedApplications` moi ~5 phut lam mang luoi an toan. Xu ly cac truong hop:

- Kafka consumer bi tre hoac ngung thoang qua
- Resource service moi khoi dong (can hydrate cache tu dau)
- Event da duoc publish len Kafka nhung chua duoc consume (consumer restart, rebalance)

### Vong lap pull

```
khoi dong:
    cursor = doc last_change_sequence tu local store (0 neu lan dau chay)

moi 5 phut (va khi khoi dong):
    vong lap:
        response = PollChangedApplications(after_sequence=cursor, limit=200)
        voi moi app trong response.applications:
            upsert vao subject_cache cuc bo
        cursor = response.max_sequence
        neu khong has_more: thoat vong lap
    luu cursor vao local store
```

### Tai sao dung change_sequence thay vi updated_at

| Van de | updated_at | change_sequence |
|---|---|---|
| Clock skew giua cac node | Co the - hai node co the khong dong ho | Khong the - sequence chi tinh phia server |
| Ghi khong theo thu tu | Co - concurrent update co the xep sai thu tu | Khong bao gio - BIGSERIAL tang monotonic |
| Phan trang chinh xac | Khong - hai event cung milli giay co the bi bo sot | Co - sequence la unique, phan trang chinh xac |
| Reset khi khoi dong lai | Khong - nhung van co the mat nhat quan | Khong bao gio - BIGSERIAL khong bao gio reset |

`change_sequence` la cot `BIGSERIAL` cap nhat cung voi `state_version` moi khi trang thai hoac quyen thay doi. Query cua Pull API:

```sql
SELECT * FROM applications
WHERE change_sequence > :cursor
ORDER BY change_sequence ASC
LIMIT :limit
```

---

## Mo hinh cache tren Resource Service

Resource service duoc ky vong duy tri hai cau truc cache cho moi APPLICATION:

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

Khi co request tu APPLICATION (xac dinh qua cert SAN):
1. Tra cuu `subject_cache` theo `identity_id`
2. Neu miss: goi `GetSubjectInfo`, nap cache
3. Kiem tra `status == ACTIVE`; tu choi neu khong
4. Tra cuu `subject_permissions`; kiem tra quyen can thiet co trong do

Truong `state_version` cho phep cache phat hien entry cu: neu push event den voi `state_version` thap hon entry dang cache, bo qua no nhu duplicate.

---

## Dam bao do tre

| Su kien | Thoi gian truyen tai du kien |
|---|---|
| Thay doi trang thai/quyen duoc commit vao DB | 0 ms |
| Row outbox duoc ghi (cung transaction) | 0 ms |
| Kafka event duoc publish boi scheduler | < 5 giay (khoang scheduler) |
| Kafka consumer xu ly event | < 5 giay (consumer lag) |
| **End-to-end: thay doi hien thi trong resource service** | **< 30 giay trong dieu kien binh thuong** |
| Pull fallback bat kip | < 10 phut (poll 5 phut + xu ly) |

Tat mot app (trang thai `DISABLED`) co hieu luc trong resource service trong vong 30 giay. Day la chap nhan duoc vi boi canh van hanh (hanh dong cua admin, khong phai tu dong).
