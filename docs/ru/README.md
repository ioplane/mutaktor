---
id: docs-index
title: Документация Mutaktor
sidebar_label: Индекс
---

# Документация Mutaktor

![Kotlin](https://img.shields.io/badge/Kotlin-2.3-7F52FF?style=flat-square&logo=kotlin&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-9.4.1-02303A?style=flat-square&logo=gradle&logoColor=white)
![PIT](https://img.shields.io/badge/PIT-1.23.0-green?style=flat-square)
![JDK](https://img.shields.io/badge/JDK-17%2B-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![License](https://img.shields.io/badge/License-Apache_2.0-blue?style=flat-square)
![Lang](https://img.shields.io/badge/Lang-Русский-blue)

> Также доступно на: [English](../en/README.md)

**mutaktor** — это Kotlin-first Gradle-плагин для мутационного тестирования с помощью [PIT](https://pitest.org/). Он добавляет git-aware-ограничение области анализа, фильтрацию Kotlin-мусорных мутаций, стандартизированную отчётность (SARIF, mutation-testing-elements JSON) и интеграцию с GitHub CI/CD поверх проверенного мутационного движка PIT.

---

## Карта документации

```kroki-mermaid
graph TD
    INDEX["README.md\nИндекс документации"]

    subgraph "Начало работы"
        G01["01-getting-started.md\nУстановка и быстрый старт"]
        G02["02-configuration.md\nПолный справочник DSL"]
    end

    subgraph "Основные функции"
        F03["03-kotlin-filter.md\nKotlin Junk Filter"]
        F04["04-git-diff.md\nАнализ в рамках git-диффа"]
        F05["05-reports.md\nОтчёты и Quality Gate"]
    end

    subgraph "Разработка и эксплуатация"
        D06["06-development.md\nРуководство по разработке"]
        D07["07-ci-cd.md\nCI/CD-интеграция"]
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

| # | Документ | Аудитория | Описание |
|---|----------|----------|-------------|
| 01 | [Начало работы](01-getting-started.md) | Все пользователи | Установка плагина, настройка `build.gradle.kts`, первый запуск `./gradlew mutate` |
| 02 | [Конфигурация](02-configuration.md) | Все пользователи | Полный справочник DSL: все 24 свойства, типы, значения по умолчанию, примеры |
| 03 | [Kotlin Junk Filter](03-kotlin-filter.md) | Kotlin-разработчики | Как `KotlinJunkFilter` устраняет ложноположительные мутации из байткода, генерируемого компилятором |
| 04 | [Git-Diff Analysis](04-git-diff.md) | CI/CD-пользователи | Ограничение мутирования изменёнными классами с помощью свойства `since` и `GitDiffAnalyzer` |
| 05 | [Отчёты и Quality Gate](05-reports.md) | CI/CD-пользователи | HTML, XML, SARIF, mutation-testing-elements JSON; порог quality gate |
| 06 | [Руководство по разработке](06-development.md) | Контрибьюторы | Команды сборки, структура проекта, соглашения о коде, как добавлять фильтры и форматы отчётов |
| 07 | [CI/CD-интеграция](07-ci-cd.md) | DevOps / контрибьюторы | GitHub Actions CI и release-воркфлоу; загрузка SARIF; настройка GitHub Checks API |
| 08 | [Руководство по Changelog](08-changelog.md) | Контрибьюторы | Формат Keep a Changelog, политика SemVer, процесс релиза с автоматизацией на основе тегов |

---

## Быстрые ссылки

### Для пользователей плагина

- **Установить плагин** — [Начало работы](01-getting-started.md)
- **Настроить область мутирования** — [Конфигурация: targetClasses](02-configuration.md#targetclasses)
- **Мутировать только изменённый код** — [Git-Diff Analysis](04-git-diff.md)
- **Интеграция с проверками GitHub PR** — [CI/CD: GitHub Checks API](07-ci-cd.md#github-checks-api)
- **Загрузить SARIF в Code Scanning** — [CI/CD: загрузка SARIF](07-ci-cd.md#sarif-upload-to-code-scanning)
- **Завершать сборку ниже порога мутаций** — [Отчёты: Quality Gate](05-reports.md)

### Для контрибьюторов

- **Настроить локальную разработку** — [Руководство по разработке](06-development.md#getting-started)
- **Запустить тесты** — [Руководство по разработке: команды сборки](06-development.md#build-commands)
- **Добавить новый паттерн фильтрации** — [Руководство по разработке: добавление фильтров](06-development.md#adding-new-filter-patterns)
- **Добавить новый формат отчёта** — [Руководство по разработке: добавление форматов](06-development.md#adding-new-report-formats)
- **Выпустить новую версию** — [Руководство по Changelog: процесс релиза](08-changelog.md#release-process)

---

## Обзор архитектуры

```kroki-mermaid
graph TB
    subgraph "Сборка Gradle"
        PLG["MutaktorPlugin\napply()"]
        EXT["mutaktor { ... }\nMutaktorExtension\n24 properties"]
        TSK["задача mutate\nMutaktorTask\n@CacheableTask"]
        GIT["GitDiffAnalyzer\nchangedClasses()"]
        CFG["конфигурация mutaktor\nPIT classpath"]
    end

    subgraph "Процесс PIT (дочерняя JVM)"
        PIT["PIT CLI\nMutationCoverageReport"]
        FLT["KotlinJunkFilter\nMutationInterceptor SPI\n5 паттернов"]
        RPT["mutations.xml\nHTML-отчёт"]
    end

    subgraph "Пост-обработка"
        SAR["SarifConverter\nSARIF 2.1.0"]
        JSON["MutationElementsConverter\nmutation-testing-elements"]
        QG["QualityGate\nпорог score"]
        GH["GithubChecksReporter\nCheck Run + аннотации"]
    end

    PLG --> EXT
    PLG --> TSK
    PLG --> CFG
    EXT -->|"Provider chains"| TSK
    GIT -->|"since → targetClasses"| TSK
    CFG -->|"PIT + JUnit5 JARs"| TSK
    TSK -->|"JavaExec"| PIT
    FLT -->|"SPI загружается во время выполнения"| PIT
    PIT --> RPT
    RPT --> SAR
    RPT --> JSON
    RPT --> QG
    QG --> GH

    style PLG fill:#7F52FF,color:#fff
    style TSK fill:#02303A,color:#fff
    style PIT fill:#e37400,color:#fff
    style GH fill:#181717,color:#fff
```

---

## Обзор модулей

| Модуль | Plugin ID | Назначение |
|--------|-----------|---------|
| `mutaktor-gradle-plugin` | `io.github.dantte-lp.mutaktor` | Применяется к проектам потребителей; регистрирует задачу `mutate` |
| `mutaktor-gradle-plugin` | `io.github.dantte-lp.mutaktor.aggregate` | Применяется к корневому проекту; регистрирует задачу `mutateAggregate` |
| `mutaktor-pitest-filter` | _(PIT SPI)_ | Загружается PIT во время выполнения; фильтрует мутации, генерируемые компилятором Kotlin |
| `build-logic` | _(внутренний)_ | Convention-плагин для общего Kotlin + JVM toolchain |

---

## Требования

| Требование | Минимум | Протестировано с |
|-------------|---------|-------------|
| Gradle | 9.0 | 9.4.1 |
| JDK | 17 | 17, 21, 25 (Temurin) |
| Kotlin | 1.8+ | 2.3.0 |
| PIT | 1.19.0 | 1.23.0 |
| pitest-junit5-plugin | 1.1.0 | 1.2.3 |

---

## Лицензия

Apache License 2.0. См. [LICENSE](../../LICENSE).
