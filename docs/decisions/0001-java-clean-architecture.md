# ADR 0001 — Backend: Java + Spring Boot, Clean Architecture (nArchitecture düzeni)

- Status: Accepted
- Date: 2026-11-17

## Context
`school.md` altyapı olarak nArchitecture'ı (Kodlama.io'nun .NET clean architecture şablonu) gösteriyor.
Ürün sahibi backend dilini Java, veritabanını PostgreSQL olarak belirledi ve nArchitecture yapısının
korunmasını istedi. Ekip küçük; MVP hızı ve hassas alanlarda (tenant, ödeme, KVKK) net sınırlar gerekli.

## Decision
Backend Java 25 LTS + Spring Boot 4 ile, Maven multi-module olarak Clean Architecture düzeninde kurulur:
`core-*` (nArchitecture `Core.*` karşılığı) + `domain` / `application` / `persistence` / `infrastructure` / `webapi`.
CQRS için küçük, projeye ait bir Mediator + `PipelineBehavior` zinciri yazılır (dış kütüphane yok).

## Consequences
- Kazanç: nArchitecture'ın tanıdık klasör düzeni ve pipeline yaklaşımı korunur; katman sınırları Maven modül
  grafiğiyle derleme anında zorlanır; tek dağıtılabilir birim.
- Maliyet: .NET şablonunun hazır kodu kullanılamaz; Core katmanı Java'da yeniden yazılır (walking skeleton ve
  ilk feature'larda zaman). Mediator'ın bakımı bize ait.

## Alternatives considered
- **.NET 10 + nArchitecture olduğu gibi:** Ürün sahibi Java'yı seçti.
- **Spring Modulith:** Modül sınırları için iyi, ancak nArchitecture'ın katman-öncelikli düzeniyle çakışıyor.
- **Hazır Java mediator kütüphanesi (ör. PipelinR):** Küçük bir kod için dış bağımlılık; kontrol bizde kalsın.

## Revisit triggers
- Bir modülün bağımsız ölçeklenmesi gerekirse (ayrı servis).
- Mediator kodu bakım yükü üretmeye başlarsa.
