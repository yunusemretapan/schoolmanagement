# Domain

> Ortak dil: iş, insanlar ve ajanlar arasında. Bir terim burada yoksa ajan kendi anlamını uydurur.
> Ürün kapsamı ve faz planı: `school.md`. Bootstrap hedefi **Faz 1 (MVP)**; Faz 2–3 kapı kriterine bağlı.

## Ürün ve hedef
Küçük ve orta ölçekli özel okullar (tek şubeli veya 2–5 şubeli) ile kurs/etüt merkezleri için AI-native,
multi-tenant okul işletim sistemi. Velinin kullandığı mobil uygulama satışı başlatır; kurucunun derdini
(tahsilat, kayıt, iletişim yükü) çözmek satışı kapatır.
- **Değer önerisi:** Veliyle iletişimi ve tahsilatı tek uygulamada toplamak, öğretmene günde ~1 saat kazandırmak.
- **Faz 1 başarı kapısı (Kapı 1):** Pilot okullar bir dönemi tamamen bu sistemle yürütür.
- **Faz 1 rolleri:** Kurucu/Yönetici (web), Öğretmen (mobil + web), Veli (mobil), Muhasebe/İdari personel (web).
- **Kapsam dışı:** Tam LMS/canlı ders, tam muhasebe/bordro, e-Okul'un yerine geçmek, devlet okulları,
  veliler arası grup sohbeti (`school.md`, "Kapsam dışı").

## Ubiquitous language

| Terim | Kod adı | Anlam | Karıştırılmasın |
|---|---|---|---|
| Kurum | `Tenant` / `Institution` | Sistemi satın alan okul veya kurs merkezi; veri izolasyonunun sınırı. | Şube |
| Şube | `Campus` | Kurumun fiziksel yerleşkesi (kampüs). | Sınıf |
| Sınıf | `ClassGroup` | Öğrenci grubu (ör. 7/A; MEB dilindeki "şube"). Bir şubeye ve döneme bağlı. Arayüzde "7/A şubesi" etiketi kullanılabilir; kodda ve spec'lerde yalnızca bu anlam geçerli. | Sınıf seviyesi |
| Sınıf seviyesi | `GradeLevel` | Kademe (7. sınıf, hazırlık, anaokulu 5 yaş). | Sınıf |
| Dönem | `Term` | Akademik yıl veya yarıyıl; ücret ve ders programı döneme bağlı. | — |
| Öğrenci | `Student` | Kuruma kayıtlı çocuk/kursiyer. | Kullanıcı |
| Veli | `Guardian` | Öğrenciye yasal/yetkili bağlı kişi. Bir öğrencinin birden çok velisi olabilir; kardeşli veli tek hesapla tüm çocuklarını görür. | Teslim alabilecek kişi |
| Teslim alabilecek kişi | `AuthorizedPickup` | Öğrenciyi okuldan alabilecek kişi; hesabı olmak zorunda değil. | Veli |
| Kullanıcı | `User` | Giriş yapan kişi; birden fazla rolü olabilir (ör. öğretmen + veli). | Rol |
| Rol | `Role` | Kurucu/Yönetici, Öğretmen, Veli, Muhasebe (Faz 1); Öğrenci, Rehber öğretmen (Faz 2). | Kullanıcı |
| Personel | `Staff` | En az bir personel rolü (Kurucu/Yönetici, Öğretmen, Muhasebe) taşıyan kullanıcı; yönetici yönetir (spec 0004). | Veli |
| Hesap durumu | `AccountStatus` | Davet bekliyor (şifre yok), aktif, pasif. Yalnızca aktif hesap giriş yapar; hesap silinmez, pasifleştirilir. | Rol |
| Davet | `Invitation` | Personelin şifresini kendisinin belirlediği tek kullanımlık, 72 saatlik bağlantı; yönetici kendi kanalıyla iletir. | Onay formu |
| Değişiklik kaydı | `AuditEvent` | Kim, kime, ne yaptı, ne zaman: değiştirilemez, silinemez (BR-6). | — |
| Yoklama | `AttendanceRecord` | Bir ders/gün için öğrencinin var/yok/geç durumu. | Devamsızlık |
| Devamsızlık | `Absence` | Yoklamada "yok" olarak işaretlenmiş kayıt. | Yoklama |
| Duyuru | `Announcement` | Çoklu, tek yönlü mesaj; kapsamı kurum/şube/sınıf/öğrenci; okundu bilgisi tutulur. | Mesaj |
| Mesaj | `Message` | Öğretmen ↔ veli birebir yazışma. | Duyuru |
| Onay formu | `ConsentForm` | Velinin evet/hayır yanıtladığı izin talebi (gezi, etkinlik). | Rıza |
| Rıza | `KvkkConsent` | KVKK açık rıza kaydı (aydınlatma metni sürümü + zaman + kişi). | Onay formu |
| Ödev | `Assignment` | Öğretmenin verdiği ödev ve teslim durumu. | — |
| Not | `Grade` | Yazılı/sözlü değerlendirme sonucu. | Öğretmen notu |
| Öğretmen notu | `TeacherNote` | Öğretmenin öğrenciye dair serbest metin gözlemi. | Not |
| Ücret planı | `FeePlan` | Öğrencinin dönemlik borcu; indirim ve burs uygulanmış hali. | Taksit |
| Taksit | `Installment` | Ücret planının vadeli parçası. | Tahsilat |
| Tahsilat | `Payment` | Gerçekleşmiş ödeme; makbuz bu kayda bağlanır. | Taksit |
| Makbuz | `Receipt` | Tahsilata bağlı, kurum içinde sıralı numaralı belge. | Fatura (Faz 2) |
| Cari | `GuardianAccount` | Velinin borç/alacak hesabı. | — |
| Randevu | `Appointment` | Öğretmen müsaitliğinden velinin talep ettiği görüşme. | — |
| AI taslağı | `AiDraft` | AI'ın ürettiği, insan onayı bekleyen içerik; onaysız hiçbir yere gönderilmez. | Mesaj |
| Haftalık gelişim özeti | `WeeklySummary` | Yoklama, ödev, not ve öğretmen notlarından çocuğa özel AI özeti. | — |

## Business rules
- **BR-1:** Bir kurumun verisi başka bir kurumun kullanıcısına hiçbir yoldan görünmez (API, rapor, dışa aktarım, bildirim, AI bağlamı).
- **BR-2:** Kullanıcı yalnızca rolünün ve yetkili olduğu şube/sınıfın verisini görür; veli yalnızca bağlı olduğu öğrencileri görür (kardeşler tek hesapta).
- **BR-3:** AI taslağı insan onayı olmadan veliye gönderilmez.
- **BR-4:** Her AI çıktısında üretimde kullanılan kaynak veri görünür.
- **BR-5:** Öğrenci verisi model eğitiminde kullanılmaz; AI sağlayıcısına yalnızca eğitim dışı (no-training) API ile, gereken en az veri gönderilir.
- **BR-6:** Öğrenci kaydının görüntülenmesi ve değiştirilmesi audit log'a yazılır (kim, ne, ne zaman).
- **BR-7:** Öğretmen-veli mesajları kurumun mesai saatleri dışında sessiz moda düşer: mesaj iletilir, push gönderilmez. Saatler kurum ayarıdır.
- **BR-8:** Fotoğraf yalnızca o öğrenci için fotoğraf izni varsa paylaşılır.
- **BR-9:** Tahsilat kaydı silinmez; düzeltme ters kayıtla (iptal/iade) yapılır. Makbuz numarası kurum içinde sıralı ve benzersizdir.
- **BR-10:** Para tutarı kuruş cinsinden tam sayı (`long`) saklanır; para birimi TRY.
- **BR-11:** Devamsızlık bildirimi yoklama kaydedilince otomatik gider; kurum kapatabilir.
- **BR-12:** Mezun/ayrılan öğrencinin verisi saklama süresi dolunca anonimleştirilir. Süre kurum ayarıdır (hukuki görüşle belirlenir).

Kesin değerler (sessiz mod saatleri, saklama süresi, gecikme hatırlatma takvimi) ilgili spec'te karara bağlanır.

## Key domain invariants
**BR-1, BR-3, BR-5 ve BR-9** hiçbir özellik baskısıyla gevşetilmez. Bunları gevşeten bir değişiklik
ancak yeni bir ADR ile ve insan onayıyla mümkündür.

## Sensitive areas
Bu alanlara dokunan spec Constraints bölümünde `Sensitive area: <ad>` yazar.
1. **Tenant izolasyonu ve yetkilendirme** (BR-1, BR-2)
2. **Ödeme / tahsilat / makbuz** (BR-9, BR-10)
3. **Çocuk kişisel verisi ve KVKK**: sağlık notu, alerji, fotoğraf, rıza, saklama (BR-6, BR-8, BR-12)
4. **AI çıktılarının veliye ulaşması** (BR-3, BR-4, BR-5)
5. **Kimlik doğrulama**: giriş, 2FA, oturum
