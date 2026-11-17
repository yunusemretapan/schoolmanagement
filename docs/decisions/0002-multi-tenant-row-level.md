# ADR 0002 — Multi-tenancy: paylaşılan şema, `tenant_id` + Hibernate filtresi + PostgreSQL RLS

- Status: Accepted
- Date: 2026-11-17

## Context
Çocuk verisi ve para taşıyan bir SaaS'ta kurumlar arası veri sızıntısı (BR-1) en ağır hata türü. Hedef müşteri
çok sayıda küçük kurum; kurum başına ayrı veritabanı işletim maliyetini artırır.

## Decision
Tek veritabanı, tek şema. Her tenant'a bağlı tablo `tenant_id` taşır. Uygulama katmanında Hibernate tenant
filtresi otomatik uygulanır; veritabanında PostgreSQL Row-Level Security ikinci savunma hattıdır.
Şube (`Campus`) tenant'ın altında ayrı bir kavramdır.

## Consequences
- Kazanç: düşük işletim maliyeti, tek migration akışı, çift katmanlı izolasyon.
- Maliyet: native SQL tenant filtresini atlar (Known traps); RLS oturum değişkeni her bağlantıda doğru set
  edilmeli. Test kararı (ADR 0003) gereği RLS davranışı otomatik testle kanıtlanmıyor (backlog B-1).

## Alternatives considered
- **Kurum başına şema / veritabanı:** Güçlü izolasyon, ama küçük kurum sayısı arttıkça işletim ve migration yükü.
- **Yalnızca uygulama filtresi:** Tek bir hata BR-1'i deler.

## Revisit triggers
- Bir müşteri sözleşmesi fiziksel veri ayrımı isterse.
- Faz 3 çok şubeli/zincir müşterileri.
