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
- Backend: **Java 25 LTS**, **Spring Boot 4.x**, Maven 3.9 (wrapper `mvnw` / `mvnw.cmd` commit edilir), Flyway, Spring Data JPA, JobRunr.
- Veritabanı: **PostgreSQL 18**.
- Frontend: **React 19 + TypeScript (strict) + Vite**, Tailwind CSS v4, Radix UI, Motion, TanStack Query, Zod.
- Mobil: **React Native + Expo** (TypeScript), Reanimated 3, Gesture Handler, Skia.
- AI: Claude API (`claude-sonnet-5-5`) + ayrı bir STT servisi (spec'te seçilir).
- Tam sürümler walking skeleton'da sabitlenir (`pom.xml`, `package.json`, `.nvmrc`); sürüm yükseltmesi kendi spec'iyle yapılır.

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

## Known traps
- JPA lazy loading / N+1 — controller'a entity değil DTO döner, `spring.jpa.open-in-view=false` — from bootstrap
- Hibernate tenant filtresi native sorguda çalışmaz — native SQL yasak; zorunluysa spec'te gerekçe + elle `tenant_id` koşulu — from bootstrap
- `@Transactional` aynı sınıf içi çağrıda etkisiz — transaction yalnızca `TransactionBehavior`'da, serviste `@Transactional` yok — from bootstrap
- `LocalDateTime` zaman dilimi taşımaz — `Instant` / `ZonedDateTime` kullan — from bootstrap
- `BigDecimal.equals` ölçeğe bakar (`1.0` ≠ `1.00`), `double` yuvarlar — para yalnızca `Money` (long kuruş) — from bootstrap
- Türkçe İ/I: `toUpperCase()` locale'e bağlı — kod içi karşılaştırmada `Locale.ROOT`, gösterimde `tr-TR` — from bootstrap
- RN animasyonu JS thread'de `setState` ile — Reanimated worklet kullan; native modül gerekiyorsa Expo development build — from bootstrap
- React 19'da `useEffect` içinde veri çekme — sunucu verisi TanStack Query ile — from bootstrap
- Windows satır sonları — `.gitattributes` ile LF; `mvnw` ve `mvnw.cmd` ikisi de commit edilir — from bootstrap

## Enforced by tooling
Walking skeleton'da devreye girer (`scripts/check.conf`):
- Java format: Spotless + Palantir Java Format; statik analiz: Error Prone.
- Katman sınırları: Maven modül grafiği, derleme anında (`docs/architecture.md`, Forbidden dependencies 1–3).
- TypeScript: `strict: true`, ESLint (`no-explicit-any`, `no-restricted-imports` → kural 6) + Prettier.
