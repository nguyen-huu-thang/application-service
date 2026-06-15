# Application Service - API Reference

[English](../en/api.md)

Tat ca API deu la gRPC qua mTLS. Khong co REST endpoint public cho nghiep vu (chi co health/actuator tren HTTP 8085).

---

## File Proto

| File | Package | Muc dich |
|---|---|---|
| `external/application/application_admin.proto` | `vn.xime.application.external` | CRUD va vong doi ung dung (admin) |
| `external/permission/application_permission.proto` | `vn.xime.application.external` | Quan ly System Permission (admin) |
| `internal/subject/application_subject.proto` | `vn.xime.application.internal` | Dong bo subject cho resource service |

---

## External - ApplicationAdminService

Dung boi cong cu admin de dang ky va quan ly ung dung.

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

Tao ung dung moi voi trang thai `PENDING_REVIEW` va cap `identity_id` vinh vien.

```proto
message RegisterApplicationRequest {
    string application_code = 1;  // Base62 chu thuong, 2-64 ky tu, vd "xime-social"
    string name             = 2;  // bat buoc, toi da 255 ky tu
    string description      = 3;  // tuy chon, toi da 2000 ky tu
}
message RegisterApplicationResponse {
    bytes  identity_id      = 1;  // KSUID 24 byte
    string application_code = 2;  // gia tri da duoc chuan hoa luu vao DB
}
```

`application_code` duoc chuan hoa (lowercase + trim) truoc khi luu. Code trung tra ve `ALREADY_EXISTS`.

### Cac phuong thuc vong doi

Tat ca deu nhan `ApplicationIdRequest`:

```proto
message ApplicationIdRequest {
    bytes identity_id = 1;  // KSUID 24 byte
}
```

| RPC | Trang thai hop le | Trang thai ket qua |
|---|---|---|
| `ActivateApplication` | `PENDING_REVIEW` | `ACTIVE` |
| `SuspendApplication` | `ACTIVE` | `SUSPENDED` |
| `ReactivateApplication` | `SUSPENDED` | `ACTIVE` |
| `DisableApplication` | `ACTIVE` hoac `SUSPENDED` | `DISABLED` |
| `RetireApplication` | `DISABLED` | `RETIRED` |

Chuyen trang thai khong hop le tra ve `FAILED_PRECONDITION`.

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
    repeated string permissions = 8;  // danh sach PermissionCode dang duoc cap
    int64  created_at        = 9;   // epoch millis
    int64  updated_at        = 10;
}

message ListApplicationsRequest {
    string status_filter = 1;  // tuy chon, loc theo trang thai
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

Dung boi cong cu admin de quan ly System Permission cho APPLICATION subject.

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

### Hanh vi

- `GrantSystemPermission`: them quyen vao tap quyen cua app. Tra ve `ALREADY_EXISTS` neu da cap.
- `RevokeSystemPermission`: xoa quyen. Tra ve `NOT_FOUND` neu chua duoc cap.
- Ca hai deu tang `state_version` va `change_sequence`, sau do publish `SubjectChangedEvent`.

### Cac gia tri PermissionCode

| Code | Y nghia |
|---|---|
| `DATA_CREATE_OBJECT` | Tao object trong data-service |
| `DATA_READ_OBJECT` | Doc object trong data-service |
| `DATA_UPDATE_OBJECT` | Cap nhat object trong data-service |
| `DATA_DELETE_OBJECT` | Xoa object trong data-service |
| `DATA_CREATE_SCHEMA` | Tao schema trong data-service |
| `DATA_READ_SCHEMA` | Doc schema trong data-service |

Permission code moi se them vao khi co resource service moi gia nhap platform.

---

## Internal - ApplicationSubjectService

Dung boi resource service (data-service...) de:
1. Lay thong tin subject cu the khi miss cache
2. Pull batch cac app thay doi de reconcile dinh ky

Xac thuc: gRPC + mTLS. Caller phai co client certificate hop le do Trust cap.

```proto
service ApplicationSubjectService {
    rpc GetSubjectInfo          (GetSubjectInfoRequest)  returns (SubjectInfoResponse);
    rpc PollChangedApplications (PollChangedRequest)     returns (PollChangedResponse);
}
```

### GetSubjectInfo

Tra cuu truc tiep - dung khi resource service bi miss cache voi mot `identity_id` cu the.

```proto
message GetSubjectInfoRequest {
    bytes identity_id = 1;  // KSUID 24 byte
}
message SubjectInfoResponse {
    bytes  identity_id       = 1;
    string subject_type      = 2;  // luon la "APPLICATION"
    string status            = 3;  // PENDING_REVIEW | ACTIVE | SUSPENDED | DISABLED | RETIRED
    int64  state_version     = 4;
    int64  change_sequence   = 5;
    repeated string permissions = 6;
    string tenant_id         = 7;  // nullable
}
```

Tra ve `NOT_FOUND` neu khong co ung dung nao voi `identity_id` do.

### PollChangedApplications

Pull theo lo - dung de reconcile khi khoi dong va dong bo du phong dinh ky.

```proto
message PollChangedRequest {
    int64 after_sequence = 1;  // 0 = lay tat ca, hoac cursor tu lan poll truoc
    int32 limit          = 2;  // toi da 200 moi lan goi
}
message PollChangedResponse {
    repeated SubjectInfoResponse applications = 1;
    int64 max_sequence = 2;  // change_sequence lon nhat trong response, dung lam cursor tiep theo
    bool  has_more     = 3;  // true neu con trang tiep theo
}
```

**Pattern phan trang:**

```
cursor = 0
vong lap:
    response = PollChangedApplications(after_sequence=cursor, limit=200)
    xu ly response.applications
    cursor = response.max_sequence
    neu khong has_more: thoat vong lap
luu cursor lam last_change_sequence cho lan poll tiep theo
```

`change_sequence` tang monotonic va khong bao gio reset, nen an toan dung lam cursor qua cac lan khoi dong lai va ngat mang.

---

## Ma loi

Application Service dung dai ma `030000-039999`:

| Dai ma | Visibility | Vi du |
|---|---|---|
| 030000-033999 | Private (noi bo service) | Loi implementation noi bo |
| 034000-036999 | System (service khac doc duoc) | `ApplicationNotFoundException` |
| 037000-039999 | Public (admin client doc duoc) | `DuplicateApplicationCodeException` |

gRPC error mang metadata header `xime-error` chua `{errorKey, code, message}`. REST error dung body `{errorKey, code, message}`.
