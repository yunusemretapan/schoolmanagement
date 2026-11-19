# Architecture

> Ajanlar planlamadan önce bu dosyayı okur. Burada yazan, karara bağlanmış mimaridir.

## System overview
Multi-tenant bir SaaS. **Backend:** Java 25 + Spring Boot 4 üzerinde, Maven multi-module olarak kurulan
**Clean Architecture (nArchitecture düzeni)**. Bağımlılıklar içe akar (`domain` ← `application` ←
`persistence` / `infrastructure` / `webapi`), istekler CQRS ile Mediator + pipeline behavior zincirinden
(Authorization → Validation → Transaction → Audit) geçer. Veritabanı **PostgreSQL 18** (Flyway migration,
Spring Data JPA). Arka plan işleri **JobRunr** ile Postgres üzerinde çalışır. **Frontend:** React 19 + TypeScript
(Vite) yönetici paneli ve React Native + Expo veli/öğretmen uygulaması; ikisi ortak design token ve OpenAPI'den
üretilen API istemcisini paylaşır. Gerekçe: küçük ekip için tek dağıtılabilir birim, katman sınırları test ile
derleme anında zorlanır, ileride parçalanabilir (ADR 0001, ADR 0002).

## Repository layout
```
backend/
├── core/core-application     Mediator, PipelineBehavior'lar (Authorization, Validation, Transaction, Audit, Logging, Caching)
├── core/core-persistence     Tenant'a bağlı base entity + Hibernate tenant filtresi (spec 0003); paging, dinamik sorgu sonra
├── core/core-security        JWT, Argon2id, @Public/@RequiresPermission, CurrentUser (spec 0003); 2FA (TOTP), şifreleme sonra
├── core/core-crosscutting    Exception tipleri, ProblemDetails, logging
├── domain                    Entity, enum, değer nesnesi (Money…) — saf Java
├── application               features/<modül>/{commands, queries, rules, dtos, mappers, constants}
├── persistence               JPA config, repository implementasyonları, Flyway migration'ları; webapi'ye yalnızca runtime
├── infrastructure            Dış servis adaptörleri: ödeme, SMS, Claude, STT, push, nesne depolama
└── webapi                    REST controller'lar, OpenAPI, Spring Boot uygulaması
frontend/
├── packages/design-tokens    Renk, tipografi, boşluk, hareket token'ları (web + mobil, karanlık mod, kurum renkleri)
├── packages/api-client       OpenAPI'den üretilen tipli istemci
├── apps/web                  Yönetici + muhasebe paneli
└── apps/mobile               Veli + öğretmen uygulaması (tek uygulama, role göre arayüz)
```

## Modules / components and ownership

| Module | Single responsibility | Owns |
|---|---|---|
| `institution` | Kurum, şube, dönem, sınıf, sınıf seviyesi, ders programı | Akademik yapı, kurum ayarları |
| `identity` | Giriş, oturum (cihaz başına dönen yenileme zinciri), kilitleme, kurum açma (operatör) | Giriş kimlikleri, oturumlar; `login_identities` ve `sessions` bilinçli olarak tenant'sız/RLS'siz (plan 0003, R1) |
| `people` | Öğrenci, veli, kullanıcı, rol, kardeş bağlantısı, teslim alabilecek kişi, içe aktarma | Kişi kayıtları, rol atamaları |
| `attendance` | Yoklama ve devamsızlık | Yoklama kayıtları |
| `communication` | Duyuru, mesaj, onay formu, dosya/fotoğraf paylaşımı, bildirim gönderimi | İletişim kayıtları, okundu bilgisi |
| `academics` | Ödev, teslim, not | Ödev ve not kayıtları |
| `billing` | Ücret planı, indirim/burs, taksit, tahsilat, makbuz, cari | Finansal kayıtlar (değiştirilemez tahsilat) |
| `appointments` | Öğretmen müsaitliği, randevu talebi | Randevular |
| `reporting` | Yönetici günlük özeti, raporlar, Excel dışa aktarma | Yalnızca okuma modelleri (başka modülün verisini query ile alır) |
| `ai` | AI taslakları, sesli not → mesaj, haftalık gelişim özeti | AI taslakları ve kaynak veri referansları |
| `consent` | KVKK aydınlatma/rıza akışı, fotoğraf izni, saklama/anonimleştirme | Rıza kayıtları |

## Communication rules
- Controller → yalnızca Mediator'a command/query gönderir.
- Modüller arası senkron okuma: diğer modülün **query**'si üzerinden. Modüller arası yan etki: **domain event**
  (ör. `attendance` → `AbsenceRecorded` → `communication` bildirimi gönderir).
- Dış servis çağrısı yalnızca `application`'daki port arayüzü üzerinden; implementasyon `infrastructure`'da.
- Her tenant'a bağlı tablo `tenant_id` taşır; Hibernate tenant filtresi (uygulama) + PostgreSQL RLS
  (veritabanı) birlikte uygulanır (BR-1).

## Forbidden dependencies (make them testable)
Test kararı yalnızca unit (ADR 0003) olduğu için ayrı bir mimari test seti yok. Zorlama:
- **Derleme (Maven modül grafiği):** 1–3 — `domain`'in pom'unda Spring/JPA yok; `application` pom'u
  `persistence`/`infrastructure`/`webapi`'ye bağımlı değil; repository implementasyonları `webapi`'ye görünmez.
- **Review:** 4–5 (paket sınırı) — her review'da kontrol edilir.
- **ESLint** `no-restricted-imports`: 6.
1. `domain` hiçbir modüle bağımlı olmaz; Spring/JPA/Jakarta anotasyonu kullanmaz.
2. `application`, `persistence` / `infrastructure` / `webapi` modüllerine bağımlı olmaz; onlara yalnızca port arayüzleriyle erişir.
3. `webapi` repository'leri doğrudan çağırmaz; her işlem bir command/query üzerinden geçer.
4. Bir feature modülü başka bir feature'ın repository'sine veya entity'sine dokunmaz; yalnızca query, command veya domain event.
5. Claude/STT istemcisine yalnızca `infrastructure.ai` erişir; AI'ya giden veri tek kapıdan geçer (BR-5).
6. Frontend uygulamaları API'ye yalnızca `packages/api-client` üzerinden erişir; elle `fetch` yok.

## Deliberately out of scope
- Faz 2–3 modülleri (ölçme-değerlendirme, servis, etüt, öğrenci uygulaması, CRM, çok şube konsolidasyonu,
  markalı uygulama yayını) — kapı kriteri sağlanmadan tasarlanmaz. Yalnızca `Tenant` ↔ `Campus` ayrımı ve
  kurum renkleri için token desteği baştan vardır.
- Mikroservisler, ayrı mesaj kuyruğu (Kafka/RabbitMQ).
- `school.md` "Kapsam dışı" listesi.
