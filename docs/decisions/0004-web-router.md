# ADR 0004 — Web yönlendirme: React Router 7 (bildirimsel mod)

- Status: Accepted
- Date: 2026-11-19

## Context
Spec 0002'ye kadar web uygulamasında yalnızca iki ekran vardı ve `pathname` ayrımı yetiyordu; plan 0002 (R6) yönlendirme
kütüphanesi kararını spec 0003'e bıraktı. Spec 0003 giriş ekranı, korumalı adresler, girişten sonra istenen adrese geri
dönüş ve "çıkıştan sonra Geri tuşu korumalı ekranı göstermez" davranışı istiyor (AC-9, AC-11). Bunlar geçmiş (history)
yönetimi, yönlendirme ve durum taşıma gerektirir; elle yazmak hata ve test yükü demek.

## Decision
Web uygulaması `react-router` 7'yi **bildirimsel modda** (`BrowserRouter` + `Routes`) kullanır. Korumalı ekranlar
`RequireAuth` ile sarılır; yönlendirme `replace` ile yapılır, dönülecek adres yönlendirme durumunda (`state.from`) taşınır.
Testler aynı rotaları `MemoryRouter` ile çalıştırır. Mobil uygulama gezinme kütüphanesi kullanmaz (durum değişkeni; plan 0002).

## Consequences
- Kazanç: Yönlendirme, geri dönüş ve geçmiş davranışı tek yerde, bileşen testleriyle kanıtlanabilir; yeni modül ekranları
  yalnızca bir `Route` satırı ekler.
- Maliyet: Yeni bağımlılık (MIT). 8.x sürümü de yayımlanmış durumda; 7.x bakımı sürüyor (7.18.6, 2026-11-18). Veri
  yükleyici (data/framework) modu bilinçli olarak kullanılmıyor: sunucu verisi TanStack Query'de kalır
  (docs/conventions.md, Known traps).

## Alternatives considered
- **Elle `pathname` + `history.pushState`:** Bağımlılıksız, ama geri dönüş, `replace` ve Geri tuşu davranışını kendimiz
  yazar ve test ederiz; ekran sayısı arttıkça büyür.
- **TanStack Router:** Tip güvenli rotalar; ekip için yeni bir model ve daha büyük bir API yüzeyi; şu anki ihtiyaç için fazla.
- **React Router 8:** Plan 0003 onayı 7'yi kapsıyordu; 8'e geçiş ayrı bir sürüm yükseltme spec'i olur
  (docs/conventions.md, sürüm yükseltmesi kendi spec'iyle).

## Revisit triggers
- 7.x bakımının durması veya güvenlik açığı.
- Rotalarda veri yükleme ya da sunucu tarafı render ihtiyacı.
