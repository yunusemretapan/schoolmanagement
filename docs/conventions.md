# Conventions

> Yalnızca gerçek kurallar: her kural tooling ile zorlanır (tercih edilen) veya review'da kontrol edilir.

## Language
- Chat language (interviews, sessions): Türkçe (`tr`)
- Document language (docs/, specs, plans, ADRs, review and verify reports): Türkçe (`tr`)
- Always English (protocol, not prose): ANEW core files, template headings and field labels,
  `Status:` values (Draft / Approved / In progress / Shipped), `Approved by / on:`, `Source:`.
- Code identifiers, branch names, commit messages: İngilizce. Türkçe alan terimi ↔ kod adı eşlemesi `docs/domain.md`'de.
- Agent messages (handoff summaries, refusals, `Next:` lines): chat language; command names, file
  paths and protocol fields verbatim.
- Kullanıcıya görünen metinler: i18n dosyalarında (önce `tr-TR`), koda gömülmez.

## Language & framework versions
Sabitlendiği yer: `backend/pom.xml`, `frontend/**/package.json` + `package-lock.json`, `frontend/.nvmrc` (spec 0001).
- Backend: **Java 25 LTS**, **Spring Boot 4.1.2** (`spring-boot-starter-webmvc`), Maven 3.9.16 (wrapper `mvnw` / `mvnw.cmd`),
  Error Prone 2.52, Spotless 3.11 + Palantir Java Format 2.103. Spec 0003: Spring Data JPA (Hibernate 7.4), Flyway 12.4
  (+ `flyway-database-postgresql`), Spring Security 7.1 (+ `spring-security-oauth2-jose`: Nimbus JWT), Bean Validation,
  PostgreSQL JDBC 42.7 — hepsi Boot 4.1.2 BOM'undan; Bouncy Castle `bcprov-jdk18on` 1.87 (Argon2id). JobRunr: ilk ihtiyaç duyan spec ekler.
- Veritabanı: **PostgreSQL 18** (spec 0003; yerelde `backend/compose.yaml`).
- Frontend: **Node 26 LTS**, **TypeScript 6.0** (7.x `typescript-eslint` ile uyumsuz), **React 19.3.0**, Vite 8, Tailwind CSS 4.3,
  TanStack Query 5, i18next 27; test: Vitest 5 + Testing Library. Radix UI 1.8.1 (`radix-ui`), Inter (`@fontsource-variable/inter`
  5.3.0, OFL-1.1) — spec 0002. Spec 0003: React Router 7.18.6 (ADR 0004), Zod 4.6.7 (`@schoolos/auth`). Motion: ilk ihtiyaç duyan spec ekler.
- Mobil: **Expo SDK 58** (React Native 0.88.1, React 19.3.0); test: jest-expo 58 + React Native Testing Library 14.
  Spec 0002: Reanimated 4.7.1 + Worklets 0.13.0, AsyncStorage 2.2.0, `expo-font` / `expo-splash-screen` ~58.0,
  Inter (`@expo-google-fonts/inter` 0.4.2, OFL-1.1). Spec 0003: `expo-secure-store` ~58.0.3. Gesture Handler, Skia: ilk ihtiyaç duyan spec ekler; sürüm her zaman
  Expo'nun `bundledNativeModules` listesinden.
- AI: Claude API (`claude-sonnet-5-5`) + ayrı bir STT servisi (spec'te seçilir).
- Sürüm yükseltmesi kendi spec'iyle yapılır; Expo paketleri `npx expo install` ile SDK'nın izin verdiği sürüme.

## Naming
- Java: `CreateStudentCommand`, `GetStudentByIdQuery`, `StudentBusinessRules`, `StudentResponse`, `CreateStudentCommandValidator`.
  Paket: `<root>.application.features.<modül>.{commands,queries,rules,dtos,mappers,constants}`.
- Veritabanı: `snake_case`, çoğul tablo adı (`students`, `installments`), her tenant tablosunda `tenant_id`.
- REST: `/api/v1/<kebab-case-kaynak>`, sürümlü.
- Hata kodu: `<MODÜL>_<DURUM>` (ör. `BILLING_INSTALLMENT_ALREADY_PAID`), değişmez sabit.
- Frontend: bileşen `PascalCase.tsx`, hook `useXxx`, test `*.test.ts(x)`; Java testleri `*Test.java`.

## Error handling
- API hataları **RFC 9457 ProblemDetails**. Tek global handler: `BusinessException` → 422 (veya kurala göre 409),
  `ValidationException` (alan bazlı) → 400, `AuthorizationException` → 403, `NotFoundException` → 404.
- Kullanıcıya stack trace, SQL, iç kimlik veya başka kurumun verisi dönmez; yalnızca hata kodu + alanlar.
  Türkçe mesaj frontend'de koddan üretilir.
- Log'larda kişisel veri yok; yalnızca ID'ler (öğrenci adı, TC no, sağlık notu log'a yazılmaz).

## Data rules
- **Para:** `Money` değer nesnesi, `long` kuruş + `TRY`. `double`/`float`/`BigDecimal` ile para yasak (BR-10).
  API biçimi: `{ "amount": 125050, "currency": "TRY" }`.
- **Zaman:** DB'de `timestamptz`, UTC. Domain'de `Instant` / `ZonedDateTime`; `LocalDateTime` yasak.
  Kurum saat dilimi (`Europe/Istanbul`) gösterim ve iş kuralı hesaplarında. Saat her zaman enjekte edilen `Clock`'tan.
- **Kimlik:** UUID v7. Sıralı tam sayı ID dışarı açılmaz (istisna: makbuz numarası).
- **Silme:** Kişisel veri soft-delete + saklama süresi sonunda anonimleştirme (BR-12); tahsilat asla silinmez (BR-9).
- **Hassas alanlar** (sağlık notu, alerji, TC kimlik no) uygulama seviyesinde şifrelenir.

## Design rules
Spec 0002; ayrıntı: `docs/design/motion.md`.
- Görsel değerler yalnızca `@schoolos/design-tokens`'tan: ekranlar anlamsal rolleri (`semantic.light/dark`) ve ölçekleri
  kullanır; renk, ölçü, süre değeri yazılmaz (`frontend-design-lint` adımı). Web'de Tailwind'in varsayılan paleti kapalı
  (`--color-*: initial`); token sınıfı (`p-md`, `size-logo`) veya `var(--sos-…)`.
- Mobilde tema canlıdır: `useAppearance().theme` + `useMemo(() => createStyles(theme), [theme])`; modül düzeyinde
  `StyleSheet` tema değeri okumaz. Yazı stili `theme.text(adım, ağırlık)` (her ağırlık ayrı Inter yüzü).
- Kurum rengi her zaman `deriveBrand` üzerinden (marka üstü metin ve marka metni ≥ 4.5:1); ham kurum rengi metin rengi olmaz.
- Görünüm ve hareket tercihi `AppearanceProvider`'dan okunur; animasyon kuralları `docs/design/motion.md`.

## Known traps
- JPA lazy loading / N+1 — controller'a entity değil DTO döner, `spring.jpa.open-in-view=false` — from bootstrap
- Hibernate tenant filtresi native sorguda çalışmaz — native SQL yasak; zorunluysa spec'te gerekçe + elle `tenant_id` koşulu — from bootstrap
- `@Transactional` aynı sınıf içi çağrıda etkisiz — transaction yalnızca `TransactionBehavior`'da, serviste `@Transactional` yok — from bootstrap
- `LocalDateTime` zaman dilimi taşımaz — `Instant` / `ZonedDateTime` kullan — from bootstrap
- `BigDecimal.equals` ölçeğe bakar (`1.0` ≠ `1.00`), `double` yuvarlar — para yalnızca `Money` (long kuruş) — from bootstrap
- Türkçe İ/I: `toUpperCase()` locale'e bağlı — kod içi karşılaştırmada `Locale.ROOT`, gösterimde `tr-TR` — from bootstrap
- RN animasyonu JS thread'de `setState` ile — Reanimated worklet kullan; native modül gerekiyorsa Expo development build — from bootstrap
- React 19'da `useEffect` içinde veri çekme — sunucu verisi TanStack Query ile — from bootstrap
- `@ExceptionHandler(Exception.class)` Spring MVC'nin kendi 4xx istisnalarını da (405, 406, 400) yakalayıp 500'e çevirir — `ErrorResponse` uygulayan istisnayı kendi durum koduyla ve `getHeaders()` ile (`ResponseEntity`; düz `ProblemDetail` 405 `Allow` / 415 `Accept` başlığını düşürür) döndür; 4xx'i ERROR'a yazma, 5xx'i yaz (`GlobalExceptionHandler`) — from spec 0001 review 1 L-2, review 2 Low-1/Low-2 (A-3, A-5)
- Tailwind v4'te `--spacing-sm/md/lg/xl` token adları `max-w-sm/md/lg/xl` sınıflarını ezer (`max-w-md` → 16px); jsdom testleri yakalamaz — genişlik için `--container-*` token'ı ve `max-w-<ad>` kullan — from spec 0001 fix-1 F-1 (A-4)
- npm workspace'te Vite (`envDir` varsayılanı) ve Expo `.env*` dosyalarını uygulamanın kendi klasöründen okur, `frontend/` kökünden değil — `.env.example` / `.env.local` her uygulamanın klasöründe — from spec 0001 review 2 Low-3 (A-5)
- `@expo-google-fonts/<aile>` paket girişinden içe aktarmak ailenin bütün yüzlerini (Inter: 18, ~6 MB) pakete koyar — ağırlık başına alt yol (`@expo-google-fonts/inter/600SemiBold`) + `expo-font`'un `useFonts`'u — from spec 0002 build step 5
- Reanimated stil değişikliği RNTL'de `props.style`'a yansımaz — Jest'te `getAnimatedStyle(element)` ile oku, sahte zamanlayıcıyla ilerlet; Jest `resolver: react-native-reanimated/jest/resolver` — from spec 0002 build step 5-6
- RN'de `role` taşıyan ama `accessible` olmayan `View` RNTL'nin `getByRole`'üne görünmez; grubu `accessible` yapmak ise VoiceOver'da çocuklarını gizler — kapsayıcıyı testID ile bul, rol ve adını prop olarak doğrula — from spec 0002 build step 5
- Expo `app.json` `userInterfaceStyle: "light"` yerel build'de `useColorScheme()`'i hep 'light'a kilitler; Jest sahtesi bunu yakalamaz — tema destekleyen uygulamada `"automatic"` + `expo-system-ui` (Android) ve `app.json`'u okuyan test — from spec 0002 review 1 H-1 (A-4)
- Tailwind v4 `transition-*` ve Reanimated `withTiming` / `FadeIn` gibi builder'lar eğri ya da süre verilmezse kendi varsayılanını kullanır (`cubic-bezier(0.4, 0, 0.2, 1)`, `Easing.inOut(quad)`, 300 ms); değer yazılmadığı için regex lint görmez — varsayılanı kaynağında kapat: web `index.css` kilitleri + `--default-transition-*` token'a bağlı, mobil zamanlama yalnızca `src/motion/timing` üzerinden — from spec 0002 review 3 M-1 (A-7)
- PostgreSQL süper kullanıcısı (ve `BYPASSRLS` rolü) Row-Level Security'yi `FORCE` olsa bile atlar; Docker imajının `POSTGRES_USER`'ı süper kullanıcıdır — uygulama `schoolos_app` gibi süper olmayan bir rolle bağlanır (`backend/compose.yaml`) — from spec 0003 build step 1
- Hibernate filtresi `EntityManager.find` (ve Spring Data `findById`) için çalışmaz; tenant'a bağlı okuma JPQL ile yapılır, RLS ikinci hat — from spec 0003 build step 1
- Spring Security'de `CorsConfigurationSource` parametre olarak enjekte edilirse Spring MVC'nin `mvcHandlerMappingIntrospector` bean'i de aday olur ve başlatma çöker — `.cors(Customizer.withDefaults())` + `corsConfigurationSource` adlı bean — from spec 0003 build step 5
- Dönen refresh token'la iki paralel yenileme, ikincisini "yeniden kullanım" yapar ve oturumu iptal ettirir — istemci yenilemeyi tek uçuşta (single-flight) yapar (`@schoolos/api-client`) — from spec 0003 build step 6
- RNTL 14'te `act` asenkrondur; senkron `act(() => …)` sonrası durum güncellenmemiş olabilir — `await act(async () => …)` — from spec 0003 build step 8
- Windows satır sonları — `.gitattributes` ile LF; `mvnw` ve `mvnw.cmd` ikisi de commit edilir — from bootstrap; Windows'tan eklenen `mvnw` git'e çalıştırılamaz (100644) girer ve Linux CI `Permission denied` verir — `git update-index --chmod=+x backend/mvnw` — from spec 0002 SHIP (A-9)

## Enforced by tooling
Walking skeleton'da devreye girer (`scripts/check.conf`):
- Java format: Spotless + Palantir Java Format; statik analiz: Error Prone.
- Katman sınırları: Maven modül grafiği, derleme anında (`docs/architecture.md`, Forbidden dependencies 1–3).
- TypeScript: `strict: true`, ESLint (`no-explicit-any`, `no-restricted-imports` → kural 6) + Prettier.
