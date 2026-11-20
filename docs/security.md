# Security

> Her plan ve review'da uyulması gereken temel kurallar. Hassas alanlar: `docs/domain.md`.

## Secrets
- Secrets never enter the repo, specs, prompts, or chat. `.env` is gitignored; provide `.env.example`.
- Agents never print secret values, even when debugging.
- Ödeme (iyzico/Paycell), SMS (Netgsm), Claude ve STT anahtarları ortam başına ayrı; sunucuda ortam
  değişkeni veya secret manager. Log'a yazılmaz.

## Input & output
- Doğrulama her command'da `ValidationBehavior` ile (Jakarta Validation + `*Validator`); frontend'de Zod ile
  tekrarlanır. Sunucu istemciye güvenmez.
- Dosya yükleme: tür/boyut beyaz listesi, nesne depolama, kısa ömürlü imzalı URL; fotoğraflardan EXIF/konum silinir.
- Hata yanıtında stack trace, SQL, iç kimlik veya başka kurumun verisi yok (ProblemDetails + hata kodu).
- Log'da kişisel veri yok; yalnızca ID.
- AI: kullanıcı/veli içeriği sistem talimatlarından ayrı tutulur (prompt injection); çıktı her zaman
  `AiDraft` olarak oluşur, insan onayı olmadan gönderilmez (BR-3); yalnızca gereken en az veri gider (BR-5).

## AuthN / AuthZ
- Giriş: e-posta veya telefon + şifre (Argon2id); kısa ömürlü JWT access token (15 dk, HS256, `JWT_SECRET`) + dönen
  (rotating) refresh token. Kullanılmış bir refresh token tekrar gelirse o cihazın oturumu tamamen iptal edilir (spec 0003).
- **Token saklama (plan 0003, R2):** access token her istemcide yalnızca bellekte. Web: refresh token yalnızca
  `sos_refresh` çerezinde (HttpOnly, Secure, SameSite=Strict, Path=/api/v1/auth); JavaScript göremez. Mobil: SecureStore
  (iOS Keychain / Android Keystore). Expo web: yalnızca bellek (geliştirme yüzeyi). İstemci aynı anda tek yenileme yapar
  (iki paralel yenileme yeniden kullanım sayılır); web'de sekmeler aynı çerezi paylaştığı için yenileme tarayıcının
  sekmeler arası kilidinde (Web Locks API, `sos-refresh`) yapılır (review 1, L-3).
- **Kurum bağlamı:** tenant yalnızca imzalı token'dan gelir; başlık, parametre veya gövdedeki kurum bilgisi yok sayılır.
  Uygulama veritabanına süper kullanıcı olmayan, `BYPASSRLS` yetkisi olmayan bir rolle bağlanır.
- **2FA (TOTP) zorunlu:** Kurucu/Yönetici ve Muhasebe rolleri. Diğer rollerde isteğe bağlı.
- **Varsayılan reddet:** Her command/query bir `@RequiresPermission` taşır; taşımayan reddedilir (`AuthorizationBehavior`).
  Bilinçli `@Public` istisnaları: durum sorgusu, kimlik doğrulama kapısı (giriş, oturum yenileme, çıkış) ve davet bağlantısı
  (bağlantıyı okuma ve şifre belirleme; spec 0004). Çıkış yalnızca çağıranın kanıtladığı oturumu kapatır (spec 0003, R-08).
- **Roller her istekte güncel:** erişim token'ındaki roller yetki için kullanılmaz; filtre her istekte oturumu ve hesabın
  güncel rollerini/durumunu okur (`SessionValidator`). Kaldırılan rol ve pasifleştirilen hesap bir sonraki istekte reddedilir;
  pasifleştirme ayrıca hesabın tüm oturumlarını iptal eder (spec 0004, plan R2).
- **Davet bağlantısı:** `<id>.<32 bayt rastgele>`; yalnızca SHA-256 özeti saklanır, sabit zamanlı karşılaştırılır. 72 saat
  geçerli, tek kullanımlık; yenileme, kimlik değişikliği ve pasifleştirme eski bağlantıyı iptal eder. Web adresi
  `/davet#<token>`: parça (fragment) sunucuya, loglara ve Referer'a gitmez; API token'ı gövdede alır. Geçersiz, süresi dolmuş,
  kullanılmış ve tanınmayan bağlantı aynı 404'ü alır. Kaynak başına dakikada 20 deneme, girişten ayrı sayılır (spec 0004).
- Yetki kapsamı: kurum → şube → sınıf → öğrenci (BR-2). Tenant: Hibernate filtresi + PostgreSQL RLS (BR-1).
- Login'de kaynak başına rate limit (dakikada 20) ve hesap başına kilitleme (5 hata → 15 dk); hatalı giriş, olmayan hesap ve
  kilitli hesap aynı yanıtı alır (spec 0003).
- Audit log (BR-6): append-only tablo (`audit_events`: tetikleyici UPDATE/DELETE/TRUNCATE'i her rolde reddeder; tenant'lı,
  RLS). Spec 0004 personel değişikliklerini yazar (kim, kime, ne, ne zaman; şifre, bağlantı ve iletişim bilgisi yazılmaz);
  öğrenci kaydı görüntüleme kendi spec'inde eklenir.

## Dependencies
- Yeni kütüphane: spec/planda gerekçe + insan onayı.
- Kontrol: lisans (MIT/Apache/BSD serbest; GPL/AGPL yasak), son 12 ayda bakım, bilinen kritik CVE yok.
- OWASP Dependency-Check CI'da haftalık çalışır (`check` dışında; yavaş ve ağ bağımlı).

## Review lens
Security is a mandatory dimension of every independent review (see `prompts/review.md`), not a
separate afterthought phase.
Bu projede her review özellikle arar: tenant sızıntısı, eksik `@RequiresPermission`, log'da kişisel veri,
AI'ın onaysız gönderimi, tahsilatın silinmesi/değiştirilmesi.
