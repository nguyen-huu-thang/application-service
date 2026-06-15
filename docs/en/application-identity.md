# Application Service - Application Identity

[Tieng Viet](../vn/application-identity.md)

## Three Concepts to Distinguish

| Concept | What It Is | Identifier | Example |
|---|---|---|---|
| **SERVICE** | Runtime workload, infrastructure | mTLS cert (`service_id`, `shard_id`) issued by Trust | `social-post-service`, `data-service` |
| **APPLICATION** | A product-level entity, a first-class Subject | 24-byte `identity_id` issued by Application Service | Xime Social, dental clinic app |
| **BOT / AI_AGENT** | Autonomous agent, a first-class Subject | 24-byte `identity_id` issued by Agent Service | Moderation Bot, AI Assistant |

**Invariant: SERVICE is never a Subject.** It never appears as a data owner, permission holder, or in the `sub` field of a JWT. Services act *on behalf of* Subjects.

---

## The Soul-Body Model

An APPLICATION has two identity facets, analogous to "soul" and "body":

### Soul - identity_id

- 24-byte KSUID, issued by Application Service at registration time
- **Permanent and immutable** - lives for the lifetime of the product, potentially years
- Used for: data ownership, permission grants, and audit trail ("acting as whom")
- Stored in the `applications` table; is the shard routing key for data owned by this app

### Body - mTLS Certificate

- Issued by Trust Service per service process
- **Ephemeral** - lives approximately 100 days, renewed on rotation
- Used for: mTLS handshake, service routing, process-level audit ("which process")
- An application has 1:N relationship with certificates: one app, many service processes

### The Link - owner_app_identity_id in Cert SAN

Trust Service's `services` registry table has a nullable column:

```
services.owner_app_identity_id  BYTEA(24)  NULL
```

- `NULL` for Base Platform services (they are not owned by any APPLICATION)
- Set to the application's `identity_id` for Application Layer service processes

When Trust issues a certificate, it embeds the `owner_app_identity_id` value into a Subject Alternative Name (SAN) entry alongside the `service_id`. The value is opaque to Trust - it does not know what application it refers to.

A service process reads its own certificate at startup to discover which application it belongs to. **No bootstrap API call needed. No hardcoded application ID in config.**

---

## Why the Certificate Only Carries Immutable Data

Certificates live ~100 days and are not revocable via CRL in this design. If a certificate carried status or permissions:

- Disabling a malicious app would require revoking and re-issuing hundreds of certs - operationally impractical
- Status `SUSPENDED` would take 100 days to take effect in the worst case

Therefore the certificate only carries the **immutable binding** (which app does this service belong to). All mutable state - application status and system permissions - travels via the sync channel (Kafka push + periodic pull), with a staleness tolerance of a few minutes.

```
Certificate (Trust):              "this service belongs to app X"  - immutable
Application Service -> resource:  app status + system permissions   - mutable, via sync
```

---

## Subject Resolution at Resource Services

Resource services resolve the acting Subject from each incoming request:

```
Request has JWT             -> subject = JWT.sub                       (HUMAN / BOT / AI_AGENT)
No JWT, cert has app id     -> subject = cert.owner_app_identity_id   (APPLICATION)
No JWT, cert has no app id  -> no subject (infra endpoints only: health, sync)
```

**JWT wins when both are present.** A call that wants to act as an APPLICATION must not include a JWT - the intent is explicit in the structure of the request.

Audit logs record the pair: subject (soul) + actor (`service_id` from cert - body).

The public REST edge never has a client certificate, so APPLICATION subjects can never arrive from outside the mTLS mesh.

Both gRPC and internal REST resolve to the same model:

```java
AuthenticatedSubject {
    identity_id,
    subject_type,       // APPLICATION
    actor_service_id,   // from cert
    tenant_id           // null for now
}
```

---

## Bootstrap Sequence

Performed once by an admin, not automated:

```
1. Register app at Application Service   -> receives identity_id (24 bytes)
2. Register service process at Trust,
   providing owner_app_identity_id
3. Trust issues cert                     -> app identity_id embedded in cert SAN
4. Service process starts, reads cert    -> discovers which app it belongs to
```

Preventing dual-write: the list "which services belong to app X" is owned by Trust. Application Service reads from Trust when it needs to display this information - it does not maintain a copy.

---

## Why Not JWT for APPLICATION

| Factor | Detail |
|---|---|
| All calls are internal | No external token needed; mTLS is already enforced on every internal hop |
| Cert is proof-of-possession | Stronger than bearer; cannot be stolen and replayed without the private key |
| No Identity runtime dependency | Identity Service has no role here; reduces coupling |
| No token leakage | JWT tokens can be logged, cached, forwarded; certs cannot leave the TLS context |
| Near-zero overhead | mTLS verification already runs on every internal call; adding cert SAN parsing is negligible |

---

## Why Not service_id as Data Owner

Service processes are ephemeral. They are redeployed, renamed, scaled, or retired independently. An `identity_id` must be:

- **Stable** across the full lifetime of the data it owns
- **Hashable** to a shard assignment that never changes

Service IDs (`social-post-service-shard-0`) fail on both counts. Application IDs (`identity_id` 24-byte KSUID) meet both requirements.

---

## Relationship to Agent Service

Agent Service manages BOT and AI_AGENT subjects. These subjects do use JWT because agents are dynamic (potentially millions), not always inside the mTLS mesh (standalone robots, external bots), and need short-lived tokens for safety.

APPLICATION subjects are the opposite: small count (tens to hundreds), always inside the mesh, long-lived identity - hence certificate-based identity is the right fit.
