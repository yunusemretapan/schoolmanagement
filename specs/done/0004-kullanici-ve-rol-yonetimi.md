# Spec 0004 — Kullanıcı ve rol yönetimi

- Status: Shipped
- Mode: lite
- Plan: `specs/plans/0004-plan.md`
- Self-critique: done

## Intent
Kurucu/Yönetici, kendi kurumunun personelini (öğretmen, muhasebe, diğer yöneticiler) platform operatörüne ihtiyaç
duymadan sisteme alabilmeli: hesap açmalı, rol verip geri almalı, ayrılan personelin erişimini kapatabilmeli. Bugün bir
kurumda hesap açabilen tek kişi operatör; bu yüzden hiçbir Faz 1 modülü gerçek kullanıcılarla denenemiyor. Başarı:
yönetici yeni bir öğretmeni birkaç dakikada davet eder, öğretmen kendi şifresini kendisi belirleyip girer; ayrılan
personel erişimini anında kaybeder; kurum hiçbir zaman yöneticisiz kalmaz. Bilinçli olarak yapılmayanlar: veli
hesapları (öğrenci ve veli kayıtları spec'i), şube/sınıf bazlı yetki kapsamı, 2FA, şifre sıfırlama, toplu içe aktarma,
davetin sistem tarafından e-posta/SMS ile gönderilmesi.

## Requirements
1. **Yetki:** Personel ve rol yönetimini yalnızca Kurucu/Yönetici rolü yapar. Öğretmen, Muhasebe veya Veli rolüyle
   yapılan her personel yönetimi işlemi reddedilir; bu kullanıcılar personel listesini göremez.
2. **Kurum sınırı:** Yönetici yalnızca kendi kurumunun personelini görür ve yönetir; başka kurumun personeli hiçbir
   yoldan (liste, arama, doğrudan erişim) görünmez ve değiştirilemez (BR-1).
3. **Personel listesi:** Yönetici kurumunun personelini ad, e-posta/telefon, roller ve durum ile listeler. Durum
   üçtür: *davet bekliyor*, *aktif*, *pasif*. Ad, e-posta veya telefonun bir kısmıyla arama yapılabilir (büyük/küçük
   harf duyarsız; telefonda spec 0003'teki normalize biçim). Liste sayfa başına 50 kişi gösterir. Listede yalnızca en az
   bir personel rolü (Kurucu/Yönetici, Öğretmen, Muhasebe) taşıyan hesaplar görünür; Veli rolü de olan kişide Veli rolü
   rozet olarak gösterilir, bu ekrandan eklenemez ve kaldırılamaz.
4. **Davetle hesap açma:** Yönetici ad soyad, e-posta ve/veya cep telefonu (en az biri) ve en az bir rol girerek
   personel davet eder. Atanabilir roller: Kurucu/Yönetici, Öğretmen, Muhasebe; bir kişiye birden fazlası verilebilir.
   Veli rolü bu ekrandan atanamaz. E-posta ve telefon spec 0003'teki biçim ve tekillik kurallarına uyar: platform
   genelinde başka bir hesaba ait bir kimlik kabul edilmez.
5. **Davet bağlantısı:** Davet oluşturulunca sistem tek kullanımlık bir davet bağlantısı üretir ve yöneticiye gösterir;
   yönetici bağlantıyı kendi kanalıyla iletir. Bağlantı oluşturulmasından itibaren 72 saat geçerlidir ve bir kez
   kullanılabilir. Yönetici bekleyen bir davet için yeni bağlantı üretebilir; yeni bağlantı üretilince önceki bağlantı
   geçersiz olur. Davet bağlantısı oturum gerektirmez; geçersiz, süresi dolmuş veya kullanılmış bağlantı hiçbir hesap
   bilgisi göstermez. Bağlantı tahmin edilemez; davet kabul ekranına aynı kaynaktan dakikada 20'den fazla deneme, spec
   0003'teki giriş sınırı gibi geçici olarak reddedilir. Bağlantı web tarayıcısında açılır; mobil kullanan personel de
   şifresini tarayıcıda belirler.
6. **Davet kabulü:** Geçerli bağlantıyı açan kişi davet edildiği adı ve kurumu görür, şifresini iki kez girerek
   belirler (en az 10 karakter, spec 0003 Req. 1). Kabulden sonra hesap *aktif* olur ve kişi giriş ekranına yönlenir;
   kimlik alanı dolu gelir ve kişi kendi kimliği ve şifresiyle giriş yapar (otomatik giriş yoktur). Davet bekleyen hesap giriş yapamaz; denemesi spec 0003'teki genel hatalı giriş
   yanıtını alır. Yönetici personelin şifresini hiçbir aşamada görmez ve belirlemez.
7. **Rol değiştirme:** Yönetici personele rol ekleyebilir veya kaldırabilir; personelde en az bir rol kalır. Kaldırılan
   rolün işlemleri, değişiklikten sonraki ilk istekten itibaren reddedilir; kişinin yeniden giriş yapması gerekmez.
8. **Pasifleştirme ve yeniden etkinleştirme:** Personel silinmez; pasifleştirilir. Pasifleştirme onay ister.
   Pasifleştirilen kişinin tüm cihazlardaki oturumları hemen geçersiz olur: sonraki her isteği reddedilir ve giriş
   denemesi genel hatalı giriş yanıtını alır. Davet bekleyen bir hesap pasifleştirilirse davet bağlantısı geçersiz olur.
   Yönetici pasif hesabı yeniden etkinleştirebilir: daha önce aktif olmuş hesap eski şifresiyle girer; davet
   aşamasında pasifleştirilmiş hesap yeniden *davet bekliyor* durumuna döner ve yeni davet bağlantısı gerekir. Geçmiş kayıtlar (ileride yoklama, makbuz vb.) kişiye bağlı kalır.
9. **Kurum yöneticisiz kalmaz:** Kurumda her zaman en az bir aktif Kurucu/Yönetici bulunur. Son aktif yöneticinin
   yönetici rolü kaldırılamaz ve hesabı pasifleştirilemez. Yönetici kendi hesabını pasifleştiremez ve kendi yönetici
   rolünü kaldıramaz.
10. **Değişiklik kaydı:** Davet oluşturma, davet bağlantısının yenilenmesi, davetin kabulü, rol ekleme/kaldırma,
    pasifleştirme ve yeniden etkinleştirme kalıcı ve değiştirilemez biçimde kaydedilir: kim yaptı, kime, ne yaptı, ne
    zaman. Kayıt şifre veya davet bağlantısı içermez. Bu kaydı gösteren ekran bu spec'te yoktur.
11. **Bilgi düzenleme:** Yönetici personelin ad soyadını her durumda düzenleyebilir. E-posta ve telefon yalnızca davet
    bekliyorken değiştirilebilir (Req. 4 kuralları geçerli) ve değişince önceki davet bağlantısı geçersiz olur. Aktif
    veya pasif hesabın giriş kimliği bu spec'te değiştirilemez. Ad ve iletişim bilgisi değişikliği de Req. 10'a göre
    kaydedilir.

## User experience
- **Flow — personel daveti (web, yönetici):** Ana sayfa → "Personel" → personel listesi → "Personel davet et" → form
  (ad soyad, e-posta, telefon, roller) → "Davet oluştur" → başarı: davet bağlantısı, "Bağlantıyı kopyala" düğmesi ve
  geçerlilik bitişi gösterilir → listeye dönüşte kişi *davet bekliyor* durumunda görünür.
- **Flow — davet kabulü (web, davet edilen):** Davet bağlantısı tarayıcıda açılır → ad ve kurum adı görünür → "Şifre"
  ve "Şifre (tekrar)" → "Şifremi belirle" → başarı.
- **Flow — personel düzenleme (web, yönetici):** Listede bir kişi → personel ayrıntısı → ad soyadı (davet bekliyorsa
  e-posta/telefonu da) ve rolleri değiştir, "Kaydet";
  "Pasifleştir" (onay penceresi: "<Ad> artık sisteme giremeyecek. Pasifleştirilsin mi?") veya "Yeniden etkinleştir";
  davet bekleyen kişide "Yeni davet bağlantısı oluştur".
- **States — personel listesi:** *yükleniyor*; *hazır* (satırlar, durum rozeti); *arama sonucu yok* ("Aramanızla
  eşleşen personel yok"); *hata* ("Personel listesi yüklenemedi" + "Tekrar dene"); *yetkisiz* (yönetici olmayan
  kullanıcıda "Personel" bağlantısı görünmez; adres doğrudan açılırsa "Bu sayfayı görüntüleme yetkiniz yok").
- **States — davet formu:** *boş*; *doğrulama hatası* (istek gönderilmeden alan altında: "Bu alan zorunlu", "E-posta
  veya telefondan en az birini girin", "En az bir rol seçin", "Geçerli bir e-posta girin", "Geçerli bir cep telefonu
  girin"); *gönderiliyor* (düğme devre dışı, çift gönderim yok); *kimlik kullanılamıyor* (sunucu e-postayı/telefonu
  kabul etmedi); *ağ hatası* ("Sunucuya ulaşılamıyor, tekrar deneyin"; girilenler korunur); *başarı* (bağlantı).
- **States — personel ayrıntısı:** *yükleniyor*; *hazır*; *kaydediliyor*; *kaydedildi* (duyurulan onay mesajı);
  *engellendi* (son yönetici / kendi hesabı: ilgili kontrol devre dışı ve nedeni yazılı, ör. "Kurumda en az bir aktif
  yönetici kalmalı"); *hata*.
- **States — davet kabul ekranı:** *yükleniyor*; *geçerli* (ad, kurum, şifre alanları); *geçersiz* (süresi dolmuş,
  kullanılmış veya tanınmayan bağlantı — tek ve aynı mesaj: "Bu davet bağlantısı geçerli değil. Kurumunuzun
  yöneticisinden yeni bir bağlantı isteyin."); *doğrulama hatası* ("Şifre en az 10 karakter olmalı", "Şifreler
  eşleşmiyor"); *gönderiliyor*; *başarı* (giriş ekranına yönlenir, kimlik dolu, "Şifreniz belirlendi, giriş
  yapabilirsiniz"); *ağ hatası*; *çok fazla deneme* ("Çok fazla deneme yapıldı, biraz sonra tekrar deneyin").
- **Design reference:** none — the flow above is the design. Görsel dil spec 0002'deki tasarım token'ları ve görünüm
  (açık/koyu) kurallarıdır; onay SHIP'teki ekran görüntüsü galerisiyle verilir.
- **Accessibility:** Her alanın görünür etiketi ve erişilebilir adı vardır; rol seçimi klavyeyle yapılabilen onay
  kutularıdır; hata mesajları ilgili alana bağlıdır ve ekran okuyucuya duyurulur; onay penceresi odağı içinde tutar,
  Esc ile kapanır ve kapanınca odak tetikleyen düğmeye döner; "Bağlantıyı kopyala" sonucu duyurulur; durum rozetleri
  yalnızca renkle değil metinle de ayırt edilir; kontrast en az WCAG AA (4.5:1), açık ve koyu görünümde.

## Constraints & out of scope
- Sensitive area: Tenant izolasyonu ve yetkilendirme (BR-1, BR-2)
- Sensitive area: Kimlik doğrulama (hesap açma, ilk şifre, oturum sonlandırma)
- Split: proposed 0004 (kullanıcı ve rol yönetimi) → akademik yapı → öğrenci ve veli kayıtları → hassas öğrenci
  bilgileri (sağlık, alerji, teslim alabilecek kişi) → kapsamlı görünürlük (öğretmen–sınıf, BR-2 kapsam düzeyi) →
  içe aktarma (Excel, e-Okul) — approved, yunusemretapan, 2026-11-20. Spec 0003'teki bölme sırası (0004 = 2FA) bu
  kararla değişti; 2FA ve şifre sıfırlama, pilotta gerçek veri girilmeden (Kapı 1 öncesi) yapılır.
- `docs/security.md`'deki "2FA Kurucu/Yönetici ve Muhasebe için zorunlu" kuralı bu spec'te henüz uygulanmaz (2FA
  spec'i); bu durum gerçek kurum verisi girilmeden önce kapanmalıdır.
- Size: 10 kabul kriteriyle sınırda; kept as one — yunusemretapan, 2026-11-20: davet, rol değiştirme ve pasifleştirme
  tek yeteneğin (yönetici personelini yönetir) parçaları; ayrılırsa son yönetici koruması ve oturum sonlandırma gibi
  hassas kurallar iki spec'e bölünür.
- Yalnızca web (yönetici paneli). Hacim: kurum başına 500 personele kadar liste ve arama akıcı çalışır.
- **Out of scope:** veli hesabı ve veli rolü ataması (öğrenci ve veli kayıtları spec'i), şube/sınıf bazlı yetki
  kapsamı, davetin e-posta/SMS ile otomatik gönderimi, mobil uygulamada personel yönetimi ve davetin uygulama içi derin bağlantıyla açılması, aktif hesabın giriş
  kimliğini (e-posta/telefon) değiştirme, değişiklik kaydı ekranı,
  personel silme, toplu davet/içe aktarma, şifre sıfırlama ve değiştirme, 2FA, tüm cihazlardan kendi isteğiyle çıkış,
  Faz 2 rolleri (Öğrenci, Rehber öğretmen), özel/ince taneli izin tanımlama (roller sabittir).

## Acceptance criteria
- [x] AC-1 — Yetki: Öğretmen, Muhasebe ve Veli rolüyle personel listeleme, davet, rol değiştirme, pasifleştirme ve
  bağlantı yenileme istekleri reddedilir; bu kullanıcılarda "Personel" bağlantısı görünmez ve adres doğrudan açılırsa
  "yetkisiz" durumu gösterilir. Oturumsuz istek reddedilir.
- [x] AC-2 — Kurum sınırı: A kurumunun yöneticisi B kurumunun personelini listede ve aramada görmez; B kurumundaki bir
  personele doğrudan yapılan görüntüleme, rol değiştirme, pasifleştirme ve bağlantı yenileme istekleri, kaydın var
  olup olmadığını belli etmeyen aynı yanıtla reddedilir.
- [x] AC-3 — Liste ve arama: liste ad, iletişim, roller ve durum (davet bekliyor / aktif / pasif) gösterir; ad,
  e-posta ve telefonun bir kısmıyla büyük/küçük harf duyarsız arama çalışır (`0532 123` ile `+905321234567` bulunur);
  yalnızca Veli rolü taşıyan hesap listede görünmez, Öğretmen + Veli olan kişi Veli rozetiyle görünür ve Veli rolü bu
  ekrandan değiştirilemez;
  51 personelde ilk sayfa 50, ikinci sayfa 1 kişi gösterir; liste ekranının yükleniyor, hazır, arama sonucu yok ve hata
  durumları gösterilir.
- [x] AC-4 — Davet oluşturma: geçerli bir davet *davet bekliyor* durumunda kişi ve bağlantı üretir; ad yokken, e-posta
  ve telefonun ikisi de yokken, rol seçilmemişken, geçersiz e-posta/telefonda ve Veli rolüyle istek reddedilir (arayüzde
  istek gönderilmeden alan mesajı); kurumda veya başka kurumda zaten kullanılan bir e-posta/telefon, hangi kurumda
  kullanıldığını belli etmeden reddedilir; formun gönderiliyor (çift gönderim yok), ağ hatası (girilenler korunur) ve
  başarı (bağlantı + kopyalama) durumları gösterilir.
- [x] AC-5 — Davet bağlantısı: bağlantı 71 sa 59 dk'da geçerli, 72 sa'te geçersizdir; bir kez kullanılınca ikinci
  kullanım geçersizdir; yeni bağlantı üretilince eskisi geçersiz olur; pasifleştirilen bekleyen davetin bağlantısı
  geçersizdir; e-posta/telefonu değiştirilen bekleyen davetin eski bağlantısı geçersizdir; geçersiz, süresi dolmuş, kullanılmış ve
  tanınmayan bağlantı aynı mesajı verir ve hiçbir hesap bilgisi göstermez; aynı kaynaktan dakikadaki 21. kabul denemesi
  reddedilir, pencere sonrası açılır.
- [x] AC-6 — Davet kabulü: geçerli bağlantıda ad ve kurum görünür; 9 karakterlik şifre ve eşleşmeyen şifreler istek
  gönderilmeden reddedilir, 10 karakterlik kabul edilir; kabulden sonra hesap *aktif* olur, kişi kimlik alanı dolu giriş
  ekranına yönlenir ve kendi kimliği ve şifresiyle giriş yapar; kabulden önce aynı kimlikle giriş denemesi spec 0003'ün genel hatalı giriş yanıtını alır;
  ekranın yükleniyor, gönderiliyor, başarı ve ağ hatası durumları gösterilir.
- [x] AC-7 — Rol değiştirme: eklenen rolün işlemleri ve kaldırılan rolün reddedilmesi, değişiklikten sonraki ilk
  istekte yeniden giriş gerekmeden geçerlidir; tüm rolleri kaldırma isteği reddedilir; ad soyad her durumda,
  e-posta/telefon yalnızca davet bekliyorken değişir, aktif veya pasif hesabın e-posta/telefon değişikliği reddedilir; ayrıntı ekranının kaydediliyor,
  kaydedildi ve hata durumları gösterilir.
- [x] AC-8 — Pasifleştirme: onaydan sonra kişinin iki ayrı cihazdaki oturumlarının sonraki istekleri reddedilir ve
  oturum yenilemeleri başarısız olur; doğru şifreyle giriş genel hatalı giriş yanıtını alır; onay penceresinde
  vazgeçmek hiçbir şeyi değiştirmez; yeniden etkinleştirilen daha önce aktif kişi eski şifresiyle giriş yapabilir; davet aşamasında
  pasifleştirilip yeniden etkinleştirilen kişi *davet bekliyor* durumuna döner ve eski bağlantısı geçersiz kalır.
- [x] AC-9 — Kurum yöneticisiz kalmaz: tek aktif yöneticinin yönetici rolünü kaldırma ve hesabını pasifleştirme
  reddedilir; yöneticinin kendi hesabını pasifleştirmesi ve kendi yönetici rolünü kaldırması reddedilir; iki aktif
  yönetici varken biri diğerini pasifleştirebilir; arayüzde engellenen kontrol devre dışıdır ve nedeni yazılıdır.
- [x] AC-10 — Değişiklik kaydı: davet oluşturma, bağlantı yenileme, kabul, rol ekleme, rol kaldırma, pasifleştirme ve
  yeniden etkinleştirme ve bilgi düzenleme her biri yapan, hedef kişi, işlem ve zamanla bir kayıt üretir; kayıtlar değiştirilemez ve
  silinemez; hiçbir kayıtta şifre veya davet bağlantısı bulunmaz.

## Evidence
Review round 1 → fix-1 → review round 2 → fix-2 → review round 3 (temiz), salt okunur reviewer; VERIFY ayrı QA alt ajanında
(izole worktree, yalnızca dosyalardan) · `CHECK GREEN (full, 10 steps, 295 s) @3261925` (sonrası yalnızca `specs/`). Kasıtlı
kırmalar K-1…K-21 worktree'de yapıldı, hiçbiri commit edilmedi (K-21 kırmızıya düşmedi: B-19). Gerçek Postgres gözlemleri
(dev compose, worktree'den derlenen jar, Node fetch + psql): R5 değiştirilemezlik, R3 kurum kilidi, M-1 kabul ↔ düzenleme
yarışı (10 deneme, kayıp güncelleme 0), L-4 eşzamanlı aynı kimlik (5 deneme, 422), L-1 Türkçe arama, AC-8 oturum kapanışı —
B-1'in otomatik test açığı açık kalır. Ayrıntı ve triage: `specs/plans/0004-plan.md`.

| Acceptance criterion | Evidence | Status |
|---|---|---|
| AC-1 | `RolePermissionsTest`, `StaffControllerTest` › `teachersAccountantsAndGuardiansAreRefusedEveryStaffCall` / `withoutASignedInUserEveryStaffCallIs401`, web `RequireRole.test.tsx`, `StaffListScreen.test.tsx` — kırıldı (K-1, K-15); canlı 403 / 401 | PASS |
| AC-2 | `StaffCommandHandlersTest` › `anotherInstitutionsStaffAndAnUnknownIdAreRefusedAlikeAndNothingChanges` / `anotherInstitutionsIdIsRefusedBeforeItsLoginRowIsLocked`, `ListStaffQueryHandlerTest` › `anotherInstitutionsAdministratorSeesOnlyTheirOwn` — kırıldı (K-2, K-3); canlı başka kurum = bilinmeyen id, aynı 404 | PASS |
| AC-3 | `StaffSearchTermTest`, `ListStaffQueryHandlerTest`, web `StaffListScreen.test.tsx`; canlı L-1 araması | PASS |
| AC-4 | `InviteStaffCommandValidatorTest`, `InviteStaffCommandHandlerTest`, web `InviteStaffScreen.test.tsx`; canlı aynı 422, Veli rolü 400 | PASS |
| AC-5 | `InvitationPolicyTest`, `AccountLifecycleTest`, `RenewInvitationCommandHandlerTest`, `UpdateStaffCommandHandlerTest`, `DeactivateStaffCommandHandlerTest`, `AcceptInvitationCommandHandlerTest`, web `AcceptInvitationScreen.test.tsx` — kırıldı (K-4, K-5, K-6); canlı tek kullanım, 20/21. deneme 404/429, giriş kovası ayrı | PASS |
| AC-6 | `AcceptInvitationCommandHandlerTest`, `AcceptInvitationCommandValidatorTest`, `LoginCommandHandlerTest`, web `AcceptInvitationScreen.test.tsx` — kırıldı (K-7, K-8); canlı kabul öncesi giriş 401, kabul 200 | PASS |
| AC-7 | `SessionActivityCheckerTest`, `JwtAuthenticationFilterTest` › `permissionsComeFromTheAccountsCurrentRolesNotFromTheToken`, `UpdateStaffCommandHandlerTest`, web `StaffDetailScreen.test.tsx` — kırıldı (K-9, K-10); canlı aynı token 403 → 200 → 403 | PASS |
| AC-8 | `DeactivateStaffCommandHandlerTest`, `SessionActivityCheckerTest`, `RefreshSessionCommandHandlerTest`, `LoginCommandHandlerTest`, `ReactivateStaffCommandHandlerTest`, web `ConfirmDialog.test.tsx`, `StaffDetailScreen.test.tsx` — kırıldı (K-11, K-12, K-13, K-16); canlı iki cihaz 401, yenileme ve giriş reddi, yeniden etkinleştirme | PASS |
| AC-9 | `DeactivateStaffCommandHandlerTest`, `UpdateStaffCommandHandlerTest`, `ListStaffQueryHandlerTest`, web `StaffDetailScreen.test.tsx` — kırıldı (K-14, K-15, K-17); canlı R3: eşzamanlı karşılıklı pasifleştirme, her koşuda biri 422 | PASS |
| AC-10 | `StaffCommandHandlersTest` › `everyStaffChangeLeavesOneRecordWithWhoWhomWhatAndWhenButNoSecrets`, `InviteStaffCommandHandlerTest`, `AcceptInvitationCommandHandlerTest`, `UpdateStaffCommandHandlerTest`, `AccountLifecycleTest` — kırıldı (K-18, K-19, K-20); canlı R5: UPDATE/DELETE/TRUNCATE her rolde reddedildi, satırlar yerinde | PASS |

UI: S00…S21 agent-captured (web, açık/koyu, 44 görüntü), judged by yunusemretapan at SHIP, 2026-11-21. Geçici durumlar
(yükleniyor, kaydediliyor, ağ hatası, çok fazla deneme, kabul sonrası yönlenme) bileşen testleriyle kanıtlı, görüntüsü yok.

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
| Review findings: real / noise / deferred | 7 / 3 / 3 (review 1: 6 real, 2 noise, 1 defer → B-18; review 2: 1 real, 1 noise; review 3: temiz; VERIFY: 2 defer → B-19, B-20, 1 mevcut B-17) |
| Regressions introduced | 0 |
| Bugs escaped to production | 0 |
