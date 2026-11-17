# Okul Yönetim Platformu — Uygulama Kapsamı

## Ürün özeti

Küçük ve orta ölçekli özel okullar ile kurs/etüt merkezleri için AI-native bir okul işletim sistemi. Velinin bayılacağı mobil deneyim satış kancası; kurucunun derdini (tahsilat, kayıt, iletişim yükü) çözmek ise satışı kapatan değer.

- **Hedef segment:** Tek şubeli veya 2–5 şubeli özel okullar, kolejler, kurs ve etüt merkezleri. Karar verici kurucu veya genel müdür.
- **Bilinçli olarak hedeflenmeyen:** Büyük zincir kolejler (K12NET'in güçlü olduğu alan) ilk 18 ayda hedef dışı.
- **Değer önerisi:** "Veliyle iletişimi ve tahsilatı tek uygulamada toplayın, öğretmenin günde 1 saatini geri kazanın."
- **Farklılaşma:** Modern mobil deneyim, AI ile otomatik veli iletişimi, Excel/e-Okul verisinden hızlı geçiş.

## Kullanıcı rolleri

Altı rol var; MVP'de ilk dördü yeterli.

| Rol | Ana işi | Kanal | Faz |
| --- | --- | --- | --- |
| Kurucu / Yönetici | Kayıt, tahsilat, raporlar, ayarlar | Web | 1 |
| Öğretmen | Yoklama, not, ödev, veli mesajı | Mobil + Web | 1 |
| Veli | Takip, bildirim, ödeme, randevu | Mobil | 1 |
| Muhasebe / İdari personel | Tahsilat, taksit, makbuz | Web | 1 |
| Öğrenci | Ödev, sınav sonucu, program | Mobil | 2 |
| Rehber öğretmen | Görüşme notu, gelişim takibi | Web | 2 |

Bir kişi birden fazla role sahip olabilir (örneğin hem öğretmen hem veli). Kardeşi olan veli tek hesapla tüm çocuklarını görür.

## MVP modülleri (Faz 1)

Faz 1, bir okulun ilk dönemini tamamen bu sistemle yürütebilmesi için gereken en dar set.

1. **Kurum ve akademik yapı**
   - Şube, sınıf, şube-sınıf eşleşmesi, dönem ve akademik takvim
   - Ders programı (manuel giriş + Excel içe aktarma)
2. **Öğrenci ve veli kayıtları**
   - Öğrenci kartı, birden fazla veli, kardeş bağlantısı
   - Excel ve e-Okul çıktısından tek tıkla içe aktarma
   - Sağlık notu, alerji, teslim alabilecek kişiler
3. **Yoklama ve devamsızlık**
   - Öğretmen mobilden 10 saniyede yoklama alır
   - Devamsız öğrencinin velisine otomatik bildirim
4. **Veli iletişimi**
   - Duyuru (okul, sınıf, öğrenci bazında), okundu bilgisi
   - Öğretmen-veli birebir mesajlaşma, mesai saati dışı sessiz mod
   - Onay formları (gezi izni, etkinlik katılımı)
   - Fotoğraf ve dosya paylaşımı
5. **Ödev ve not takibi**
   - Ödev verme, teslim durumu, yazılı/sözlü not girişi
   - Veli tarafında sade özet görünüm
6. **Tahsilat ve ön muhasebe**
   - Yıllık ücret, taksit planı, indirim ve burs tanımları
   - Online ödeme (kart, taksitli), otomatik gecikme hatırlatması
   - Makbuz/fatura çıktısı, cari takibi
7. **Randevu**
   - Öğretmen müsaitlik takvimi, veliden randevu talebi
8. **Yönetici paneli**
   - Günlük özet: devamsızlık, geciken ödemeler, okunmayan duyurular
   - Temel raporlar ve Excel dışa aktarma
9. **Mobil uygulamalar**
   - Veli ve öğretmen için tek uygulama, role göre arayüz
   - Push bildirim, kurum logosuyla markalı açılış

## AI katmanı

AI, ayrı bir modül değil; öğretmenin ve yöneticinin iş yükünü azaltan, her modüle gömülü bir yetenek. İlk ikisi MVP'de olmalı, çünkü satış demosunun yıldızı bunlar.

| Özellik | Kim kullanır | Ne yapar | Faz |
| --- | --- | --- | --- |
| Sesli nottan veli mesajı | Öğretmen | 20 saniyelik sesli notu düzgün, kibar bir veli mesajına çevirir; öğretmen onaylayıp gönderir | 1 |
| Haftalık gelişim özeti | Veli | Yoklama, ödev, not ve öğretmen notlarından çocuğa özel haftalık özet üretir | 1 |
| Sınav eksik analizi | Öğretmen, Veli | Sınav sonuçlarından konu bazlı eksikleri ve çalışma önerisini çıkarır | 2 |
| Tahsilat riski uyarısı | Yönetici | Ödeme geçmişinden gecikme riski yüksek velileri işaretler | 2 |
| Doğal dilde rapor | Yönetici | "Bu ay 3 günden fazla devamsızlık yapan 7. sınıflar" gibi sorulara cevap verir | 2 |
| Ön kayıt asistanı | Aday veli | Web sitesi ve WhatsApp'ta ücret, servis, program sorularını yanıtlar, ön kaydı CRM'e düşürür | 3 |
| Erken uyarı | Rehber öğretmen | Devamsızlık + not düşüşü + davranış notu birleşiminden risk sinyali verir | 3 |

**İlkeler:** AI hiçbir mesajı onaysız veliye göndermez. Çocuklara dair veriler model eğitiminde kullanılmaz. Her AI çıktısında kaynak veri görünür.

## Faz 2 ve Faz 3 modülleri

Faz 2 elde tutmayı (retention) artırır, Faz 3 ise satış hacmini ve ortalama sepeti büyütür.

**Faz 2: Derinleşme**

- **Ölçme-değerlendirme:** Deneme sınavı, optik okuma sonucu içe aktarma, konu bazlı analiz
- **Etüt ve özel ders:** Etüt planlama, öğretmen müsaitliği, katılım takibi
- **Servis takibi:** Servis listesi, binme/inme bildirimi, şoför uygulaması
- **Yemek ve sağlık:** Yemek menüsü, revir kaydı, ilaç takibi (anaokulu için kritik)
- **Öğrenci uygulaması:** Ödev, program, sınav sonuçları
- **Rehberlik:** Görüşme notları, gelişim dosyası
- **Personel:** Öğretmen bilgileri, ders yükü, izin

**Faz 3: Büyüme**

- **Ön kayıt CRM'i:** Aday öğrenci hunisi, deneme dersi, bursluluk sınavı, dönüşüm raporu
- **Çok şubeli yönetim:** Şubeler arası konsolide rapor, merkezi ayarlar
- **Kurum markalı uygulama:** Okula özel uygulama adı ve logosu ile mağaza yayını (premium paket)
- **Pazaryeri / ek hizmetler:** Kırtasiye, kıyafet, etkinlik ödemeleri

## Platform altyapısı, entegrasyonlar, KVKK

Çocuk verisi işlendiği için KVKK ve güvenlik, satış görüşmesinde ilk sorulacak konu; MVP'den itibaren tam olmalı.

**Altyapı**

- Multi-tenant SaaS, tenant bazlı veri izolasyonu (nArchitecture tabanı)
- Rol ve yetki yönetimi, şube bazlı görünürlük
- Audit log: kim, hangi öğrenci kaydını, ne zaman görüntüledi/değiştirdi
- Mobil: veli ve öğretmen için tek uygulama (iOS + Android)
- Yedekleme, Türkiye'de barındırma seçeneği

**Entegrasyonlar**

| Entegrasyon | Amaç | Faz |
| --- | --- | --- |
| Ödeme altyapısı (ör. Paycell, iyzico) | Kartla ve taksitli aidat tahsilatı | 1 |
| SMS sağlayıcı (ör. Netgsm) | Uygulamayı kurmayan veliye ulaşma | 1 |
| e-Okul veri aktarımı | Öğrenci listesi ve not içe aktarma (resmi API yoksa dosya üzerinden) | 1 |
| e-Fatura / e-Arşiv | Ücret faturası kesimi | 2 |
| WhatsApp Business API | Ön kayıt ve duyuru kanalı | 2 |
| Google / Microsoft SSO | Öğretmen girişi | 2 |
| Optik okuyucu çıktıları | Sınav sonuç aktarımı | 2 |

**KVKK ve güvenlik**

- Veli açık rıza ve aydınlatma metni akışı, rıza kayıtları
- Fotoğraf paylaşımında öğrenci bazlı izin
- Mezun/ayrılan öğrenci verisi için saklama süresi ve anonimleştirme
- 2FA (yönetici zorunlu), şifreli veri saklama

## Kapsam dışı

Bunlar bilinçli olarak yapılmayacak; odak kaybını önlemek için listede tutuluyor.

- **Tam LMS / canlı ders:** İçerik üretimi ve video ders platformu yok; gerekirse kodlama.io altyapısıyla ayrı ürün.
- **Tam muhasebe / bordro:** Ön muhasebe ve tahsilat var; resmi muhasebe Logo, Mikro gibi araçlarla entegrasyonla çözülür.
- **MEB süreçlerinin yerine geçmek:** e-Okul resmi kayıt sistemi olarak kalır; ürün onu tamamlar, ikame etmez.
- **Devlet okulları:** Satın alma süreci ve ödeme gücü farklı; ilk 18 ayda hedef dışı.
- **Sosyal ağ / veliler arası grup sohbeti:** Moderasyon yükü ve KVKK riski yüksek.

## Yol haritası

| Faz | Kapsam | Sonraki faza geçiş kapısı |
| --- | --- | --- |
| Faz 1: MVP | Yoklama, iletişim, tahsilat, randevu, AI sesli not + haftalık özet | Kapı 1: Pilotlar bir dönemi sistemle tamamladı |
| Faz 2: Derinleşme | Sınav analizi, servis, etüt, öğrenci uygulaması | Kapı 2: İlk ücretli yenilemeler |
| Faz 3: Büyüme | Ön kayıt CRM'i, çok şube, markalı uygulama | — |

Her aşama, önündeki kapının kriteri sağlanmadan başlamaz; böylece yatırım ancak talep kanıtlandıkça büyür.
