# Application Service - API Reference

[English](../en/api.md)

Tất cả API đều là gRPC qua mTLS. Không có REST endpoint public cho nghiệp vụ (chỉ có health/actuator trên HTTP 8085).

---

## File Proto

| File | Package | Mục đích |
|---|---|---|
| `external/application/application_admin.proto` | `vn.xime.application.external` | CRUD và vòng đời ứng dụng (admin) |
| `external/permission/application_permission.proto` | `vn.xime.application.external` | Quản lý System Permission (admin) |
| `internal/subject/application_subject.proto` | `vn.xime.application.internal` | Đồng bộ subject cho resource service |

---

## External - ApplicationAdminService

Dùng bởi công cụ admin để đăng ký và quản lý ứng dụng.

```proto
service ApplicationAdminService {
    rpc RegisterApplication   (RegisterApplicationRequest)   returns (RegisterApplicationResponse);
    rpc ActivateApplication   (ApplicationIdRequest)         returns (google.protobuf.Empty);
    rpc SuspendApplication    (ApplicationIdRequest)         returns (google.protobuf.Empty);
    rpc ReactivateApplication (ApplicationIdRequest)         returns (google.protobuf.Empty);
    rpc DisableApplication    (ApplicationIdRequest)         returns (google.protobuf.Empty);
    rpc RetireApplication     (ApplicationIdRequest)         returns (google.protobuf.Empty);
    rpc GetApplication        (ApplicationIdRequest)         returns (ApplicationResponse);
    rpc ListApplications      (ListApplicationsRequest)      returns (ListApplicationsResponse);
}
```

### RegisterApplication

Tạo ứng dụng mới với trạng thái `PENDING_REVIEW` và cấp `identity_id` vĩnh viễn.

```proto
message RegisterApplicationRequest {
    string application_code = 1;  // Base62 chữ thường, 2-64 ký tự, vd "xime-social"
    string name             = 2;  // bắt buộc, tối đa 255 ký tự
    string description      = 3;  // tùy chọn, tối đa 2000 ký tự
}
message RegisterApplicationResponse {
    bytes  identity_id      = 1;  // KSUID 24 byte
    string application_code = 2;  // giá trị đã được chuẩn hóa lưu vào DB
}
```

`application_code` được chuẩn hóa (lowercase + trim) trước khi lưu. Code trùng trả về `ALREADY_EXISTS`.

### Các phương thức vòng đời

Tất cả đều nhận `ApplicationIdRequest`:

```proto
message ApplicationIdRequest {
    bytes identity_id = 1;  // KSUID 24 byte
}
```

| RPC | Trạng thái hợp lệ | Trạng thái kết quả |
|---|---|---|
| `ActivateApplication` | `PENDING_REVIEW` | `ACTIVE` |
| `SuspendApplication` | `ACTIVE` | `SUSPENDED` |
| `ReactivateApplication` | `SUSPENDED` | `ACTIVE` |
| `DisableApplication` | `ACTIVE` hoặc `SUSPENDED` | `DISABLED` |
| `RetireApplication` | `DISABLED` | `RETIRED` |

Chuyển trạng thái không hợp lệ trả về `FAILED_PRECONDITION`.

### GetApplication / ListApplications

```proto
message ApplicationResponse {
    bytes  identity_id       = 1;
    string application_code  = 2;
    string name              = 3;
    string description       = 4;
    string status            = 5;  // PENDING_REVIEW | ACTIVE | SUSPENDED | DISABLED | RETIRED
    int64  state_version     = 6;
    int64  change_sequence   = 7;
    repeated string permissions = 8;  // danh sách PermissionCode đang được cấp
    int64  created_at        = 9;   // epoch millis
    int64  updated_at        = 10;
}

message ListApplicationsRequest {
    string status_filter = 1;  // tùy chọn, lọc theo trạng thái
    int32  page          = 2;
    int32  size          = 3;
}
message ListApplicationsResponse {
    repeated ApplicationResponse applications = 1;
    int64 total = 2;
}
```

---

## External - ApplicationPermissionService

Dùng bởi công cụ admin để quản lý System Permission cho APPLICATION subject.

```proto
service ApplicationPermissionService {
    rpc GrantSystemPermission  (PermissionRequest) returns (google.protobuf.Empty);
    rpc RevokeSystemPermission (PermissionRequest) returns (google.protobuf.Empty);
}
message PermissionRequest {
    bytes  app_identity_id = 1;  // KSUID 24 byte
    string permission      = 2;  // PermissionCode, vd "DATA_CREATE_OBJECT"
}
```

### Hành vi

- `GrantSystemPermission`: thêm quyền vào tập quyền của app. Trả về `ALREADY_EXISTS` nếu đã cấp.
- `RevokeSystemPermission`: xóa quyền. Trả về `NOT_FOUND` nếu chưa được cấp.
- Cả hai đều tăng `state_version` và `change_sequence`, sau đó publish `SubjectChangedEvent`.

### Các giá trị PermissionCode

| Code | Ý nghĩa |
|---|---|
| `DATA_CREATE_OBJECT` | Tạo object trong data-service |
| `DATA_READ_OBJECT` | Đọc object trong data-service |
| `DATA_UPDATE_OBJECT` | Cập nhật object trong data-service |
| `DATA_DELETE_OBJECT` | Xóa object trong data-service |
| `DATA_CREATE_SCHEMA` | Tạo schema trong data-service |
| `DATA_READ_SCHEMA` | Đọc schema trong data-service |

Permission code mới sẽ thêm vào khi có resource service mới gia nhập platform.

---

## Internal - ApplicationSubjectService

Dùng bởi resource service (data-service...) để:
1. Lấy thông tin subject cụ thể khi miss cache
2. Pull batch các app thay đổi để reconcile định kỳ

Xác thực: gRPC + mTLS. Caller phải có client certificate hợp lệ do Trust cấp.

```proto
service ApplicationSubjectService {
    rpc GetSubjectInfo          (GetSubjectInfoRequest)  returns (SubjectInfoResponse);
    rpc PollChangedApplications (PollChangedRequest)     returns (PollChangedResponse);
}
```

### GetSubjectInfo

Tra cứu trực tiếp - dùng khi resource service bị miss cache với một `identity_id` cụ thể.

```proto
message GetSubjectInfoRequest {
    bytes identity_id = 1;  // KSUID 24 byte
}
message SubjectInfoResponse {
    bytes  identity_id       = 1;
    string subject_type      = 2;  // luôn là "APPLICATION"
    string status            = 3;  // PENDING_REVIEW | ACTIVE | SUSPENDED | DISABLED | RETIRED
    int64  state_version     = 4;
    int64  change_sequence   = 5;
    repeated string permissions = 6;
    string tenant_id         = 7;  // nullable
}
```

Trả về `NOT_FOUND` nếu không có ứng dụng nào với `identity_id` đó.

### PollChangedApplications

Pull theo lô - dùng để reconcile khi khởi động và đồng bộ dự phòng định kỳ.

```proto
message PollChangedRequest {
    int64 after_sequence = 1;  // 0 = lấy tất cả, hoặc cursor từ lần poll trước
    int32 limit          = 2;  // tối đa 200 mỗi lần gọi
}
message PollChangedResponse {
    repeated SubjectInfoResponse applications = 1;
    int64 max_sequence = 2;  // change_sequence lớn nhất trong response, dùng làm cursor tiếp theo
    bool  has_more     = 3;  // true nếu còn trang tiếp theo
}
```

**Pattern phân trang:**

```
cursor = 0
vòng lặp:
    response = PollChangedApplications(after_sequence=cursor, limit=200)
    xử lý response.applications
    cursor = response.max_sequence
    nếu không has_more: thoát vòng lặp
lưu cursor làm last_change_sequence cho lần poll tiếp theo
```

`change_sequence` tăng monotonic và không bao giờ reset, nên an toàn dùng làm cursor qua các lần khởi động lại và ngắt mạng.

---

## Mã lỗi

Application Service dùng dải mã `030000-039999`:

| Dải mã | Visibility | Ví dụ |
|---|---|---|
| 030000-033999 | Private (nội bộ service) | Lỗi implementation nội bộ |
| 034000-036999 | System (service khác đọc được) | `ApplicationNotFoundException` |
| 037000-039999 | Public (admin client đọc được) | `DuplicateApplicationCodeException` |

gRPC error mang metadata header `xime-error` chứa `{errorKey, code, message}`. REST error dùng body `{errorKey, code, message}`.
