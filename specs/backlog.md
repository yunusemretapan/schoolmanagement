# Backlog — deferred findings

Findings a human triaged as **defer**: real, but fixed later by decision (`workflows/feature-development.md`,
step 7). Nothing lands here without that decision; noise is rejected in the triage record, not parked here.

- A criterion moved out at the ship gate (R-08, `→ moved to B-<n>`) lands here too, by the
  human's ship decision; the entry names the spec and criterion.
- One entry per finding, newest last. Delete the entry in the commit of the work that closes it.
- An entry that grows into new behavior becomes a spec (`specs/active/`), not a fix.

<!-- Format (labels stay English):
- **B-<n>** — <finding, one line> — Source: spec <NNNN>, review round <n> (<file:line>) — Severity: <…>
  — Recommendation: <the reviewer's action> — Deferred by: <name>, <YYYY-MM-DD>, <why>
  A decision deferred at bootstrap (its REPORT) uses Source: bootstrap <YYYY-MM-DD>.
-->

- **B-1** — BR-1 tenant izolasyonu, PostgreSQL RLS ve Flyway migration davranışı otomatik testle kanıtlanmıyor
  (yalnızca unit test kararı) — Source: bootstrap 2026-11-17 — Severity: high (sensitive area: tenant izolasyonu)
  — Recommendation: Kapı 1'den (pilotta gerçek çocuk verisi) önce hassas alanlar için küçük bir Testcontainers seti
  (ADR 0003, seçenek C) — Deferred by: ürün sahibi, 2026-11-17, yalnızca unit test tercihi
- **B-2** — e2e aday yolculukları: "veli taksit öder → makbuz oluşur", "öğretmen yoklama alır → veliye bildirim"
  — Source: bootstrap 2026-11-17 — Severity: low — Recommendation: ihtiyaç doğarsa ADR ile, bütçe ≤ 2 yolculuk / 5 dk,
  Playwright `--workers=1` — Deferred by: ürün sahibi, 2026-11-17, e2e şimdilik yok
- **B-3** — Faz 2 ve Faz 3 modülleri (`school.md`) — Source: bootstrap 2026-11-17 — Severity: n/a
  — Recommendation: ilgili kapı kriteri sağlanınca spec'e dönüşür — Deferred by: ürün sahibi, 2026-11-17, faz kapısı
- **B-4** — `frontend/packages/api-client/openapi.yaml` elle yazılıyor; backend'den üretilip karşılaştırılmıyor, sözleşme kayabilir
  — Source: spec 0001, plan R3 (review round 1, L-4) — Severity: medium — Recommendation: backend'den OpenAPI üret
  (springdoc) ve `frontend-contract` adımında elle yazılanla karşılaştır — Deferred by: yunusemretapan, 2026-11-17, plan R3 kararı
- **B-5** — Token dışı ölçüler (mobil/web logo 40, nokta 12; web `h-[36rem]`, `-top-40`, `size-10`, spinner `size-5 border-2` (review 2, Info-1); kart genişliği A-4 ile token'a taşındı) ve
  Tailwind'in varsayılan renk paleti açık — Source: spec 0001, review round 1 (L-3; `frontend/apps/web/src/index.css:1`)
  — Severity: low — Recommendation: "Tasarım sistemi v1"de boyut token'ları + `@theme { --color-*: initial; }`
  — Deferred by: yunusemretapan, 2026-11-17, tasarım sistemi spec'inin konusu
- **B-6** — Web ve mobilde birebir kopya `i18n/tr-TR.json`, `i18n/index.ts`, `api/ApiProvider.tsx`, `useSystemStatus.ts`
  — Source: spec 0001, review round 1 (I-1) — Severity: info — Recommendation: ortak `packages/i18n` (ve paylaşılan hook)
  — Deferred by: yunusemretapan, 2026-11-17, plan bunu onayladı; ayrışma riski ileride. Not (VERIFY V-F1): web `tr-TR.json`'daki
  `status.runningAnnouncement` yalnızca mobilde kullanılıyor; ortak paket gelince tek kopyada kalır
- **B-7** — Yetkilendirme spec'i için notlar: durum sorgusu açık bir `@Public` (veya eşdeğeri) işaretiyle tek istisna olmalı;
  Expo web (8081) ↔ backend (8080) CORS ayarı yok — Source: spec 0001, review round 1 (I-2, I-3;
  `backend/webapi/.../config/MediatorConfig.java:21`) — Severity: info — Recommendation: yetkilendirme spec'inin kabul
  kriterlerine ekle; CORS'u ilk ihtiyaç duyan spec'te — Deferred by: yunusemretapan, 2026-11-17, kimlik doğrulama kapsam dışı
- **B-8** — 415 `Accept` başlığının korunması testsiz (405 `Allow` test ediliyor; kod yolu ortak `framework.getHeaders()`)
  — Source: spec 0001, VERIFY round 1 (V-F2; `backend/webapi/.../errors/GlobalExceptionHandlerTest.java`) — Severity: low
  — Recommendation: sonraki backend spec'inde 415 için `Accept` başlığı assert'i — Deferred by: yunusemretapan, 2026-11-17, risk düşük
- **B-9** — Uygulamaların kendi deterministik modu eksik: web `data-motion=off` yalnızca test setup'ında, prod/preview'da açılamıyor;
  mobilde `MOTION_DISABLED` benzeri bayrak yok (`ActivityIndicator` reduce-motion altında da dönüyor) — Source: spec 0001,
  VERIFY round 1 (V-O1; docs/testing.md, Determinism) — Severity: low — Recommendation: "Tasarım sistemi v1" hareket ilkeleriyle
  birlikte, iki uygulamada da çalışma zamanında açılabilir hareket kapatma anahtarı — Deferred by: yunusemretapan, 2026-11-17
