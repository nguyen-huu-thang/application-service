# Application Service - Sync Mechanism

[Tieng Viet](../vn/sync.md)

## Why Sync Is Needed

APPLICATION subjects do not use JWT. Resource services (data-service and others) need to know two things about each APPLICATION at request time:

1. **Status** - is the app `ACTIVE`? Reject requests from `SUSPENDED` or `DISABLED` apps.
2. **System Permissions** - is the app allowed to perform this specific operation?

Calling Application Service on every request would introduce a runtime coupling that violates the platform's failure-isolation principle. Instead, resource services maintain a local subject cache and keep it up-to-date via two channels: push (Kafka) as the primary mechanism and pull (gRPC) as the fallback.

---

## Primary Channel - Kafka Push

### Topic

```
application.subject.changed
```

### Event Payload

```json
{
  "event_id": "01HX...",
  "event_type": "APPLICATION_STATUS_CHANGED | APPLICATION_PERMISSION_CHANGED",
  "identity_id": "<hex 48 chars, 24 bytes>",
  "status": "ACTIVE",
  "permissions": ["DATA_CREATE_OBJECT", "DATA_READ_OBJECT"],
  "state_version": 5,
  "change_sequence": 1042,
  "tenant_id": null,
  "occurred_at": "2026-06-12T10:00:00Z"
}
```

The payload is a **snapshot** of the full application state, not a diff. The consumer replaces the cached entry entirely, so there is no need to process events in order or handle partial updates.

### Trigger

A `SubjectChangedEvent` is written to the outbox table in the same database transaction that saves the application state change. An outbox scheduler publishes it to Kafka.

### Which Events Trigger a Kafka Message

| Domain Event | Triggers Kafka? | Reason |
|---|---|---|
| `ApplicationRegisteredEvent` | No | App is still `PENDING_REVIEW`; resource services do not need it yet |
| `ApplicationActivatedEvent` | Yes | App becomes usable |
| `ApplicationSuspendedEvent` | Yes | Resource services must start rejecting requests |
| `ApplicationReactivatedEvent` | Yes | Resource services must start accepting requests again |
| `ApplicationDisabledEvent` | Yes | Resource services must reject all requests permanently |
| `ApplicationRetiredEvent` | Yes | App is gone |
| `SystemPermissionGrantedEvent` | Yes | Permission set changed |
| `SystemPermissionRevokedEvent` | Yes | Permission set changed |

---

## Transactional Outbox

A naive approach of publishing directly to Kafka after save has a race condition: the database transaction might commit, then the Kafka publish might fail, leaving the event permanently lost.

The Transactional Outbox pattern eliminates this:

1. **Within the business transaction**: save application state + insert event row into `outbox_events` - one atomic write.
2. **Outbox scheduler** (separate process): polls `outbox_events WHERE published = false`, publishes to Kafka, marks `published = true`.

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

The scheduler reads unpublished rows in creation order and publishes them. On Kafka failure it retries; the event is never lost as long as the database is healthy.

---

## Fallback Channel - gRPC Pull

Resource services poll `PollChangedApplications` every ~5 minutes as a safety net. This handles:

- Kafka consumer lag or temporary outage
- Resource service startup (needs to hydrate cache from scratch)
- Events that were published to Kafka but never consumed (consumer restart, rebalance)

### Pull Loop

```
startup:
    cursor = read last_change_sequence from local store (0 if first run)

every 5 minutes (and on startup):
    loop:
        response = PollChangedApplications(after_sequence=cursor, limit=200)
        for each app in response.applications:
            upsert into local subject_cache
        cursor = response.max_sequence
        if not response.has_more: break
    persist cursor to local store
```

### Why change_sequence Instead of updated_at

| Issue | updated_at | change_sequence |
|---|---|---|
| Clock skew between nodes | Possible - two nodes may disagree on time | Not possible - sequence is server-side only |
| Out-of-order writes | Yes - concurrent updates may land in wrong order | Never - BIGSERIAL is monotonically increasing |
| Precise pagination | No - two events at same millisecond may be missed | Yes - sequence is unique, pagination is exact |
| Reset on restart | No - but inconsistency still possible | Never - BIGSERIAL never resets |

`change_sequence` is a `BIGSERIAL` column updated alongside `state_version` on every status or permission change. The Pull API query is:

```sql
SELECT * FROM applications
WHERE change_sequence > :cursor
ORDER BY change_sequence ASC
LIMIT :limit
```

---

## Resource Service Cache Model

Resource services are expected to maintain two cached structures per APPLICATION:

```
subject_cache (per identity_id):
    identity_id    bytes
    subject_type   string  -- "APPLICATION"
    status         string
    state_version  long
    tenant_id      string

subject_permissions (per identity_id):
    identity_id    bytes
    permission     string
```

On incoming request from an APPLICATION (identified via cert SAN):
1. Look up `subject_cache` by `identity_id`
2. If miss: call `GetSubjectInfo`, populate cache
3. Check `status == ACTIVE`; reject if not
4. Look up `subject_permissions`; check required permission is present

The `state_version` field allows the cache to detect stale entries: if a push event arrives with a `state_version` lower than what is cached, it is discarded as a duplicate.

---

## Latency Guarantee

| Event | Expected propagation time |
|---|---|
| Status / permission change committed to DB | 0 ms |
| Outbox row written (same transaction) | 0 ms |
| Kafka event published by scheduler | < 5 seconds (scheduler interval) |
| Kafka consumer processes event | < 5 seconds (consumer lag) |
| **End-to-end: change visible in resource service** | **< 30 seconds under normal conditions** |
| Fallback pull catch-up | < 10 minutes (5-minute poll + processing) |

Disabling an application (`DISABLED` status) takes effect in resource services within 30 seconds. This is acceptable given the operational context (admin action, not automated).
