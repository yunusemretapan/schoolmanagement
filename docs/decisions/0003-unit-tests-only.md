# ADR 0003 — Test stratejisi: yalnızca unit test

- Status: Accepted
- Date: 2026-11-17

## Context
Bootstrap'ta üç seçenek sunuldu: (A) yalnızca unit; (B) unit + ArchUnit; (C) unit + ArchUnit + hassas alanlar için
küçük bir Testcontainers seti. Ajan C'yi önerdi; ürün sahibi A'yı seçti.

## Decision
Otomatik testler yalnızca unit seviyesindedir: backend JUnit 5 + AssertJ + Mockito; web Vitest + Testing Library;
mobil jest-expo + RN Testing Library. Entegrasyon, mimari ve e2e test katmanı yoktur.

## Consequences
- Kazanç: hızlı ve altyapısız `check`; tek profil, Docker gerektirmez.
- Maliyet: BR-1 tenant izolasyonu, PostgreSQL RLS ve Flyway migration davranışı otomatik kanıtlanmaz
  (backlog B-1). Katman sınırlarından 4–5 yalnızca review ile korunur.

## Alternatives considered
- **B (ArchUnit):** Altyapısız ve hızlı; ürün sahibi kapsam dışı bıraktı.
- **C (hassas alan Testcontainers seti):** Ajanın önerisi; reddedildi.

## Revisit triggers
- Kapı 1 öncesi (pilot okullarda gerçek çocuk verisi girilmeden önce) — B-1 yeniden değerlendirilir.
- Herhangi bir tenant sızıntısı veya migration hatası bulgusu.
