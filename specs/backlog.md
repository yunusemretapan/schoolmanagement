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
- **B-6** — Web ve mobilde birebir kopya `i18n/tr-TR.json`, `i18n/index.ts`, `api/ApiProvider.tsx`, `useSystemStatus.ts`
  — Source: spec 0001, review round 1 (I-1) — Severity: info — Recommendation: ortak `packages/i18n` (ve paylaşılan hook)
  — Deferred by: yunusemretapan, 2026-11-17, plan bunu onayladı; ayrışma riski ileride. Not (VERIFY V-F1): web `tr-TR.json`'daki
  `status.runningAnnouncement` yalnızca mobilde kullanılıyor; ortak paket gelince tek kopyada kalır
- **B-8** — 415 `Accept` başlığının korunması testsiz (405 `Allow` test ediliyor; kod yolu ortak `framework.getHeaders()`)
  — Source: spec 0001, VERIFY round 1 (V-F2; `backend/webapi/.../errors/GlobalExceptionHandlerTest.java`) — Severity: low
  — Recommendation: sonraki backend spec'inde 415 için `Accept` başlığı assert'i — Deferred by: yunusemretapan, 2026-11-17, risk düşük
- **B-11** — Mobil `AppearancePanel` radyoları react-native-web çıktısında `aria-checked` taşımıyor (RN-web
  `accessibilityState.checked`'i çevirmiyor); yerel platformda sorun yok — Source: spec 0002, VERIFY round 1 (I-1;
  `frontend/apps/mobile/src/appearance/AppearancePanel.tsx`, OptionGroup) — Severity: info — Recommendation: mobil web
  yayınlanırsa `aria-checked={checked}` ekle — Deferred by: yunusemretapan, 2026-11-18, mobil web hedef platform değil
- **B-12** — Örtük zamanlamalı mobil animasyon API'leri design-lint `rn-motion-direct` kapsamı dışında: Reanimated `Layout`
  takma adı, `Keyframe`, Reanimated 4 CSS geçiş/animasyon prop'ları (`transitionProperty`/`transitionDuration`,
  `animationName`), react-native `Animated.timing/spring/decay`, `LayoutAnimation` — bugün kodda yok — Source: spec 0002,
  review round 4 (L-2; `frontend/scripts/design-lint.mjs`, rn-motion-direct) — Severity: low — Recommendation: ilk kullanan
  spec (0004 mobil bileşenler) kuralı genişletsin ya da `src/motion/timing`'e yardımcı eklesin — Deferred by: yunusemretapan,
  2026-11-18, A-8 kapsam sınırı
- **B-13** — `rn-motion-direct` deseni geniş: `\w+Transition` ve `(Fade|Slide|Zoom…)\w*(In|Out)` React `useTransition`,
  `<SlideIndicator>`, `<ZoomInput>` gibi adları da işaretler — bugün yanlış pozitif yok — Source: spec 0002, review round 4
  (I-1) — Severity: info — Recommendation: Reanimated export adlarına sabitle ya da import kaynağına göre denetle —
  Deferred by: yunusemretapan, 2026-11-18, mevcut kaynakta etki yok
- **B-14** — Spec 0002'nin yerel iOS/Android cihaz gözlemi yapılmadı: Sistem seçiliyken cihaz açık→koyu (yeniden başlatmadan, AC-1),
  TalkBack/VoiceOver ile Görünüm paneli ve geri hareketi (AC-10), yerel "hareketi azalt" (AC-2). Kriterler Jest + Expo web + config
  introspection ile PASS (spec 0001 AC-6 emsali); yerel gözlem ek güvence — Source: spec 0002, VERIFY round 1 (G-1) — Severity: low
  — Recommendation: Android emülatör (ve imkân olursa iOS simülatör) development build'de gözlem + ekran görüntüsü; aksayan
  davranış bug raporu olur — Deferred by: yunusemretapan, 2026-11-18, emülatör kurulumu sonraya bırakıldı
- **B-15** — Güvenlik olaylarının (başarılı/başarısız giriş, kilitlenme, oturum zinciri iptali) kalıcı kaydı yok; yalnızca ID içeren
  uygulama logu var — Source: spec 0003, self-critique (G4) — Severity: low — Recommendation: audit log (BR-6) spec'iyle birlikte
  append-only güvenlik olayı kaydı — Deferred by: yunusemretapan, 2026-11-19, audit log spec'ine bırakıldı
- **B-16** — Giriş rate limit'i tek sunucunun belleğinde ve kaynak olarak `remoteAddr` kullanıyor: birden çok sunucuda sayaç
  bölünür; ters vekil (reverse proxy) arkasında tüm kullanıcılar vekilin adresini paylaşır — Source: spec 0003, plan R4
  (`backend/webapi/.../security/InMemorySourceRateLimiter.java`) — Severity: medium — Recommendation: canlıya çıkıştan önce
  güvenilen vekil başlığını (`X-Forwarded-For`, yalnızca bilinen vekilden) okuyan kaynak çözümü ve paylaşılan sayaç
  (Postgres veya Redis) — Deferred by: yunusemretapan, 2026-11-19, plan 0003 R4 kararı; tek sunuculu geliştirme
- **B-17** — Bozuk JSON gövdesi (`HttpMessageNotReadableException`) 400 yerine 500 `INTERNAL_ERROR` dönüyor ve ERROR log'u
  üretiyor; iç ayrıntı sızmıyor — Source: spec 0003, VERIFY round 1 (F-1; `backend/webapi/.../errors/GlobalExceptionHandler.java`)
  — Severity: low — Recommendation: okunamayan gövdeyi 400 `VALIDATION_FAILED`'e eşle, 4xx'i ERROR'a yazma; bir webapi testiyle
  — Deferred by: yunusemretapan, 2026-11-20, spec 0001'den beri var, spec dışı
