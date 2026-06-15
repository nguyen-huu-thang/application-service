# Application Service - API Reference

[Tieng Viet](../vn/api.md)

All APIs are gRPC over mTLS. There are no public REST endpoints for business operations (health/actuator endpoints are on HTTP 8085).

---

## Proto Files

| File | Package | Purpose |
|---|---|---|
| `external/application/application_admin.proto` | `vn.xime.application.external` | Admin CRUD and lifecycle |
| `external/permission/application_permission.proto` | `vn.xime.application.external` | System permission management |
| `internal/subject/application_subject.proto` | `vn.xime.application.internal` | Subject sync for resource services |

---

## External - ApplicationAdminService

Used by admin tooling to register and manage applications.

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

Creates a new application in `PENDING_REVIEW` status and issues a permanent `identity_id`.

```proto
message RegisterApplicationRequest {
    string application_code = 1;  // Base62 lowercase, 2-64 chars, e.g. "xime-social"
    string name             = 2;  // required, max 255 chars
    string description      = 3;  // optional, max 2000 chars
}
message RegisterApplicationResponse {
    bytes  identity_id      = 1;  // 24-byte KSUID
    string application_code = 2;  // normalized value stored
}
```

`application_code` is normalized (lowercase + trim) before storage. Duplicate codes return `ALREADY_EXISTS`.

### Lifecycle Methods

All lifecycle methods take an `ApplicationIdRequest`:

```proto
message ApplicationIdRequest {
    bytes identity_id = 1;  // 24-byte KSUID
}
```

| RPC | Allowed from state | Result state |
|---|---|---|
| `ActivateApplication` | `PENDING_REVIEW` | `ACTIVE` |
| `SuspendApplication` | `ACTIVE` | `SUSPENDED` |
| `ReactivateApplication` | `SUSPENDED` | `ACTIVE` |
| `DisableApplication` | `ACTIVE` or `SUSPENDED` | `DISABLED` |
| `RetireApplication` | `DISABLED` | `RETIRED` |

Invalid transitions return `FAILED_PRECONDITION`.

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
    repeated string permissions = 8;  // PermissionCode values currently granted
    int64  created_at        = 9;   // epoch millis
    int64  updated_at        = 10;
}

message ListApplicationsRequest {
    string status_filter = 1;  // optional, filter by status
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

Used by admin tooling to manage System Permissions for APPLICATION subjects.

```proto
service ApplicationPermissionService {
    rpc GrantSystemPermission  (PermissionRequest) returns (google.protobuf.Empty);
    rpc RevokeSystemPermission (PermissionRequest) returns (google.protobuf.Empty);
}
message PermissionRequest {
    bytes  app_identity_id = 1;  // 24-byte KSUID
    string permission      = 2;  // PermissionCode, e.g. "DATA_CREATE_OBJECT"
}
```

### Behavior

- `GrantSystemPermission`: adds the permission to the application's permission set. Returns `ALREADY_EXISTS` if already granted.
- `RevokeSystemPermission`: removes the permission. Returns `NOT_FOUND` if not granted.
- Both operations increment `state_version` and `change_sequence`, then publish a `SubjectChangedEvent`.

### PermissionCode Values

| Code | Meaning |
|---|---|
| `DATA_CREATE_OBJECT` | Create objects in data-service |
| `DATA_READ_OBJECT` | Read objects in data-service |
| `DATA_UPDATE_OBJECT` | Update objects in data-service |
| `DATA_DELETE_OBJECT` | Delete objects in data-service |
| `DATA_CREATE_SCHEMA` | Create schemas in data-service |
| `DATA_READ_SCHEMA` | Read schemas in data-service |

New permission codes are added as new resource services join the platform.

---

## Internal - ApplicationSubjectService

Used by resource services (data-service, etc.) to:
1. Fetch a single application's subject info on cache miss
2. Pull batches of changed applications for periodic reconciliation

Authentication: gRPC + mTLS. Caller must present a valid client certificate issued by Trust.

```proto
service ApplicationSubjectService {
    rpc GetSubjectInfo          (GetSubjectInfoRequest)  returns (SubjectInfoResponse);
    rpc PollChangedApplications (PollChangedRequest)     returns (PollChangedResponse);
}
```

### GetSubjectInfo

Point lookup - used when a resource service has a cache miss for a specific `identity_id`.

```proto
message GetSubjectInfoRequest {
    bytes identity_id = 1;  // 24-byte KSUID
}
message SubjectInfoResponse {
    bytes  identity_id       = 1;
    string subject_type      = 2;  // always "APPLICATION"
    string status            = 3;  // PENDING_REVIEW | ACTIVE | SUSPENDED | DISABLED | RETIRED
    int64  state_version     = 4;
    int64  change_sequence   = 5;
    repeated string permissions = 6;
    string tenant_id         = 7;  // nullable
}
```

Returns `NOT_FOUND` if no application exists with that `identity_id`.

### PollChangedApplications

Batch pull - used for startup reconciliation and periodic fallback sync.

```proto
message PollChangedRequest {
    int64 after_sequence = 1;  // 0 = fetch all, or cursor from last poll
    int32 limit          = 2;  // max 200 per call
}
message PollChangedResponse {
    repeated SubjectInfoResponse applications = 1;
    int64 max_sequence = 2;  // highest change_sequence in this response, use as cursor
    bool  has_more     = 3;  // true if more pages exist
}
```

**Pagination pattern:**

```
cursor = 0
loop:
    response = PollChangedApplications(after_sequence=cursor, limit=200)
    process response.applications
    cursor = response.max_sequence
    if not response.has_more: break
store cursor as last_change_sequence for next poll cycle
```

`change_sequence` is monotonically increasing and never resets, making it safe for use as a cursor across restarts and network interruptions.

---

## Error Codes

Application Service uses error code range `030000-039999`:

| Code range | Visibility | Example |
|---|---|---|
| 030000-033999 | Private (service internal) | internal implementation errors |
| 034000-036999 | System (readable by other services) | `ApplicationNotFoundException` |
| 037000-039999 | Public (readable by admin clients) | `DuplicateApplicationCodeException` |

gRPC errors include metadata header `xime-error` with `{errorKey, code, message}`. REST errors use body `{errorKey, code, message}`.
