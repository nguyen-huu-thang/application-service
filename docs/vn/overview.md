# Application Service - Tong quan

[English](../en/overview.md)

## Vai tro

Application Service la **Application Identity Domain Service** cua Xime Base Platform. Nhiem vu duy nhat cua no la lam nguon truth cho moi thu lien quan den Subject loai APPLICATION:

- Dang ky ung dung va cap `identity_id` vinh vien
- Luu tru metadata (`application_code`, ten, mo ta)
- Quan ly vong doi ung dung (state machine)
- Cap va thu hoi System Permission cho APPLICATION subject
- Thong bao cho resource service khi trang thai hoac quyen cua ung dung thay doi

Service nay khong co vai tro trong xac thuc, quan ly session hay logic nghiep vu cua bat ky ung dung nao.

---

## Vi tri trong Base Platform

```
Trust Service        <- CA, cert mTLS, JWT signing key
Identity Service     <- Authentication gateway, cap JWT cho HUMAN/BOT/AI_AGENT
User Service         <- Registry HUMAN subject
Agent Service        <- Registry BOT, AI_AGENT subject
Application Service  <- Registry APPLICATION subject  (service nay)
Data Service         <- Data infrastructure, kiem tra quyen subject
Notification Service <- Gui thong bao
Payment Service      <- Thanh toan
```

Application Service la doi tac cua User Service va Agent Service. User Service quan ly HUMAN subject, Agent Service quan ly BOT/AI_AGENT, con Application Service quan ly APPLICATION subject.

---

## Ranh gioi

| Application Service LAM | Application Service KHONG LAM |
|---|---|
| Dang ky app, cap `identity_id` 24 byte | Cap hoac xac minh JWT token |
| Luu metadata: code, ten, mo ta | Giu credential cho app |
| Quan ly vong doi (state machine + `state_version`) | Luu binding service-to-app (Trust quan ly qua cert SAN) |
| Cap va thu hoi System Permission | Tham gia luong dang nhap Identity |
| Publish sync event cho resource service | Chua logic nghiep vu cua ung dung cu the |
| Expose Pull API de reconcile du phong | Tro thanh runtime dependency cua service khac |

---

## Tai sao APPLICATION khong dung JWT

Moi loi goi nhan danh APPLICATION deu phat sinh tu tien trinh service chay ben trong mTLS mesh. Cert mTLS luon co san va la bang chung so huu (manh hon bearer token). Trust Service nhung `owner_app_identity_id` vao SAN cua cert, nen khong can bootstrap API hay hardcode config.

Neu dung JWT se khien APPLICATION subject phu thuoc vao Identity Service luc runtime - mot coupling khong duoc thiet ke san.

Chi tiet: [Dinh danh ung dung](application-identity.md).

---

## Cac loai Subject tren platform

```
SubjectType     Owner Service          Co che xac thuc       JWT?
HUMAN        -> user-service           credential + Identity  co
BOT          -> agent-service          API key + Identity     co
AI_AGENT     -> agent-service          API key + Identity     co
APPLICATION  -> application-service    cert (Trust) + sync    khong
(SERVICE     -> trust-service          cert mTLS              khong bao gio la subject)
```

SERVICE khong bao gio la Subject va khong bao gio xuat hien voi tu cach so huu du lieu hay giu quyen.

---

## Cong va co so du lieu

| Thong tin | Gia tri |
|---|---|
| HTTP port | 8085 |
| gRPC port | 9094 |
| Database | `application_service` |
| Package goc | `vn.xime.application` |
| Main class | `ApplicationServiceApplication` |

---

## Quan he voi Trust Service

Application Service chinh no la mot Base Platform service binh thuong: no bootstrap cert mTLS tu Trust Service (cert cua no KHONG mang `owner_app_identity_id`).

Khai niem `owner_app_identity_id` chi ap dung cho cac tien trinh service thuoc Application Layer. Bang `services` cua Trust co cot nullable `owner_app_identity_id BYTEA(24)`. Application Service la nguon tu cuong cung cap gia tri cua cot nay.

---

## Quan he voi Resource Service

Resource service (data-service va cac service tuong lai) cache thong tin subject va System Permission cua APPLICATION subject. Application Service day cap nhat qua Kafka va expose Pull API de reconcile. Resource service co the hoat dong trong thoi gian dai ma khong can lien he Application Service - chi can du lieu tuoi de kiem tra quyen.

Chi tiet: [Co che dong bo](sync.md).
