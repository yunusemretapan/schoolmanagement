# ADR 0006 — Modüller arası yan etki için süreç içi, senkron domain event

- Status: Accepted
- Date: 2026-11-21

## Context
`docs/architecture.md`'ye göre modüller arası yan etki domain event ile olur, ama olay altyapısı yoktu (ADR 0005, revisit
trigger). Spec 0005 (akademik yapı) yeni açılan her kurumun ilk şubesini kurum adıyla istiyor (Req. 3). Kurum `identity`
modülünde açılıyor, şube ise `institution` modülünün. `identity` başka modülün repository'sine dokunamaz (Forbidden
dependencies 4), Mediator içinden Mediator çağrısı da ADR 0005'te reddedildi. Kaynak: plan 0005, R2.

## Decision
`core-application` küçük bir olay mekanizması sağlar: `DomainEvent`, `DomainEventPublisher` ve `DomainEventHandler`.
Yayıncı (`SynchronousDomainEventPublisher`) olayı aynı türdeki her handler'a çağıranın iş parçacığında, dolayısıyla
çağıranın işleminde ve bağlı kurumunda iletir. Olay kaydı yayınlayan modülün `events` paketindedir (ör.
`identity/events/TenantProvisioned`); handler'lar Spring bean'i olarak kendiliğinden abone olur.

## Consequences
- Kazanç: Modüller birbirinin repository'sine dokunmadan tek işlemde birlikte değişir; handler hata verirse istek
  bütünüyle geri alınır (yarım kurum yok). Kuyruk, tekrar deneme ve ayrı tablo yok.
- Maliyet: Handler'lar yayıncının işlemini uzatır ve aynı hata alanındadır; yavaş veya dış servise giden bir tepki
  (bildirim, e-posta) bu mekanizmayla yapılmamalı. Olay sırası yalnızca kayıt sırasıdır.

## Alternatives considered
- **Veritabanı tetikleyicisi (kurum eklenince şube):** İş kuralı uygulama dışında, testsiz kalır; ad kısaltma gibi
  kurallar SQL'e taşınır.
- **İlk ziyarette şubeyi oluşturmak:** Okuma isteği veri yazar; "her kurumun en az bir şubesi vardır" kuralı ziyarete
  kadar doğru değildir.
- **JobRunr ile asenkron olay:** Kurum açılışı ile şube ayrı işlemlerde olur; arada şubesiz kurum görünür ve hata
  telafisi gerekir. Senkron ihtiyaç için fazla.

## Revisit triggers
- Bir tepkinin yayıncıdan bağımsız başarısız olması veya gecikmesi gerektiğinde (ör. `attendance` → bildirim gönderimi):
  asenkron kanal (JobRunr, outbox) ayrı ADR ile.
- Handler sayısı veya süresi istek gecikmesini ölçülebilir biçimde artırdığında.
