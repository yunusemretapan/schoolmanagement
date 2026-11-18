# Hareket ilkeleri

> Spec 0002 AC-2, AC-8. Kaynak: `frontend/packages/design-tokens/src/tokens.ts` → `motion`.
> Web ve mobil aynı kuralları izler; bu sayfa kuralları, token dosyası değerleri tutar.

## Ne zaman hareket
1. **Hareket bilgi taşır, süs değildir.** Bir durum değişikliğini (panel açıldı, seçim değişti), bir bağlantıyı
   (nereden nereye) veya süren bir işi (yükleniyor) gösterir. Bunlardan birini yapmayan animasyon eklenmez.
2. **Sonsuz döngü yalnızca meşgul göstergelerinde** (dönen halka, nabız). Başka hiçbir şey kendiliğinden tekrar etmez.
3. **Basma geri bildirimi anlıktır:** saydamlık (`opacity.pressed`) ve küçülme (`motion.pressScale`) geçiş değil,
   basılı durumun görünüşüdür.
4. **Hareket kimseyi bekletmez.** Bir işlem animasyon bitmeden de tamamlanmış sayılır; animasyon bitişine bağlı mantık yazılmaz.

## Süre ve eğri
Bütün süreler `motion.durationMs`, bütün eğriler `motion.easing` token'larından gelir; kodda süre ya da
`cubic-bezier` değeri yazılmaz (design-lint yakalar: web'de `css-unit-literal` / `tailwind-arbitrary`,
mobilde `rn-style-number`).

| Token | Kullanım |
|---|---|
| `fast` | Küçük durum değişikliği: renk, saydamlık, panelin belirmesi, seçim |
| `normal` | Orta ölçekli öğe: kartın, listenin yer değiştirmesi |
| `slow` | Ekranda uzun yol alan hareket (vitrin örneği); nadiren |
| `loop` | Meşgul göstergesinin bir turu |

| Eğri | Kullanım |
|---|---|
| `standard` | Ekranda kalan öğenin hareketi |
| `enter` | Beliren öğe (yavaşlayarak durur) |
| `exit` | Kaybolan öğe (hızlanarak çıkar) |
| `linear` | Sabit hız: yalnızca meşgul döngüleri (dönen halka) |

## Hareketi azalt
Tercih: **Sistem / Azaltılmış / Normal** (varsayılan Sistem = cihazın "hareketi azalt" ayarı). Azaltılmış iken
**hiçbir animasyon veya geçiş oynamaz**; değişiklik anında görünür, bilgi kaybolmaz (meşgul göstergesi sabit
bir halka olarak kalır).

- **Web:** `<html data-motion="on|off">` ilk boyamadan önce ayarlanır (`index.html`). Animasyon ve geçiş
  sınıfları yalnızca `motion-ok:` varyantıyla yazılır (`motion-ok:transition-opacity`, `motion-ok:animate-spin`);
  varyantsız `transition-*` / `animate-*` kullanılmaz.
- **Mobil:** `useAppearance().reducedMotion`. Animasyonlar Reanimated ile yazılır (JS thread'de `setState`
  ile animasyon yok); `reducedMotion` iken değer doğrudan atanır (`withTiming` yok), `entering` animasyonu
  verilmez. RN `Modal` her zaman `animationType="none"`; belirme Reanimated ile yapılır. Durdurulamayan yerel
  `ActivityIndicator` yerine `MotionIndicator` kullanılır.

## Test
- Testler animasyon bitişini beklemez; web testleri `data-motion`, mobil testleri `reducedMotion` ile hareketin
  oynamadığını, Reanimated'ın `getAnimatedStyle` değeriyle hareketin süreye yayıldığını doğrular
  (`DesignShowcase.test.tsx`, `MotionIndicator.test.tsx`).
- Görsel doğrulama (SHIP ekran görüntüleri) hareket azaltılmış modda alınır: kareler deterministiktir.
