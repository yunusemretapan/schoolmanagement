# Spec 0001 — Walking skeleton

- Status: Shipped
- Mode: lite
- Plan: `specs/plans/0001-plan.md`
- Self-critique: done

## Intent
Ekip, ilk iş özelliğine başlamadan önce sistemin bütün parçalarının (backend, yönetici web paneli, veli/öğretmen
mobil uygulaması, ortak tasarım dili) birbirine bağlı ve otomatik olarak doğrulanabilir olduğunu görmek istiyor.
Başarı: web paneli ve mobil uygulama açıldığında backend'e ulaşıp sistemin çalıştığını gösterir; backend
erişilemezse bunu anlaşılır biçimde söyler ve yeniden denemeye izin verir; `./scripts/check` gerçek adımlarla
yeşil döner. Hiçbir iş özelliği (öğrenci, yoklama, tahsilat, giriş) yapılmıyor; görsel kimlik ve bileşen
kütüphanesi bir sonraki spec'in ("Tasarım sistemi v1") konusu.

## Requirements
1. Sistem, kimlik doğrulaması gerektirmeden, yalnızca "çalışıyor" bilgisini ve uygulama sürümünü veren bir durum sorgusunu yanıtlar. Bu yanıt hiçbir kurum, kişi veya yapılandırma bilgisi içermez.
2. Sistem, var olmayan bir kaynağa yapılan isteği standart hata biçiminde (hata kodu + başlık; iç ayrıntı yok) yanıtlar.
3. Yönetici web paneli açıldığında sistemin durumunu sorar ve üç durumdan birini gösterir: kontrol ediliyor, çalışıyor (sürümle birlikte), erişilemiyor (yeniden dene seçeneğiyle).
4. Veli/öğretmen mobil uygulaması açıldığında aynı üç durumu gösterir.
5. Web paneli ve mobil uygulama renk, yazı tipi ölçüleri ve boşluk değerlerini tek ortak kaynaktan alır; bir değer yalnızca o kaynakta değiştirildiğinde iki uygulama da yeni değeri kullanır.
6. Kullanıcıya görünen tüm metinler Türkçe'dir ve uygulama kodunun dışında, çeviri dosyasında tutulur.
7. Projenin tek doğrulama komutu (`./scripts/check`) backend, web ve mobilin derleme, biçim/lint ve unit test adımlarını gerçekten çalıştırır ve arkasında çalışan süreç bırakmaz.
8. İç katman (domain) bir framework'e bağımlı hale getirilirse doğrulama başarısız olur.

## User experience
- **Flow (web ve mobil):** Uygulama açılır → durum kontrolü otomatik başlar → sonuç gösterilir. Hata durumunda kullanıcı "Tekrar dene"ye basar → kontrol yeniden başlar.
- **States (her iki uygulama):**
  - *Kontrol ediliyor:* "Sistem kontrol ediliyor…" ve bir yükleniyor göstergesi.
  - *Çalışıyor:* "Sistem çalışıyor" ve backend sürümü (ör. "Sürüm 0.1.0").
  - *Erişilemiyor:* "Sunucuya ulaşılamıyor" ve "Tekrar dene" düğmesi. Backend 5 saniye içinde yanıt vermezse veya hata dönerse bu durum gösterilir.
  - *Boş / yetkisiz:* uygulanamaz — ekranda liste yok, giriş yok.
- **Design reference:** none — the flow above is the design. Görsel kimlik "Tasarım sistemi v1" spec'inde gelir; bu ekran yalnızca ortak token'ları kullanır.
- **Accessibility:** Durum metni ekran okuyucuya duyurulur (canlı bölge); "Tekrar dene" klavyeyle (web: Tab + Enter) ve erişilebilir etiketle kullanılabilir; metin/zemin kontrastı en az WCAG AA (4.5:1).

## Constraints & out of scope
- Sensitive area: none (tenant verisi, kişisel veri, ödeme, AI, kimlik doğrulama yok).
- Durum sorgusu varsayılan-reddet kuralının (docs/security.md) bilinçli ve tek istisnasıdır; yalnızca durum + sürüm döner.
- Test kararı ADR 0003: yalnızca unit test. Mobil ekran görüntüleri, Expo'nun web çıktısı üzerinden başsız tarayıcıyla alınır; mümkün değilse insan alır (docs/testing.md, Fallback).
- **Out of scope:** giriş/kimlik doğrulama, tenant altyapısı, veritabanı şeması ve migration'lar (veritabanı bağlantısı bu spec'te yok), karanlık mod, kurum renkleri, bileşen kütüphanesi, push bildirim, mağaza yayını, CI (remote yok).
- Split: walking skeleton tanımı gereği tek uçtan uca dilimdir; parçalara bölmek amacını bozar — kept as one — yunusemretapan, 2026-11-17, uçtan uca dilim bölünemez.

## Acceptance criteria
- [x] AC-1 — Durum sorgusu kimlik doğrulaması olmadan "çalışıyor" bilgisini ve uygulama sürümünü döner; yanıtta başka alan yoktur.
- [x] AC-2 — Var olmayan bir kaynağa istek, 404 ve standart hata biçiminde (hata kodu + başlık) yanıtlanır; yanıtta stack trace veya iç ayrıntı yoktur.
- [x] AC-3 — Web: durum kontrolü sürerken "Sistem kontrol ediliyor…" ve yükleniyor göstergesi görünür.
- [x] AC-4 — Web: backend yanıt verince "Sistem çalışıyor" ve backend'in döndüğü sürüm görünür.
- [x] AC-5 — Web: backend hata döndüğünde veya 5 saniyede yanıt vermediğinde "Sunucuya ulaşılamıyor" ve "Tekrar dene" görünür; "Tekrar dene" kontrolü yeniden başlatır.
- [x] AC-6 — Mobil: AC-3, AC-4 ve AC-5'teki üç durum ve yeniden deneme davranışı mobil uygulamada da aynen gerçekleşir.
- [x] AC-7 — Web ve mobil tema değerleri ortak token kaynağından gelir: kaynaktaki bir renk değeri değiştiğinde iki uygulamanın temasındaki karşılığı da değişir.
- [x] AC-8 — Kullanıcıya görünen tüm metinler `tr-TR` çeviri dosyasından gelir; ekran bileşenlerinde sabit Türkçe metin yoktur.
- [x] AC-9 — `./scripts/check` (fast ve full) backend, web ve mobilin derleme, biçim/lint ve unit test adımlarını çalıştırıp yeşil döner; residue testi her iki profilde temizdir.
- [x] AC-10 — Domain katmanına framework kodu eklendiğinde `./scripts/check` kırmızıya düşer.

## Evidence
VERIFY (bağımsız QA oturumu) @00e5744 · `CHECK GREEN (full, 9 steps, 286 s) @00e5744` · `RESIDUE CLEAN (fast + full) @fbce356`
(check.conf değişmedi). Kasıtlı kırılmalar repo dışındaki kopyada yapıldı, hiçbiri commit edilmedi. Ayrıntı: `specs/plans/0001-plan.md`.

| Acceptance criterion | Evidence | Status |
|---|---|---|
| AC-1 | `SystemStatusControllerTest` › `returnsUpAndVersionWithoutAuth`, `GetSystemStatusQueryHandlerTest`, `BuildInfoVersionProviderTest` — kırıldı (B1: ek alan → kırmızı) | PASS |
| AC-2 | `GlobalExceptionHandlerTest` › `unknownResourceReturnsProblemDetails404`, `unexpectedErrorLeaksNoInternalDetail` — kırıldı (B2: `detail` sızıntısı → 2 kırmızı) | PASS |
| AC-3 | web `StatusScreen.test.tsx` › `shows checking state`; S-1 | PASS |
| AC-4 | web `StatusScreen.test.tsx` › `shows running state with version`; S-2 | PASS |
| AC-5 | `client.test.ts` › `is still waiting just before 5 s`, `times out after 5 s`; web unreachable/retry/keyboard testleri — kırıldı (B4a/b/c 5 sn sınırı, B6 retry); S-3, S-4 | PASS |
| AC-6 | mobile `StatusScreen.test.tsx` üç durum + retry — kırıldı (B6); S-5…S-8 | PASS |
| AC-7 | web ve mobile `theme.test.ts` › `derives from design tokens`, `follows a change in the token source` — kırıldı (B5) | PASS |
| AC-8 | `frontend-lint` `i18next/no-literal-string`; bağımsız prob: 2 hata | PASS |
| AC-9 | `CHECK GREEN (full, 9 steps, 286 s) @00e5744`; medyan full 237 s / fast 89 s; RESIDUE CLEAN fast + full | PASS |
| AC-10 | `backend-build`: domain'e Spring import → derleme hatası; Spring/Jakarta bağımlılığı → enforcer — kırıldı (B3) | PASS |

UI: S-1…S-8 agent-captured, judged by yunusemretapan at SHIP, 2026-11-17 (web 1280×800 ve mobil Expo web 390×844; checking, running, unreachable 503, unreachable 5 sn; veri stub'lı).

## Definition of Done
- [x] Every acceptance criterion mapped to proof (test or reproducible observation)
- [x] `scripts/check` green
- [x] Independent review done; real findings fixed, noise rejected with written rationale, deferred ones in `specs/backlog.md`
- [x] Docs / ADRs updated if behavior or architecture changed
- [x] Spec moved to `specs/done/` (it becomes immutable there)

## Scorecard (fill at ship — honest numbers make the process improvable)
| Metric | Value |
|---|---|
| Spec revisions | 0 |
| Fix rounds | 2 |
| Review findings: real / noise / deferred | 12 / 3 / 7 (review 1: 7 real + 4 defer; BUILD F-1: 1 real; review 2: 4 real, 2 noise, 1 defer; VERIFY: 1 noise, 2 defer) |
| Regressions introduced | 2 (fix-1 → Low-1 5xx log, Low-2 `Allow`; review 2 yakaladı, fix-2 düzeltti) |
| Bugs escaped to production | 0 |
