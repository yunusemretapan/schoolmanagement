# Spec 0002 — Tasarım temelleri

- Status: Approved
- Mode: lite
- Plan: `specs/plans/0002-plan.md`
- Self-critique: done

## Intent
Ürün sahibi, okulların ve velilerin "sıradan bir admin şablonu" değil profesyonel, modern bir arayüz görmesini istiyor;
bunun ön koşulu, web ve mobilin aynı görsel dili konuşmasını sağlayan sağlam bir temel. Bu spec o temeli kurar:
açık/koyu görünüm, okulun kendi marka rengi, tutarlı tipografi/boşluk/ölçü ölçekleri ve kullanıcının kapatabileceği
hareket (animasyon) dili. Başarı: kullanıcı görünümünü ve hareket tercihini seçebilir, seçimi kalıcıdır; bir kurum rengi
verildiğinde arayüz o renge bürünür ama okunabilirlik (WCAG AA) hiç bozulmaz; tasarım vitrini ekranı tüm bunları tek
yerde gösterir. Bileşen kütüphaneleri (spec 0003 web, 0004 mobil) bu temelin üzerine gelir; burada yapılmaz.

## Requirements
1. Kullanıcı görünümü üç seçenekten biri olarak belirler: Sistem, Açık, Koyu. Varsayılan Sistem'dir; Sistem seçiliyken cihazın ayarı değiştiğinde arayüz yeniden başlatmadan uyar.
2. Kullanıcı hareket tercihini üç seçenekten biri olarak belirler: Sistem, Azaltılmış, Normal. Varsayılan Sistem'dir ve cihazın "hareketi azalt" ayarını izler. Azaltılmış'ta hiçbir animasyon veya geçiş oynamaz; durum değişiklikleri anında gösterilir.
3. Görünüm ve hareket tercihleri o cihazda kalıcıdır; uygulama kapatılıp açıldığında korunur.
4. Arayüzdeki her metin/zemin çifti hem açık hem koyu görünümde en az WCAG AA (4.5:1; büyük metin 3:1) kontrast sağlar.
5. Bir kurum rengi verildiğinde, marka rengini kullanan tüm öğeler o renge geçer. Marka rengi üstündeki metin rengi (açık/koyu) kontrastı AA'yı sağlayacak biçimde otomatik seçilir; marka rengi metin veya çizgi olarak kullanıldığında, gerekirse zemine göre AA'yı sağlayan bir tonu kullanılır.
6. Geçersiz bir kurum rengi verilirse varsayılan marka rengi kullanılır; arayüz bozulmaz ve kullanıcıya hata gösterilmez.
7. Tipografi, boşluk, ölçü (genişlik/yükseklik), köşe yarıçapı, gölge ve hareket (süre, eğri) değerleri ortak kaynaktan gelir; ekranlarda bu kaynağın dışında görsel değer kullanılmaz.
8. Hareket ilkeleri yazılıdır: hangi durumda hareket kullanılır, süre ve eğri seçenekleri, azaltılmış harekette ne olur.
9. Tasarım vitrini ekranı renkleri (anlamsal adlarıyla), tipografi ölçeğini, boşluk/ölçü/yarıçap/gölge ölçeklerini ve hareket örneklerini gösterir; görünüm, hareket ve kurum rengi burada canlı değiştirilip önizlenebilir.
10. Durum ekranı (spec 0001) yeni temeli kullanır ve görünüm/hareket tercihlerine buradan da ulaşılabilir.
11. Uygulamanın kurum rengi yapılandırmadan gelir (web ve mobil için ayrı ayar); vitrinde girilen renk yalnızca önizlemedir, kalıcı değildir.
12. Arayüz yazı tipi Inter'dir ve uygulamalarla birlikte gelir; cihazda yüklü olmasına bağlı değildir.

## User experience
- **Flow — görünüm/hareket:** Durum ekranı veya tasarım vitrini → başlıktaki "Görünüm" düğmesi → panel açılır: Görünüm (Sistem / Açık / Koyu), Hareket (Sistem / Azaltılmış / Normal) → seçim anında uygulanır → panel kapatılır (dışarı tıklama, Esc, kapat düğmesi; mobilde geri hareketi).
- **Flow — kurum rengi:** Tasarım vitrini → "Kurum rengi" alanına renk girilir (#RRGGBB) → önizleme anında güncellenir → "Varsayılana dön" yapılandırmadaki kurum rengine (yoksa varsayılan marka rengine) döndürür. Vitrin girişi kalıcı değildir.
- **Erişim:** Vitrin web'de `/tasarim` adresinde, mobilde durum ekranındaki "Tasarım vitrini" bağlantısıyla açılır; giriş sistemi gelene kadar her build'de erişilebilir (kısıtlama: B-10).
- **States:**
  - Görünüm paneli: kapalı, açık; her seçeneğin seçili/seçili değil hâli.
  - Kurum rengi alanı: boş (varsayılan renk), geçerli renk (önizleme), geçersiz giriş (alan altında "Geçerli bir renk girin (ör. #1E40AF)" ve varsayılan renk korunur).
  - Vitrin ve durum ekranı: açık görünüm, koyu görünüm, kurum renkli; azaltılmış harekette animasyonsuz.
  - Boş / yükleniyor / yetkisiz: uygulanamaz (veri çekilmiyor, giriş yok).
- **Design reference:** none — the flow above and the rules in Requirements are the design. Görsel yön SHIP'te ekran görüntüsü galerisiyle onaylanır; ürün sahibi plan onayından önce bir tasarım bağlantısı (sabit sürüm) verirse o esas alınır.
- **Accessibility:** Panel ve seçenekler klavyeyle tam kullanılır (Tab, ok tuşları, Enter/Space, Esc); seçenek grupları ekran okuyucuya grup adı ve seçili değerle duyurulur; panel açılınca odak panele girer, kapanınca açan düğmeye döner; renk alanının hatası ekran okuyucuya duyurulur; kontrast Requirement 4.

## Constraints & out of scope
- Sensitive area: none.
- Test kararı ADR 0003: yalnızca unit test; görsel sonuç SHIP'te ekran görüntüsüyle onaylanır.
- Split: proposed 0002 (tasarım temelleri) → 0003 (web bileşen kütüphanesi) → 0004 (mobil bileşen kütüphanesi) — approved, yunusemretapan, 2026-11-17.
- Backlog'tan kapananlar: B-5 (token dışı ölçüler, Tailwind varsayılan paleti), B-9 (çalışma zamanı hareket anahtarı).
- Yazı tipi: Inter (SIL Open Font License 1.1), uygulamalara gömülü.
- Mobilde yerel dönen gösterge hareket azaltıldığında durdurulamıyorsa yerine sabit bir gösterge kullanılır (AC-2).
- **Out of scope:** bileşen kütüphaneleri (Dialog, Toast, Tabs…; 0003/0004), kurum rengini sunucudaki kurum ayarından okumak (kurum modülü yok), tenant başına logo/uygulama adı (Faz 3), Inter dışında yazı tipi, sağdan sola diller, yüksek kontrast modu, vitrine erişimin kısıtlanması (B-10).

## Acceptance criteria
- [ ] AC-1 — Görünüm seçenekleri Sistem/Açık/Koyu'dur; varsayılan Sistem'dir; Sistem seçiliyken cihaz ayarı değiştiğinde web ve mobil arayüz yeniden başlatmadan uyar; Açık/Koyu seçildiğinde cihaz ayarı yok sayılır.
- [ ] AC-2 — Hareket seçenekleri Sistem/Azaltılmış/Normal'dir; varsayılan Sistem cihazın "hareketi azalt" ayarını izler; Azaltılmış (veya Sistem + cihaz azalt) iken web ve mobilde hiçbir animasyon/geçiş oynamaz.
- [ ] AC-3 — Görünüm ve hareket seçimi web ve mobilde uygulama yeniden açıldığında korunur.
- [ ] AC-4 — Açık ve koyu görünümün tüm anlamsal metin/zemin çiftleri en az 4.5:1 (büyük metin ve çizgi 3:1) kontrast sağlar.
- [ ] AC-5 — Geçerli bir kurum rengi verildiğinde marka öğeleri o rengi kullanır; marka üstü metin ve marka rengiyle yazılan metin her iki görünümde en az 4.5:1 sağlar (açık sarı, koyu lacivert ve orta gri gibi uç renkler dahil).
- [ ] AC-6 — Geçersiz kurum rengi (biçim hatası, boş) varsayılan marka rengini kullandırır; vitrin alanında "Geçerli bir renk girin (ör. #1E40AF)" görünür ve ekran okuyucuya duyurulur; "Varsayılana dön" varsayılanı geri getirir.
- [ ] AC-7 — Durum ekranında ve vitrinde token kaynağı dışında görsel değer (renk, ölçü, süre) yoktur ve Tailwind'in varsayılan renk paleti kullanılamaz; bunu otomatik doğrulama yakalar.
- [ ] AC-8 — Hareket ilkeleri `docs/` altında yazılıdır ve tüm animasyon süreleri/eğrileri hareket token'larından gelir.
- [ ] AC-9 — Tasarım vitrini web ve mobilde renkleri, tipografi ölçeğini, boşluk/ölçü/yarıçap/gölge ölçeklerini ve hareket örneklerini açık, koyu ve kurum renkli hâllerde gösterir.
- [ ] AC-10 — Görünüm paneli klavyeyle tam kullanılır (Tab, ok tuşları, Enter/Space, Esc), açılınca odak panele girer, kapanınca açan düğmeye döner; seçenek grupları ekran okuyucuya ad ve seçili değerle duyurulur; mobilde aynı paneli ekran okuyucu ve geri hareketiyle kullanmak mümkündür.

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
