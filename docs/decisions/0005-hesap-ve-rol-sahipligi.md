# ADR 0005 — Kullanıcı hesabı, rol ve hesap durumu `identity` modülünde

- Status: Accepted
- Date: 2026-11-20

## Context
`docs/architecture.md` kullanıcı ve rol atamalarını `people` modülüne veriyordu; spec 0003 ise kullanıcı (`users`), rol ve
giriş kimliği (`login_identities`) tablolarını `identity` modülünde kurdu. Spec 0004 (kullanıcı ve rol yönetimi) davet ve
pasifleştirme için kullanıcıyı, giriş kimliğini, davet bağlantısını ve oturumları **tek işlemde** değiştirmek zorunda.
Bunlar iki modüle bölünürse modüller arası yan etki kuralı (domain event) gerekir; henüz olay altyapısı yok ve davetin
bağlantısı yöneticiye eşzamanlı dönmeli. Kaynak: plan 0004, R1 (docs ↔ kod çelişkisi, R-10).

## Decision
Giriş yapan kişinin **hesabı** (ad, roller, hesap durumu: davet bekliyor / aktif / pasif), giriş kimliği, davet bağlantıları,
oturumlar ve personel yönetimi `identity` modülündedir. `people` modülü kişi **kayıtlarını** sahiplenir: öğrenci, veli
kaydı, kardeş bağlantısı, teslim alabilecek kişi, içe aktarma ve veli hesabının öğrenciye bağı. `people`, hesap bilgisine
`identity`'nin query'leri ve command'ları üzerinden erişir (architecture.md, Forbidden dependencies 4).

## Consequences
- Kazanç: Davet, rol değişikliği ve pasifleştirme tek işlemde, tek modülde; yeni olay altyapısı gerekmez. Rollerin her
  istekte okunması (plan 0004, R2) aynı modülün tek servisinde kalır.
- Maliyet: `identity` büyür (giriş + hesap yönetimi). Veli hesabı açılışı (öğrenci ve veli kayıtları spec'i) iki modülü
  birlikte kullanır; bu, `identity`'nin bir command'ı ile yapılır.

## Alternatives considered
- **Hesap ve roller `people`'a taşınır:** Mimari tablo olduğu gibi kalır; ama spec 0003'ün tabloları taşınır ve davet /
  pasifleştirme iki modülün yan etkisi olur (olay altyapısı ya da iç içe command).
- **İç içe command (people → identity):** Mediator içinden Mediator çağrısı; yetki ve işlem sınırı belirsizleşir.

## Revisit triggers
- Modüller arası olay altyapısı (JobRunr veya domain event dağıtıcısı) geldiğinde hesap yönetiminin ayrılması.
- `identity` modülünün tek kişinin kavrayamayacağı büyüklüğe ulaşması.
