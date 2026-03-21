---
id: docs-index
title: Документация Mutaktor
sidebar_label: Индекс
---

# Документация Mutaktor

![Version](https://img.shields.io/badge/version-0.2.0-7F52FF?style=for-the-badge)
![Kotlin](https://img.shields.io/badge/Kotlin-2.3-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-9.4.1-02303A?style=for-the-badge&logo=gradle&logoColor=white)
![PIT](https://img.shields.io/badge/PIT-1.23.0-E37400?style=for-the-badge)
![JDK](https://img.shields.io/badge/JDK-17%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Tests](https://img.shields.io/badge/tests-135-brightgreen?style=for-the-badge)
![License](https://img.shields.io/badge/License-Apache_2.0-blue?style=for-the-badge)
![Lang-Русский](https://img.shields.io/badge/Lang-Русский-blue?style=for-the-badge)

> Также доступно на: [English](../en/README.md)

**Mutaktor** — это Kotlin-first плагин для Gradle, предназначенный для мутационного тестирования с помощью [PIT](https://pitest.org/). Он оборачивает проверенный движок мутаций PIT возможностями ограничения области анализа через git, фильтрацией «мусорных» мутаций Kotlin, полностью настроенным конвейером постобработки (JSON, SARIF, quality gate, ratchet, GitHub Checks) и автоопределением GraalVM — без каких-либо внешних зависимостей во время выполнения.

---

## Что такое мутационное тестирование?

Мутационное тестирование проверяет качество набора тестов путём систематического введения небольших изменений — **мутантов** — в исходный код и верификации того, что хотя бы один тест завершается ошибкой для каждого изменения. Мутант, которого ни один тест не обнаружил, называется **выжившим мутантом**: это пробел в покрытии тестами, который не выявляется обычным покрытием по строкам кода.

**Оценка мутаций** — это процент мутантов, которые были уничтожены:

```
оценка мутаций = (уничтоженные мутанты / всего мутантов) × 100
```

---

## Основные возможности v0.2.0

| Возможность | Описание |
|-------------|----------|
| Конвейер постобработки | JSON + SARIF + quality gate + ratchet + GitHub Checks, встроенные в `exec()` |
| `mutationScoreThreshold` | Завершает сборку с ошибкой, если оценка мутаций опускается ниже заданного процента |
| `jsonReport` / `sarifReport` | Первоклассные свойства DSL для включения форматов отчётов |
| Покомпонентный ratchet | `ratchetEnabled`, `ratchetBaseline`, `ratchetAutoUpdate` предотвращают регрессию оценки |
| Аннотация `@MutationCritical` | Помечает код, который должен достичь 100% оценки мутаций |
| Аннотация `@SuppressMutations` | Исключает конкретные методы или классы из анализа |
| Модуль `mutaktor-annotations` | Отдельный JAR с аннотациями без зависимости от Gradle |
| Автоопределение GraalVM | `GraalVmDetector` переключает PIT на стандартный JDK при сборке под GraalVM + Quarkus |
| Свойство `javaLauncher` | Полная интеграция с Gradle Toolchain API для дочернего JVM процесса PIT |
| Защита от пустого `targetClasses` | Завершается с понятным сообщением, если классы не настроены |

---

## Карта документации

```mermaid
graph TD
    INDEX["README.md\nИндекс документации"]

    subgraph "Начало работы"
        G01["01-architecture.md\nАрхитектура плагина"]
        G02["02-configuration.md\nПолный справочник DSL"]
    end

    subgraph "Основные возможности"
        F03["03-kotlin-filters.md\nФильтр мусорных мутаций Kotlin"]
        F04["04-git-integration.md\nАнализ в рамках git-diff"]
        F05["05-reporting.md\nОтчёты и Quality Gate"]
    end

    subgraph "Разработка и эксплуатация"
        D06["06-development.md\nРуководство разработчика"]
        D07["07-ci-cd.md\nИнтеграция с CI/CD"]
        D08["08-changelog.md\nChangelog и релизы"]
    end

    INDEX --> G01
    INDEX --> G02
    INDEX --> F03
    INDEX --> F04
    INDEX --> F05
    INDEX --> D06
    INDEX --> D07
    INDEX --> D08

    G01 --> G02
    G02 --> F03
    G02 --> F04
    G02 --> F05
    F05 --> D07
    D06 --> D07
    D07 --> D08

    style INDEX fill:#7F52FF,color:#fff
    style G01 fill:#02303A,color:#fff
    style G02 fill:#02303A,color:#fff
    style F03 fill:#e37400,color:#fff
    style F04 fill:#e37400,color:#fff
    style F05 fill:#e37400,color:#fff
    style D06 fill:#2d8a4e,color:#fff
    style D07 fill:#2d8a4e,color:#fff
    style D08 fill:#2d8a4e,color:#fff
```

---

## Индекс документов

| № | Документ | Аудитория | Описание |
|---|----------|-----------|----------|
| 01 | [Архитектура](01-architecture.md) | Все пользователи | Модули плагина, поток данных, архитектура classpath, жизненный цикл |
| 02 | [Конфигурация](02-configuration.md) | Все пользователи | Полный справочник DSL: все 32 свойства, типы, значения по умолчанию, примеры |
| 03 | [Фильтр мусорных мутаций Kotlin](03-kotlin-filters.md) | Разработчики Kotlin | Как `KotlinJunkFilter` устраняет ложноположительные мутации из байткода, генерируемого компилятором |
| 04 | [Анализ в рамках git-diff](04-git-integration.md) | Пользователи CI/CD | Ограничение мутаций изменёнными классами через `since` и `GitDiffAnalyzer` |
| 05 | [Отчёты и Quality Gate](05-reporting.md) | Пользователи CI/CD | HTML, XML, SARIF, JSON, quality gate, ratchet, GitHub Checks |
| 06 | [Руководство разработчика](06-development.md) | Контрибьюторы | Команды сборки, структура проекта, соглашения, расширение плагина |
| 07 | [Интеграция с CI/CD](07-ci-cd.md) | DevOps / контрибьюторы | Рабочие процессы GitHub Actions CI и release; загрузка SARIF; настройка GitHub Checks |
| 08 | [Руководство по Changelog](08-changelog.md) | Контрибьюторы | Формат Keep a Changelog, политика SemVer, процесс выпуска релизов |

---

## Быстрый старт

### Kotlin DSL

```kotlin
// build.gradle.kts
plugins {
    kotlin("jvm") version "2.3.0"
    id("io.github.dantte-lp.mutaktor") version "0.2.0"
}

mutaktor {
    targetClasses = setOf("com.example.*")
    mutationScoreThreshold = 80          // завершить сборку ниже 80%
    since = "main"                       // мутировать только изменённые классы
    kotlinFilters = true                 // подавить шум компилятора Kotlin
    jsonReport = true                    // JSON mutation-testing-elements
    sarifReport = true                   // SARIF для GitHub Code Scanning
}
```

```bash
./gradlew mutate
```

### Groovy DSL

```groovy
// build.gradle
plugins {
    id 'org.jetbrains.kotlin.jvm' version '2.3.0'
    id 'io.github.dantte-lp.mutaktor' version '0.2.0'
}

mutaktor {
    targetClasses = ['com.example.*'] as Set
    mutationScoreThreshold = 80
    since = 'main'
    kotlinFilters = true
}
```

---

## Быстрые ссылки

### Для пользователей плагина

- **Настроить область мутаций** — [Конфигурация: targetClasses](02-configuration.md#targetclasses)
- **Мутировать только изменённый код** — [Анализ в рамках git-diff](04-git-integration.md)
- **Завершать сборку ниже порога** — [Конфигурация: mutationScoreThreshold](02-configuration.md#mutationscorethreshold)
- **Предотвратить регрессию оценки** — [Отчёты: Ratchet](05-reporting.md#per-package-ratchet)
- **Загрузить SARIF в Code Scanning** — [CI/CD: Загрузка SARIF](07-ci-cd.md#sarif-upload-to-code-scanning)
- **Интеграция с GitHub PR Checks** — [CI/CD: GitHub Checks API](07-ci-cd.md#github-checks-api)
- **Пропустить ошибки classpath GraalVM** — [Конфигурация: javaLauncher](02-configuration.md#javalauncher)
- **Пометить критичный код** — [Конфигурация: Модуль аннотаций](02-configuration.md#annotations-module)

### Для контрибьюторов

- **Настроить локальную разработку** — [Руководство разработчика: Начало работы](06-development.md#getting-started)
- **Запустить тесты** — [Руководство разработчика: Команды сборки](06-development.md#build-commands)
- **Добавить новый шаблон фильтра** — [Руководство разработчика: Добавление новых шаблонов фильтров](06-development.md#adding-new-filter-patterns)
- **Добавить новый формат отчёта** — [Руководство разработчика: Добавление новых форматов отчётов](06-development.md#adding-new-report-formats)
- **Выпустить новую версию** — [Руководство по Changelog: Процесс выпуска](08-changelog.md#release-process)

---

## Обзор архитектуры

```mermaid
graph TB
    subgraph "Сборка Gradle"
        PLG["MutaktorPlugin\napply()"]
        EXT["mutaktor { ... }\nMutaktorExtension\n32 свойства"]
        TSK["задача mutate\nMutaktorTask\n@CacheableTask"]
        GIT["GitDiffAnalyzer\nchangedClasses()"]
        GVM["GraalVmDetector\nавто-выбор JDK"]
        CFG["конфигурация mutaktor\nPIT classpath"]
    end

    subgraph "Процесс PIT (дочерний JVM)"
        PIT["PIT CLI\nMutationCoverageReport"]
        FLT["KotlinJunkFilter\nMutationInterceptor SPI\n5 шаблонов"]
        RPT["mutations.xml\nHTML отчёт"]
    end

    subgraph "Конвейер постобработки"
        JSON["MutationElementsConverter\nmutations.json"]
        SAR["SarifConverter\nmutations.sarif.json"]
        QG["QualityGate\nпороговое значение оценки"]
        RAT["MutationRatchet\nпокомпонентный нижний порог"]
        GH["GithubChecksReporter\nCheck Run + аннотации"]
    end

    PLG --> EXT
    PLG --> TSK
    PLG --> CFG
    GVM -->|"авто-выбор launcher"| TSK
    EXT -->|"цепочки Provider"| TSK
    GIT -->|"since → targetClasses"| TSK
    CFG -->|"PIT + JARы JUnit5"| TSK
    TSK -->|"JavaExec"| PIT
    FLT -->|"загружается SPI во время выполнения"| PIT
    PIT --> RPT
    RPT --> JSON
    RPT --> SAR
    RPT --> QG
    RPT --> RAT
    QG --> GH

    style PLG fill:#7F52FF,color:#fff
    style TSK fill:#02303A,color:#fff
    style PIT fill:#e37400,color:#fff
    style GH fill:#181717,color:#fff
    style RAT fill:#2d8a4e,color:#fff
```

---

## Обзор модулей

| Модуль | Plugin ID / Артефакт | Назначение |
|--------|----------------------|------------|
| `mutaktor-gradle-plugin` | `io.github.dantte-lp.mutaktor` | Применяется в проектах-потребителях; регистрирует задачу `mutate` |
| `mutaktor-gradle-plugin` | `io.github.dantte-lp.mutaktor.aggregate` | Применяется к корневому проекту; регистрирует задачу `mutateAggregate` |
| `mutaktor-pitest-filter` | PIT SPI JAR | Загружается PIT во время выполнения; фильтрует мутации, генерируемые компилятором Kotlin |
| `mutaktor-annotations` | `mutaktor-annotations.jar` | Аннотации `@MutationCritical` и `@SuppressMutations` |
| `build-logic` | внутренний | Общий конвенционный плагин для toolchain Kotlin + JVM |

---

## Требования

| Требование | Минимум | Протестировано с |
|------------|---------|-----------------|
| Gradle | 9.0 | 9.4.1 |
| JDK | 17 | 17, 21, 25 (Temurin) |
| Kotlin | 1.8+ | 2.3.0 |
| PIT | 1.19.0 | 1.23.0 |
| pitest-junit5-plugin | 1.1.0 | 1.2.3 |

> **Примечание:** GraalVM поддерживается в качестве JDK для сборки, когда настроен `javaLauncher` или автоматически обнаружен GraalVM + Quarkus. Сам PIT требует стандартного JVM HotSpot для дочернего процесса minion.

---

## Лицензия

Apache License 2.0. См. [LICENSE](../../LICENSE).
