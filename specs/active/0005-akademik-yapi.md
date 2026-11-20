# Spec 0005 — Akademik yapı

- Status: In progress
- Mode: lite
- Plan: `specs/plans/0005-plan.md`
- Self-critique: done

## Intent
Kurucu/Yönetici kurumunun akademik iskeletini sistemde kendisi kurabilmeli: şubeleri (yerleşke), dönemleri (akademik
yıl), sınıf seviyelerini (ör. 7. sınıf, hazırlık, LGS hazırlık) ve bu seviyelerdeki sınıfları (ör. 7/A). Bugün bu yapı
yok; öğrenci kaydı, yoklama, duyuru ve tahsilat gibi Faz 1 modüllerinin hiçbiri öğrenciyi bir sınıfa ve döneme
bağlayamıyor. Başarı: yönetici yeni dönem başında yapıyı birkaç dakikada kurar; tek şubeli okul şube kavramıyla
uğraşmaz; hiçbir kurum başka bir kurumun yapısını göremez. Bilinçli olarak yapılmayanlar: ders programı, akademik
takvim ve tatiller, yarıyıl alt dönemleri, öğrenci kayıtları, öğretmen–sınıf ataması ve buna bağlı görünürlük, Excel ile
içe aktarma, yeni döneme sınıf aktarma.

## Requirements
1. **Yetki:** Akademik yapıyı yalnızca Kurucu/Yönetici görür ve yönetir. Öğretmen, Muhasebe ve Veli rolüyle yapılan her
   okuma ve değiştirme isteği reddedilir. Yapıyı okuma ihtiyacı olan sonraki spec'ler (ör. yoklama) erişimi kendileri
   tanımlar.
2. **Kurum sınırı:** Yönetici yalnızca kendi kurumunun şube, dönem, sınıf seviyesi ve sınıflarını görür ve değiştirir.
   Başka kurumun kaydı hiçbir yoldan (liste, doğrudan erişim) görünmez, değiştirilemez ve kendi kurumunun bir sınıfına
   bağlanamaz (BR-1). Başka kurumun kaydı ile var olmayan kayıt aynı yanıtı alır.
3. **Şube:** Her kurumun en az bir şubesi vardır. Yeni açılan kurumun ilk şubesi kurum adıyla kendiliğinden oluşur; bu
   spec'ten önce açılmış kurumlara da birer şube eklenir; kurum adı 60 karakterden uzunsa ilk 60 karakteri kullanılır.
   Yönetici şube ekler ve adını değiştirir. Şube adı kurum içinde benzersizdir. Kurumda tek şube varken sınıf ekranları
   şube sormaz ve göstermez; şube sayısı yeniden bire düşünce yine göstermez.
4. **Dönem:** Dönemin adı, başlangıç ve bitiş tarihi vardır (ör. "2026–2027", 2026-09-14 – 2027-06-26); bitiş
   başlangıçtan sonradır. Tarihler 2000-01-01 ile 2100-12-31 arasındadır ve bir dönem en çok 731 gün sürer (başlangıç ve
   bitiş günü dahil). Bir kurumda dönemlerin tarih aralıkları çakışmaz; iki dönem aynı günü paylaşamaz. Dönem adı kurum
   içinde benzersizdir. Yönetici dönemin adını ve tarihlerini, sınıfı olsa da, çakışma oluşmadıkça değiştirebilir.
   Dönem varken kurumda tam bir *aktif dönem* vardır: ilk eklenen dönem kendiliğinden aktif olur, yönetici aktif dönemi
   başka bir dönemle (geçmiş veya gelecek) değiştirebilir; sistem aktif dönemi tarihe bakarak kendiliğinden değiştirmez.
   Aktif dönem, sınıf ekranlarının varsayılan dönemidir; aktif olmayan döneme de (ör. gelecek yıl için) sınıf eklenebilir.
   Yarıyıl gibi alt dönemler yoktur.
5. **Sınıf seviyesi:** Kurum kendi seviye listesini tutar: ad ve sıra. Yönetici seviye ekler, adını değiştirir ve sırayı
   değiştirir (ilk seviye yukarı, son seviye aşağı taşınamaz). Seviye adı kurum içinde benzersizdir; kurumda en çok 50
   seviye olur. Liste boşken tek işlemle *standart liste* eklenebilir: Anaokulu,
   1. sınıf … 12. sınıf, Hazırlık (14 seviye, bu sırayla).
6. **Sınıf:** Sınıfın adı (ör. "7/A"), sınıf seviyesi, şubesi ve dönemi vardır. Sınıf adı aynı şube ve dönem içinde
   benzersizdir; başka dönemde veya şubede aynı ad kullanılabilir. Yönetici sınıf ekler, adını ve seviyesini değiştirir.
   Sınıfın dönemi ve şubesi oluşturulduktan sonra değiştirilemez. Sınıflar dönem dönem listelenir; liste seviye sırasına,
   aynı seviyede ada göre dizilir; birden çok şube varsa şubeye göre süzülebilir.
7. **Adlar:** Bütün adlar baştaki ve sondaki boşluklar atılarak saklanır; boş ad kabul edilmez. Benzersizlik büyük/küçük
   harf duyarsızdır (Türkçe I/İ/ı dahil, spec 0004 L-1 ile aynı katlama). Şube, dönem ve seviye adı en çok 60, sınıf adı
   en çok 20 karakterdir.
8. **Eşzamanlılık:** Benzersiz ad, dönem çakışması, tek aktif dönem, son şube ve seviye üst sınırı kuralları, iki
   yönetici aynı anda işlem yaptığında da geçerlidir: isteklerden biri kabul edilir, diğeri kural yanıtıyla reddedilir;
   beklenmeyen hata oluşmaz.
9. **Silme:** Kullanılmayan kayıt silinir; silme onay ister. Kurumun son şubesi, aktif dönem, sınıfı olan şube, dönem ve
   seviye silinemez; reddedilen silmede nedeni yazılır. Bu spec'te sınıfın kendisine bağlı kayıt yoktur, sınıf
   silinebilir; öğrenci kayıtları spec'i sınıfa bağlı öğrencisi olan sınıfın silinmesini engeller.
10. **Değişiklik kaydı:** Şube, dönem, seviye ve sınıf ekleme, ad veya bilgi değişikliği, sıra değişikliği, aktif
    dönem değişikliği ve silme, spec 0004'teki değiştirilemez değişiklik kaydına yazılır: kim yaptı, hangi kayıt (tür,
    kimlik ve ad), ne yaptı (değişen alan adları), ne zaman. Akademik yapı kişisel veri içermediği için kaydın adı
    yazılabilir. Bu kaydı gösteren ekran bu spec'te yoktur.

## User experience
- **Flow — ilk kurulum (web, yönetici):** Ana sayfa → "Akademik yapı" → dönem yoksa "Önce bir dönem ekleyin" →
  "Dönem ekle" (ad, başlangıç, bitiş) → dönem aktif olur → "Sınıf seviyeleri" boşsa "Standart listeyi ekle" veya
  "Seviye ekle" → "Sınıflar" → "Sınıf ekle" (ad, seviye; birden çok şube varsa şube) → sınıf listede görünür.
- **Flow — düzenleme ve silme:** Her bölümde bir kayıt → düzenle (ad ve bilgiler) → "Kaydet"; "Sil" → onay penceresi
  ("<Ad> silinsin mi?") → silinir veya reddedilme nedeni gösterilir. Seviyelerde "Yukarı taşı" / "Aşağı taşı". Dönemlerde
  "Aktif dönem yap".
- **States — akademik yapı ekranı (bölümler: Dönemler, Şubeler, Sınıf seviyeleri, Sınıflar):** *yükleniyor*; *hazır*;
  *boş* (bölüme göre yönlendirme: "Henüz dönem yok", "Henüz sınıf seviyesi yok" + "Standart listeyi ekle", sınıflar
  için dönem veya seviye yoksa "Önce bir dönem ekleyin" / "Önce sınıf seviyesi ekleyin"); *hata* ("Akademik yapı
  yüklenemedi" + "Tekrar dene"); *yetkisiz* (yönetici olmayan kullanıcıda "Akademik yapı" bağlantısı görünmez; adres
  doğrudan açılırsa "Bu sayfayı görüntüleme yetkiniz yok").
- **States — ekleme/düzenleme formları:** *doğrulama hatası* (istek gönderilmeden alan altında: "Bu alan zorunlu",
  "En çok 60 karakter" / "En çok 20 karakter", "Bitiş tarihi başlangıçtan sonra olmalı"); *gönderiliyor* (düğme devre
  dışı, çift gönderim yok); *reddedildi* (sunucu kuralı: "Bu ad zaten kullanılıyor", "Bu tarihler başka bir dönemle
  çakışıyor"); *ağ hatası* ("Sunucuya ulaşılamıyor, tekrar deneyin"; girilenler korunur); *kaydedildi* (duyurulan onay
  mesajı).
- **States — silme:** onay penceresi; *engellendi* (nedeni yazılı: "Kurumun son şubesi silinemez", "Aktif dönem
  silinemez", "Bu kayda bağlı sınıflar var"); *silindi*.
- **Design reference:** none — the flow above is the design. Görsel dil spec 0002'deki tasarım token'ları ve görünüm
  (açık/koyu) kurallarıdır; onay SHIP'teki ekran görüntüsü galerisiyle verilir.
- **Accessibility:** Her alanın görünür etiketi ve erişilebilir adı vardır; tarih alanları klavyeyle doldurulabilir;
  hata mesajları ilgili alana bağlıdır ve ekran okuyucuya duyurulur; aktif dönem yalnızca renkle değil metinle de
  belirtilir; sıra değiştirme düğmeleri klavyeyle kullanılır ve yeni sıra duyurulur; onay penceresi spec 0004'teki gibi
  odağı içinde tutar, Esc ile kapanır ve kapanınca odak anlamlı bir öğeye döner; kontrast en az WCAG AA (4.5:1), açık ve
  koyu görünümde.

## Constraints & out of scope
- Sensitive area: Tenant izolasyonu ve yetkilendirme (BR-1, BR-2)
- Split: spec 0004'te onaylanan sıra: 0004 (kullanıcı ve rol yönetimi) → **0005 akademik yapı** → öğrenci ve veli
  kayıtları → hassas öğrenci bilgileri → kapsamlı görünürlük (öğretmen–sınıf, BR-2 kapsam düzeyi) → içe aktarma.
  Faz 1'in "akademik takvim" ve "ders programı" maddeleri bu spec'ten ayrıldı (clarify 1, yunusemretapan, 2026-11-21);
  sonraki bir spec'e kalır.
- Size: 10 kabul kriteriyle sınırda; kept as one — yunusemretapan, 2026-11-21: şube, dönem, seviye ve sınıf birbirine
  bağlı (sınıf üçünü de ister); bölünürse ilk parça kullanılabilir bir sınıf üretemez.
- Yalnızca web (yönetici paneli). Hacim: kurum başına en çok 5 şube ve dönem başına 150 sınıf, sayfalama olmadan akıcı
  çalışır.
- **Out of scope:** ders programı, akademik takvim ve tatiller, yarıyıl/alt dönem, öğrenci kayıtları ve sınıfa öğrenci
  atama, sınıf öğretmeni / öğretmen–sınıf ataması ve sınıf bazlı yetki kapsamı, şube bazlı yönetici yetkisi, yapıyı
  okuma yetkisinin diğer rollere açılması, Excel / e-Okul içe aktarma, yeni döneme sınıfları aktarma, şube/seviye/sınıf
  arşivleme (yalnızca silme), sınıf kontenjanı, mobil uygulamada akademik yapı, değişiklik kaydı ekranı.

## Acceptance criteria
- [ ] AC-1 — Yetki: Öğretmen, Muhasebe ve Veli rolüyle şube, dönem, seviye ve sınıf okuma, ekleme, değiştirme ve silme
  istekleri reddedilir; bu kullanıcılarda "Akademik yapı" bağlantısı görünmez ve adres doğrudan açılırsa "yetkisiz"
  durumu gösterilir; oturumsuz istek reddedilir.
- [ ] AC-2 — Kurum sınırı: A kurumunun yöneticisi B kurumunun şube, dönem, seviye ve sınıflarını listelerde görmez; B'nin
  bir kaydına doğrudan yapılan görüntüleme, değiştirme ve silme istekleri, var olmayan kayıtla aynı yanıtı alır; A'da
  B'nin şubesi, dönemi veya seviyesiyle sınıf ekleme veya sınıfı B'nin seviyesine taşıma reddedilir ve hiçbir kayıt
  değişmez.
- [ ] AC-3 — Şube: yeni açılan kurumda kurum adıyla bir şube kendiliğinden vardır; spec'ten önce açılmış her kurumda bir
  şube vardır; yönetici şube ekler ve adını değiştirir; aynı ad (büyük/küçük harf ve Türkçe I farkıyla, baş/son
  boşlukla) reddedilir; tek şube varken sınıf formu ve listesi şube göstermez, ikinci şube eklenince gösterir, ikinci şube silinince yine
  göstermez; 61 karakterlik kurum adıyla açılan kurumun şubesi ilk 60 karakteri alır.
- [ ] AC-4 — Dönem: geçerli dönem eklenir ve kurumun ilk dönemiyse aktif olur; bitişi başlangıcıyla aynı gün veya
  önce olan dönem reddedilir; 731 günlük dönem kabul, 732 günlük ret; 2000-01-01 ve 2100-12-31 kabul, 1999-12-31 ve
  2101-01-01 ret; mevcut bir dönemle bir gün bile çakışan aralık (ekleme ve tarih değiştirmede, sınıfı olan dönemde de)
  reddedilir, bir dönemin bitişinden sonraki gün başlayan dönem kabul edilir; aynı ad reddedilir; yönetici aktif dönemi
  (geçmiş veya gelecek bir dönemle) değiştirince önceki dönem aktif olmaktan çıkar ve kurumda tek aktif dönem kalır;
  iki yönetici aynı anda çakışan dönem veya aynı adlı kayıt eklediğinde biri kabul edilir, diğeri kural yanıtıyla
  reddedilir.
- [ ] AC-5 — Sınıf seviyesi: seviye eklenir, adı ve sırası değiştirilir; aynı ad reddedilir; liste boşken "Standart
  listeyi ekle" 14 seviyeyi doğru sırayla ekler, liste boş değilken bu işlem sunulmaz ve istekle yapılırsa reddedilir;
  sıra değişikliği ekrana ve sonraki listelere yansır; ilk seviyede "Yukarı taşı", son seviyede "Aşağı taşı" devre
  dışıdır; 50. seviye kabul, 51. ret.
- [ ] AC-6 — Sınıf: sınıf ad, seviye, şube ve dönemle eklenir, şube tekse kendiliğinden o şubeye, dönem verilmezse aktif
  döneme bağlanır; aktif olmayan bir döneme de sınıf eklenir; aynı şube ve dönemde aynı ad reddedilir, başka dönemde veya şubede kabul edilir; adı ve seviyesi
  değişir, dönemi ve şubesi değiştirilemez; liste seçilen dönemin sınıflarını seviye sırası ve ada göre gösterir, çok
  şubede şubeye göre süzer.
- [ ] AC-7 — Ad sınırları: baş/son boşluklar atılır; boş veya yalnız boşluk ad reddedilir; şube/dönem/seviye adında 60
  karakter kabul, 61 ret; sınıf adında 20 kabul, 21 ret; arayüzde istek gönderilmeden alan mesajı gösterilir.
- [ ] AC-8 — Silme: kullanılmayan şube, dönem, seviye ve sınıf onaydan sonra silinir; onay penceresinde vazgeçmek
  hiçbir şeyi değiştirmez; kurumun son şubesi, aktif dönem ve sınıfı olan şube/dönem/seviye silinemez, arayüzde nedeni
  yazılır ve istekle denendiğinde reddedilir.
- [ ] AC-9 — Ekran durumları: akademik yapı ekranının yükleniyor, hazır, her bölümün boş (yönlendirme metniyle), hata +
  tekrar dene durumları; formların gönderiliyor (çift gönderim yok), reddedildi (ad kullanılıyor, tarih çakışıyor), ağ
  hatası (girilenler korunur) ve kaydedildi (duyurulan mesaj) durumları gösterilir.
- [ ] AC-10 — Değişiklik kaydı: şube, dönem, seviye ve sınıf ekleme, değiştirme, sıra değişikliği, aktif dönem
  değişikliği ve silme her biri yapan, kayıt (tür, kimlik, ad), işlem, değişen alan adları ve zamanla bir değişiklik
  kaydı üretir; reddedilen istek kayıt
  üretmez; kayıtlar değiştirilemez ve silinemez.

## Definition of Done
- [ ] Every acceptance criterion mapped to proof (test or reproducible observation)
- [ ] `scripts/check` green
- [ ] Independent review done; real findings fixed, noise rejected with written rationale, deferred ones in `specs/backlog.md`
- [ ] Docs / ADRs updated if behavior or architecture changed
- [ ] Spec moved to `specs/done/` (it becomes immutable there)

## Scorecard (fill at ship — honest numbers make the process improvable)
| Metric | Value |
|---|---|
| Spec revisions | |
| Fix rounds | |
| Review findings: real / noise / deferred | |
| Regressions introduced | |
| Bugs escaped to production | |
