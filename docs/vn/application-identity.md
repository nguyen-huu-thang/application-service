# Application Service - Dinh danh ung dung

[English](../en/application-identity.md)

## Ba khai niem can phan biet ro

| Khai niem | La gi | Dinh danh | Vi du |
|---|---|---|---|
| **SERVICE** | Workload runtime, ha tang | Cert mTLS (`service_id`, `shard_id`) do Trust cap | `social-post-service`, `data-service` |
| **APPLICATION** | San pham logic, Subject hang dau | `identity_id` 24 byte do Application Service cap | Xime Social, app nha khoa |
| **BOT / AI_AGENT** | Tac nhan tu hanh, Subject hang dau | `identity_id` 24 byte do Agent Service cap | Moderation Bot, AI Assistant |

**Luat bat bien: SERVICE khong bao gio la Subject.** No khong bao gio xuat hien voi tu cach so huu du lieu, giu quyen, hay trong truong `sub` cua JWT. Service hanh dong *thay mat* Subject.

---

## Mo hinh "Hon - Xac"

Mot APPLICATION co hai mat dinh danh, giong nhu "hon" va "xac":

### Phan hon - identity_id

- KSUID 24 byte, do Application Service cap khi dang ky
- **Bat bien vinh vien** - song theo vong doi san pham, co the hang nam
- Dung cho: so huu du lieu, phan quyen, audit "nhan danh ai"
- Luu trong bang `applications`; la khoa shard routing cho du lieu thuoc app nay

### Phan xac - Cert mTLS

- Do Trust Service cap cho tung tien trinh service
- **Nhat thoi** - song khoang 100 ngay, rotate theo deploy
- Dung cho: bat tay mTLS, routing service, audit "tien trinh nao"
- Mot app co quan he 1:N voi cert: mot app, nhieu tien trinh service

### Ket noi - owner_app_identity_id trong SAN cua cert

Bang `services` cua Trust co cot nullable:

```
services.owner_app_identity_id  BYTEA(24)  NULL
```

- `NULL` cho cac service Base Platform (khong thuoc APPLICATION nao)
- Duoc dat bang `identity_id` cua app cho cac tien trinh service thuoc Application Layer

Khi Trust cap cert, no nhung gia tri `owner_app_identity_id` vao mot entry Subject Alternative Name (SAN) canh `service_id`. Gia tri nay la opaque voi Trust - no khong biet app do la gi.

Tien trinh service doc cert cua chinh no luc khoi dong de biet minh thuoc app nao. **Khong can goi bootstrap API. Khong can hardcode app id trong config.**

---

## Tai sao cert chi mang du lieu bat bien

Cert song ~100 ngay va khong co thu hoi CRL trong thiet ke nay. Neu cert mang trang thai hoac quyen:

- Vo hieu hoa mot app nguy hiem doi hoi thu hoi va tai cap hang tram cert - khong kha thi van hanh
- Trang thai `SUSPENDED` se mat 100 ngay moi co hieu luc trong truong hop xau nhat

Vi vay cert chi mang **binding bat bien** (service nay thuoc app nao). Moi trang thai co the thay doi - trang thai ung dung va System Permission - di qua kenh dong bo rieng (Kafka push + pull dinh ky), voi do tre chap nhan duoc tinh bang phut.

```
Cert (Trust):                    "service nay thuoc app X" - bat bien
Application Service -> resource: trang thai app + System Permission - co the thay doi, qua kenh sync
```

---

## Giai quyet Subject tai resource service

Resource service xac dinh Subject dang hoat dong tu moi request den:

```
Request co JWT              -> subject = JWT.sub                       (HUMAN / BOT / AI_AGENT)
Khong JWT, cert co app id   -> subject = cert.owner_app_identity_id   (APPLICATION)
Khong JWT, cert khong app id -> khong co subject (chi endpoint ha tang: health, sync)
```

**JWT thang khi ca hai cung co mat.** Loi goi muon hanh dong nhan danh APPLICATION thi khong duoc kem JWT - y dinh ro rang qua cau truc request.

Log audit ghi cap: subject (hon) + actor (`service_id` tu cert - xac).

Canh REST public khong co client cert, nen APPLICATION subject khong the den tu ngoai mTLS mesh.

Ca gRPC lan REST noi bo deu resolve ve cung mot model:

```java
AuthenticatedSubject {
    identity_id,
    subject_type,       // APPLICATION
    actor_service_id,   // tu cert
    tenant_id           // null hien tai
}
```

---

## Thu tu bootstrap

Thuc hien mot lan boi admin, khong tu dong hoa:

```
1. Dang ky app tai Application Service  -> nhan identity_id (24 byte)
2. Dang ky tien trinh service tai Trust,
   cung cap owner_app_identity_id
3. Trust cap cert                        -> app identity_id nhung vao SAN cua cert
4. Tien trinh service khoi dong, doc cert -> tu biet minh thuoc app nao
```

Chong dual-write: danh sach "app X gom nhung service nao" do Trust quan ly. Application Service doc tu Trust khi can hien thi thong tin nay - khong giu ban sao.

---

## Tai sao APPLICATION khong dung JWT

| Yeu to | Chi tiet |
|---|---|
| Moi loi goi deu la noi bo | Khong can token bao ngoai; mTLS da bat buoc tren moi hop noi bo |
| Cert la bang chung so huu | Manh hon bearer; khong the an cap va phat lai ma khong co private key |
| Khong phu thuoc Identity luc runtime | Identity Service khong co vai tro o day; giam coupling |
| Khong lo ro token | JWT co the bi log, cache, forward; cert khong the roi khoi TLS context |
| Chi phi them gan bang 0 | mTLS verify da chay tren moi loi goi noi bo; phan tich SAN la khong dang ke |

---

## Tai sao khong dung service_id lam owner du lieu

Tien trinh service la nhat thoi. Chung duoc redeploy, doi ten, scale ngang, hay ngung hoat dong doc lap. Mot `identity_id` phai:

- **On dinh** trong toan bo vong doi du lieu no so huu
- **Co the hash** thanh vi tri shard khong bao gio thay doi

Service ID (vd `social-post-service-shard-0`) that bai o ca hai tieu chi. Application ID (`identity_id` KSUID 24 byte) dap ung ca hai.

---

## Quan he voi Agent Service

Agent Service quan ly BOT va AI_AGENT subject. Cac subject nay van dung JWT vi agent la dong (co the hang trieu con), khong phai luc nao cung o trong mTLS mesh (robot doc lap, bot ngoai), va can token ngan han vi muc dich an toan.

APPLICATION subject la nguoc lai: so luong it (hang chuc den hang tram), luon o trong mesh, dinh danh song lau - nen dinh danh bang cert la lua chon phu hop.
