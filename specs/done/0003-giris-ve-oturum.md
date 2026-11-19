# Spec 0003 — Giriş ve oturum

- Status: Shipped
- Mode: lite
- Plan: `specs/plans/0003-plan.md`
- Self-critique: done

## Intent
Kurum personeli (Kurucu/Yönetici, Öğretmen, Muhasebe) ve veliler platforma kendi hesaplarıyla güvenli biçimde girip
çıkabilmeli. Bugün her ekran herkese açık ve Faz 1 modüllerinin hiçbiri (öğrenci, yoklama, tahsilat) "kim, hangi
kurumdan, hangi rolle" sorusu yanıtlanmadan yapılamaz. Başarı: giriş yapmamış biri durum bilgisi dışında sistemden
hiçbir şey alamaz; giriş yapan kişi yalnızca kendi kurumunun bağlamında çalışır; kaba kuvvet denemeleri kilitlenir;
veli mobilde ayda birden sık yeniden giriş yapmak zorunda kalmaz. Bilinçli olarak yapılmayanlar: kullanıcı/rol
yönetimi ekranları ve davet (0005), 2FA (0004), şifre sıfırlama (0006), SSO (Faz 2), dışarıdan kendi kendine kayıt.

## Requirements
1. **Hesap kaynağı:** Kendi kendine kayıt yoktur. Platform operatörü, arayüz olmadan (operatör aracıyla) bir kurum ve o
   kurumun ilk Kurucu/Yönetici hesabını oluşturabilir. Şifre en az 10 karakterdir; daha kısası reddedilir. Geliştirme ve test ortamlarında her Faz 1 rolünden örnek
   kullanıcılar bulunur; bu örnekler üretim ortamında oluşmaz.
2. **Giriş kimliği:** Kullanıcı e-posta adresi **veya** cep telefonu numarası ile birlikte şifresini girerek giriş
   yapar. E-posta büyük/küçük harf duyarsızdır. Telefon numarası `+90` ile başlayan tek biçime normalize edilir;
   `05321234567`, `5321234567`, `+90 532 123 45 67` aynı numaradır. Telefonla SMS doğrulaması yapılmaz.
3. **Tekillik ve kurum:** Bir e-posta veya telefon platform genelinde yalnızca bir hesaba aittir. Faz 1'de her hesap
   tek bir kuruma bağlıdır; girişte kurum sorulmaz, kurum hesaptan belirlenir. Bir hesap birden fazla role sahip
   olabilir (ör. öğretmen + veli).
4. **Hatalı giriş:** Kimlik bulunamadığında, şifre yanlış olduğunda veya hesap kilitliyken kullanıcı aynı genel
   mesajı görür ("E-posta/telefon veya şifre hatalı"). Yanıt, hesabın var olup olmadığını belli etmez.
5. **Kilitleme:** Bir hesaba art arda 5 başarısız giriş denemesinden sonra hesap 15 dakika kilitlenir; kilit süresince
   doğru şifre de reddedilir. Başarılı giriş başarısız deneme sayacını sıfırlar. Kilit süresi dolunca hesap kendiliğinden
   açılır. Ayrıca aynı kaynaktan dakikada 20'den fazla giriş denemesi, hesaptan bağımsız olarak geçici olarak reddedilir.
6. **Oturum:** Başarılı girişten sonra kullanıcı oturumu açık kalır.
   - Mobil: oturum son kullanımdan itibaren 30 gün geçerlidir; kullanıldıkça süre uzar. "Kullanım", oturumla yapılan
     başarılı bir sunucu isteğidir. Uygulama açılırken oturumun sunucuda doğrulanması da başarılı bir oturumlu istektir ve
     süreyi uzatır; sunucuya ulaşmadan açıp kapatmak uzatmaz.
   - Web: 12 saat hareketsizlikten sonra oturum düşer; kullanıcı giriş ekranına döner.
   - Süresi dolan oturumla yapılan istek reddedilir ve kullanıcı giriş ekranına yönlendirilir.
   - Oturum yenilenirken daha önce kullanılmış (eskimiş) bir yenileme bilgisi gelirse o cihazın oturum zinciri tamamen
     geçersiz olur; kullanıcı yeniden giriş yapmak zorundadır.
7. **Çıkış:** Kullanıcı çıkış yaptığında o cihazdaki oturum hemen geçersiz olur; aynı kullanıcının diğer cihazlarındaki
   oturumları etkilenmez.
8. **Kurum bağlamı:** Giriş yapmış kullanıcının her işlemi yalnızca kendi kurumunun bağlamında çalışır; istemcinin
   gönderdiği herhangi bir kurum bilgisi dikkate alınmaz (BR-1).
9. **Varsayılan reddet:** Giriş yapmamış kişinin her isteği reddedilir; istisnalar spec 0001'deki durum sorgusu ve kimlik
   doğrulama kapısının kendisidir: giriş, oturum yenileme ve çıkış. Çıkış yalnızca çağıranın kanıtladığı oturumu (geçerli
   erişim bilgisi ya da güncel yenileme bilgisi) kapatır.
   Giriş yapmış kullanıcının isteği de, işlem kendisine açıkça bir yetki gereksinimi tanımlamamışsa reddedilir
   (`docs/security.md`, AuthN/AuthZ).
10. **Ana sayfa:** Giriş yapan kullanıcı adını, kurumunun adını ve rollerini görür; buradan sistem durumuna, tasarım
    vitrinine ve çıkışa ulaşır. Modül ekranları sonraki spec'lerin işidir. Sistem durumu ekranı (spec 0001) giriş
    yapmadan da açılabilir; giriş ekranında "Sistem durumu" bağlantısı bulunur.
11. **Tasarım vitrini** (spec 0002) artık yalnızca giriş yapmış kullanıcıya açılır (B-10).
12. **Expo web erişimi:** Mobil uygulamanın web sürümü backend'e erişebilir; yapılandırmada tanımlı olmayan kökenlerden
    gelen tarayıcı istekleri reddedilir (B-7).

## User experience
- **Flow — giriş (web ve mobil):** Uygulama açılır → oturum yoksa giriş ekranı → kullanıcı "E-posta veya telefon" ve
  "Şifre" alanlarını doldurur → "Giriş yap" → başarılıysa ana sayfa; değilse aynı ekranda hata mesajı, girilen kimlik
  korunur, şifre alanı temizlenir. Giriş ekranındaki "Sistem durumu" bağlantısı durum ekranını açar. Oturum varsa uygulama doğrudan ana sayfayla açılır.
- **Flow — çıkış:** Ana sayfa → "Çıkış yap" → giriş ekranı. Geri tuşu/tarayıcı geçmişi korumalı ekrana döndürmez.
- **Flow — korumalı adres:** Giriş yapmamış kullanıcı web'de `/tasarim` gibi korumalı bir adresi açarsa giriş ekranına
  yönlenir; giriş sonrası istediği adrese döner.
- **States — giriş ekranı:**
  - *Boş:* iki alan boş, "Giriş yap" etkin.
  - *Doğrulama hatası:* boş alan veya geçersiz e-posta/telefon biçiminde istek gönderilmeden ilgili alanın altında
    mesaj ("Bu alan zorunlu", "Geçerli bir e-posta veya telefon girin").
  - *Gönderiliyor:* "Giriş yap" devre dışı, yükleniyor göstergesi; çift gönderim olmaz.
  - *Hatalı giriş / kilitli:* genel mesaj "E-posta/telefon veya şifre hatalı" (Requirement 4).
  - *Ağ hatası:* sunucuya ulaşılamazsa veya 10 saniyede yanıt gelmezse "Sunucuya ulaşılamıyor, tekrar deneyin"; girilen
    kimlik korunur.
- **States — ana sayfa:** *Yükleniyor* (kullanıcı bilgisi gelene kadar), *hazır* (ad, kurum, roller), *oturum
  düştü* (giriş ekranına yönlenir, "Oturumunuz sona erdi, lütfen tekrar giriş yapın" mesajıyla).
- **Design reference:** none — the flow above is the design. Görsel dil spec 0002'deki tasarım token'ları ve görünüm
  (açık/koyu) kurallarıdır; onay SHIP'teki ekran görüntüsü galerisiyle verilir.
- **Accessibility:** Alanların görünür etiketi ve erişilebilir adı vardır; web'de Tab sırası kimlik → şifre → giriş,
  Enter formu gönderir; hata mesajları ekran okuyucuya duyurulur ve ilgili alana bağlıdır; şifre alanında
  göster/gizle düğmesi erişilebilir etiketlidir; kontrast en az WCAG AA (4.5:1), açık ve koyu görünümde.

## Constraints & out of scope
- Sensitive area: Kimlik doğrulama (giriş, oturum)
- Sensitive area: Tenant izolasyonu ve yetkilendirme (BR-1, BR-2)
- R-08 (2026-11-19, yunusemretapan): Req. 6 mobil "kullanım" tanımı açılıştaki oturum doğrulamasını kapsayacak, Req. 9 /
  AC-6 kimlik doğrulama kapısını (giriş, yenileme, çıkış) istisna sayacak şekilde netleştirildi — kaynak: review 1 I-1,
  review 2 I-4. Yeni kabul kriteri yok; kod davranışı değişmedi (delta plan: plan 0003 A-9).
- Güvenlik kuralları `docs/security.md` (AuthN/AuthZ) ile bağlıdır: şifre saklama yöntemi, kısa ömürlü erişim +
  dönen yenileme oturumu, varsayılan reddet.
- Split: proposed 0003 (giriş ve oturum) → 0004 (2FA, TOTP) → 0005 (kullanıcı ve rol yönetimi, davet) → 0006 (şifre
  sıfırlama) — approved, yunusemretapan, 2026-11-19. 12 kabul kriteriyle tek spec olarak kept as one — yunusemretapan,
  2026-11-19, tek yetenek (giriş ve oturum); kriterler onun sınır durumları, hassas alan kanıtı bölünmesin.
- Güvenlik olaylarının kalıcı kaydı bu spec'te yok (B-15); yalnızca ID içeren uygulama logu.
- Not: Spec 0002'nin bölme kararında adı geçen web/mobil bileşen kütüphaneleri henüz dosya olarak açılmadı; numaraları
  0003/0004 değil, açıldıkları sıradaki sonraki numaralar olur.
- **Out of scope:** 2FA (0004; yönetici ve muhasebe için zorunluluk dahil), kullanıcı oluşturma/düzenleme/rol atama
  ekranları ve davet (0005), şifre sıfırlama ve şifre değiştirme (0006), Google/Microsoft SSO (Faz 2), bir kişinin
  birden fazla kurumda hesabı, şube/sınıf/öğrenci bazlı yetki kapsamı (ilgili modül spec'leri; BR-2'nin rol düzeyi bu
  spec'te), "beni hatırla" seçeneği, tüm cihazlardan çıkış, telefonla SMS doğrulaması, audit log ekranı.

## Acceptance criteria
- [x] AC-1 — Operatör aracıyla bir kurum ve ilk Kurucu hesabı oluşturulur; aynı e-posta veya telefonla ikinci bir hesap
  oluşturma girişimi (başka kurumda bile) ve 10 karakterden kısa şifre reddedilir. Örnek kullanıcılar geliştirme/test ortamında vardır, üretim
  yapılandırmasında oluşmaz.
- [x] AC-2 — Doğru e-posta + şifre ile giriş başarılıdır; e-posta farklı büyük/küçük harfle yazılsa da aynı hesaba
  girilir. Ana sayfada kullanıcının adı, kurum adı ve rolleri görünür (birden fazla rol varsa hepsi).
- [x] AC-3 — Telefonla giriş: `05321234567`, `5321234567` ve `+90 532 123 45 67` aynı hesaba girer; geçersiz biçim
  (ör. 9 hane, harf içeren) istek gönderilmeden alan hatası verir.
- [x] AC-4 — Var olmayan kimlik, yanlış şifre ve kilitli hesap aynı mesajı ve aynı hata yanıtını üretir; yanıt hesabın
  varlığını belli eden hiçbir alan içermez.
- [x] AC-5 — Kilitleme sınırları: 4 başarısız denemeden sonra doğru şifre girişi açar ve sayacı sıfırlar; 5. başarısız
  denemeden sonra doğru şifre de reddedilir; kilit 15 dakika dolunca doğru şifre yeniden girişi açar.
  Aynı kaynaktan bir dakikada 21. giriş denemesi (farklı hesaplara olsa da) reddedilir; 20. deneme işlenir.
- [x] AC-6 — Varsayılan reddet: giriş yapmamış istekler durum sorgusu ve giriş/yenileme/çıkış dışında reddedilir (durum
  sorgusu açık kalır; çıkış kanıtlanmamış hiçbir oturumu kapatmaz);
  giriş yapmış kullanıcının yetki gereksinimi tanımlanmamış bir işleme isteği de reddedilir.
- [x] AC-7 — Kurum bağlamı: A kurumunun kullanıcısının oturumuyla yapılan her istek A bağlamında çalışır; istekte
  B kurumunu işaret eden bilgi gönderilse de yanıt B'ye ait hiçbir veri içermez.
- [x] AC-8 — Oturum süreleri: web oturumu 12 saat hareketsizlikten sonra düşer, öncesinde kullanım süreyi uzatır;
  mobil oturum son başarılı oturumlu istekten 30 gün sonra düşer, böyle bir istek süreyi uzatır; düşen oturum giriş ekranına "Oturumunuz
  sona erdi" mesajıyla yönlendirir.
- [x] AC-9 — Çıkış: çıkış sonrası o cihazın oturumuyla yapılan istek reddedilir; aynı kullanıcının başka cihazdaki
  oturumu çalışmaya devam eder; geri tuşu/tarayıcı geçmişi korumalı ekranı göstermez. Eskimiş bir yenileme bilgisi
  yeniden kullanılırsa o cihazın oturumu (son yenilenmiş hali dahil) geçersiz olur.
- [x] AC-10 — Giriş ekranı durumları (web ve mobil): boş, alan doğrulama hatası, gönderiliyor (düğme devre dışı, çift
  gönderim yok), hatalı giriş, ağ hatası (10 sn zaman aşımı dahil); her hatada girilen kimlik korunur, şifre temizlenir.
  Erişilebilirlik bölümündeki klavye sırası, etiketler, duyurular ve kontrast açık/koyu görünümde sağlanır.
- [x] AC-11 — Korumalı adresler: sistem durumu ekranı giriş yapmadan, giriş ekranındaki bağlantıyla açılır; giriş yapmamış kullanıcı `/tasarim` (web) veya mobildeki vitrin bağlantısıyla vitrine
  ulaşamaz, giriş ekranına yönlenir; web'de giriş sonrası istediği adrese döner.
- [x] AC-12 — Expo web kökeninden (yapılandırmada tanımlı) giriş ve oturumlu istekler çalışır; tanımlı olmayan bir
  kökenden gelen tarayıcı isteği reddedilir.

## Evidence
Review round 1 → fix-1 → review round 2 → fix-2 → review round 3 (temiz), salt okunur reviewer; VERIFY ayrı salt okunur QA
oturumunda · `CHECK GREEN (full, 10 steps, 261 s) @6dc8afe` (sonrası yalnızca `specs/`). Kasıtlı kırılmalar B1–B19 repo
dışındaki bir klonda yapıldı, hiçbiri commit edilmedi. Gerçek Postgres 18 gözlemleri O-1…O-10 (dev compose, klondan derlenen jar,
curl + psql) — B-1'in otomatik test açığı açık kalır. Ayrıntı ve triage: `specs/plans/0003-plan.md`.

| Acceptance criterion | Evidence | Status |
|---|---|---|
| AC-1 | `ProvisionTenantCommandHandlerTest`, `DevSeedRunnerTest` — kırıldı (B1 tekillik, B2 şifre uzunluğu); O-1 dev tohumu | PASS |
| AC-2 | `LoginCommandHandlerTest` › `emailCaseDoesNotMatter`, `GetCurrentUserQueryHandlerTest` › `returnsNameInstitutionAndEveryRole`, web/mobil `HomeScreen.test.tsx`; O-2 | PASS |
| AC-3 | `PhoneNumberNormalizerTest`, `LoginIdentifierTest`, `loginSchema.test.ts`, `normalizePhone.test.ts`, web/mobil `LoginScreen.test.tsx`; O-4 | PASS |
| AC-4 | `LoginCommandHandlerTest` › `unknownIdentifierWrongPasswordAndLockedAccountGetTheSameRefusal`, `AuthControllerTest` › `unknownWrongAndLockedLookExactlyAlike` — kırıldı (B3); O-3 | PASS |
| AC-5 | `LoginAttemptPolicyTest`, `LoginCommandHandlerTest`, `InMemorySourceRateLimiterTest` — kırıldı (B4 eşik, B5 15 dk, B6 21. deneme); O-3 paralel 8 deneme → kilit (review 1 M-1), O-8 20. deneme 200 / 21. deneme 429 | PASS |
| AC-6 | `AuthorizationBehaviorTest`, `SecurityConfigTest` — kırıldı (B7 behavior, B8 HTTP kapısı); O-2, O-5 (R-08: giriş/yenileme/çıkış istisna) | PASS |
| AC-7 | `JwtAuthenticationFilterTest` › `tenantHintsInHeadersOrParametersAreIgnored`, `TransactionBehaviorTest`, `GetCurrentUserQueryHandlerTest` — kırıldı (B9, B10); O-1 RLS (bağlanmamış: 0 kullanıcı / 0 kurum), O-2 kurum ipuçları yok sayıldı, `tid` değiştirilmiş token 401 | PASS |
| AC-8 | `SessionPolicyTest`, `RefreshSessionCommandHandlerTest`, `auth.test.ts`, web `RequireAuth.test.tsx`, mobil `AuthProvider.test.tsx` — kırıldı (B11, B12, B18); O-6 11s59dk 200 / 12 sa 401, 29g23s59dk 200 / 30 gün 401. Not: R3 (en fazla 15 dk erken düşme), review 2 I-5 | PASS |
| AC-9 | `LogoutCommandHandlerTest`, `RefreshSessionCommandHandlerTest` › `reusingAnOldRefreshToken…`, web `RequireAuth.test.tsx` › `after signing out, Back…` — kırıldı (B13, B14, B16); O-5 süresi dolmuş erişimle çıkış (review 1 H-1), O-7 diğer cihaz açık, yeniden kullanımda zincir iptali | PASS |
| AC-10 | web/mobil `LoginScreen.test.tsx` (tüm durumlar, kimlik korunur / şifre temizlenir, etiketler, Tab sırası, Enter), `auth.test.ts` › `gives up after 10 s` — kırıldı (B19); S-1…S-6; kontrast: spec 0002 token testleri + galeri, SHIP'te insan değerlendirmesi | PASS |
| AC-11 | web `RequireAuth.test.tsx`, web `LoginScreen.test.tsx` (durum bağlantısı), mobil `Screens.test.tsx` — kırıldı (B16); S-12, S-13 | PASS |
| AC-12 | `CorsConfigTest` — kırıldı (B15); O-9 izinli köken 200, diğer köken 403 | PASS |

UI: S-1…S-13 agent-captured (web + Expo web, açık/koyu), judged by yunusemretapan at SHIP, 2026-11-20. A-8 mobil açılış ekranı ve
Android geri hareketi Expo web'de oluşmuyor; davranış testlerle kanıtlı, yerel cihaz görüntüsü alınmadı.

## Definition of Done
- [x] Every acceptance criterion mapped to proof (test or reproducible observation)
- [x] `scripts/check` green
- [x] Independent review done; real findings fixed, noise rejected with written rationale, deferred ones in `specs/backlog.md`
- [x] Docs / ADRs updated if behavior or architecture changed
- [x] Spec moved to `specs/done/` (it becomes immutable there)

## Scorecard (fill at ship — honest numbers make the process improvable)
| Metric | Value |
|---|---|
| Spec revisions | 1 (R-08: Req. 6 / Req. 9 / AC-6 netleştirmesi, A-9) |
| Fix rounds | 2 |
| Review findings: real / noise / deferred | 12 / 2 / 1 (review 1: 9 real, 1 investigate; review 2: 2 real, 2 noise, 1 investigate; review 3: temiz; VERIFY 1: 1 real, 1 defer → B-17; 2 investigate R-08 ile kapandı) |
| Regressions introduced | 0 |
| Bugs escaped to production | 0 |
