---
id: architecture
title: Архитектура плагина
sidebar_label: Архитектура
---

# Архитектура плагина

![Kotlin](https://img.shields.io/badge/Kotlin-2.3-7F52FF?logo=kotlin&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-9.4.1-02303A?logo=gradle&logoColor=white)
![PIT](https://img.shields.io/badge/PIT-1.23.0-orange)
![JDK](https://img.shields.io/badge/JDK-17%2B-blue?logo=openjdk&logoColor=white)
![Lang](https://img.shields.io/badge/Lang-Русский-blue)

## Обзор

Mutaktor — это Kotlin-first Gradle-плагин для мутационного тестирования с помощью [PIT](https://pitest.org/). Он оборачивает командную строку PIT в полностью ленивую Gradle-задачу, совместимую с кэшем конфигурации, добавляет фильтрацию Kotlin-специфичных мусорных мутаций, анализ в рамках git-диффа и несколько форматов вывода отчётов, включая SARIF для GitHub Code Scanning.

Плагин состоит из двух публикуемых модулей и вспомогательного модуля build-logic:

| Модуль | Артефакт | Назначение |
|---|---|---|
| `mutaktor-gradle-plugin` | `io.github.dantte-lp.mutaktor` | Gradle-плагин, DSL-расширение, задача `mutate`, конвертеры отчётов |
| `mutaktor-pitest-filter` | companion JAR на classpath PIT | `MutationInterceptor` PIT, фильтрующий Kotlin-мусорные мутации, генерируемые компилятором |
| `build-logic` | только внутренний | Convention-плагины для общей конфигурации Kotlin и публикации |

## Структура модулей

```
mutaktor/
├── mutaktor-gradle-plugin/
│   └── src/main/kotlin/io/github/dantte_lp/mutaktor/
│       ├── MutaktorPlugin.kt          # Plugin entry point
│       ├── MutaktorExtension.kt       # DSL extension (25 properties)
│       ├── MutaktorTask.kt            # JavaExec task, builds PIT CLI args
│       ├── MutaktorAggregatePlugin.kt # Multi-module report aggregation
│       ├── git/
│       │   └── GitDiffAnalyzer.kt     # git diff → FQN class patterns
│       ├── extreme/
│       │   └── ExtremeMutationConfig.kt  # Method-body removal mutators
│       └── report/
│           ├── MutationElementsConverter.kt  # XML → Stryker Dashboard JSON
│           ├── SarifConverter.kt             # XML → SARIF 2.1.0
│           ├── QualityGate.kt                # Mutation score threshold check
│           └── GithubChecksReporter.kt       # GitHub Checks API annotations
└── mutaktor-pitest-filter/
    └── src/main/kotlin/io/github/dantte_lp/mutaktor/pitest/
        └── KotlinJunkFilter.kt        # MutationInterceptorFactory + filter
```

## Поток данных

Следующая диаграмма показывает, как конфигурация проходит от скрипта сборки через PIT обратно в конвертеры отчётов.

```kroki-mermaid
flowchart TD
    A["build.gradle.kts\n(mutaktor { ... })"] --> B[MutaktorExtension\n25 Provider properties]
    B --> C{since задан?}
    C -- да --> D[GitDiffAnalyzer\ngit diff --name-only\nsinceRef..HEAD]
    D --> E[Паттерны FQN изменённых классов]
    C -- нет --> F[targetClasses из расширения]
    E --> G[MutaktorTask\nJavaExec]
    F --> G
    B --> G
    G --> H[buildPitArguments\n--targetClasses --mutators\n--reportDir ...]
    H --> I[PIT CLI\nMutationCoverageReport]
    I --> J[mutations.xml\nНативный отчёт PIT]
    I --> K[index.html\nHTML-отчёт PIT]
    J --> L[MutationElementsConverter\nStryker Dashboard JSON]
    J --> M[SarifConverter\nSARIF 2.1.0]
    J --> N[QualityGate\nПроверка mutation score]
    N --> O[GithubChecksReporter\nАннотации Check Run]
```

## Архитектура classpath

Mutaktor создаёт отдельную Gradle-конфигурацию `mutaktor` для управления classpath PIT. Это полностью изолирует зависимости PIT от собственных зависимостей проекта.

```kroki-mermaid
flowchart LR
    subgraph "конфигурация mutaktor"
        P1["org.pitest:pitest-command-line:1.23.0"]
        P2["org.pitest:pitest-junit5-plugin:1.2.3"]
        P3["mutaktor-pitest-filter (локальная зависимость)"]
    end
    subgraph "входные данные MutaktorTask"
        C1["launchClasspath\n(pitest-command-line JAR)"]
        C2["additionalClasspath\n(test runtimeClasspath)"]
        C3["mutableCodePaths\n(build/classes/kotlin/main)"]
    end
    P1 --> C1
    P2 --> C1
    P3 --> C1
```

Когда `useClasspathFile = true` (значение по умолчанию), записи `additionalClasspath` и `mutableCodePaths` записываются в `build/mutaktor/pitClasspath` (по одному пути на строку) и передаются в PIT через `--classPathFile`, что позволяет избежать ограничений длины командной строки операционной системы.

## Ключевые классы

| Класс | Пакет | Роль |
|---|---|---|
| `MutaktorPlugin` | `io.github.dantte_lp.mutaktor` | Точка входа `Plugin<Project>`; создаёт конфигурацию `mutaktor` и регистрирует задачу `mutate` с ленивой привязкой |
| `MutaktorExtension` | `io.github.dantte_lp.mutaktor` | Типобезопасный DSL; все 25 свойств используют Provider API Gradle для ленивых вычислений и совместимости с кэшем конфигурации |
| `MutaktorTask` | `io.github.dantte_lp.mutaktor` | `@CacheableTask`, расширяющий `JavaExec`; собирает список аргументов CLI PIT из значений Provider и делегирует вызов `super.exec()` |
| `MutaktorAggregatePlugin` | `io.github.dantte_lp.mutaktor` | Опциональный плагин корневого проекта; регистрирует `mutateAggregate` (задача `Copy`), собирающую отчёты подпроектов в одну директорию |
| `GitDiffAnalyzer` | `io.github.dantte_lp.mutaktor.git` | Выполняет `git diff --name-only --diff-filter=ACMR sinceRef..HEAD` и конвертирует пути файлов в glob-паттерны FQN |
| `ExtremeMutationConfig` | `io.github.dantte_lp.mutaktor.extreme` | Содержит 6 мутаторов удаления тела метода, используемых в extreme-режиме |
| `KotlinJunkFilter` | `io.github.dantte_lp.mutaktor.pitest` | `MutationInterceptor` PIT с 5 предикатами, отфильтровывающими мусорные мутации, генерируемые компилятором |
| `KotlinJunkFilterFactory` | `io.github.dantte_lp.mutaktor.pitest` | `MutationInterceptorFactory`, обнаруживаемая через `META-INF/services`; регистрирует флаг функции `KOTLIN_JUNK` |
| `MutationElementsConverter` | `io.github.dantte_lp.mutaktor.report` | Разбирает `mutations.xml` и формирует mutation-testing-elements JSON (схема Stryker Dashboard v2) |
| `SarifConverter` | `io.github.dantte_lp.mutaktor.report` | Разбирает `mutations.xml` и формирует SARIF 2.1.0; в результаты включаются только выжившие мутации |
| `QualityGate` | `io.github.dantte_lp.mutaktor.report` | Вычисляет коэффициент уничтожения мутаций и сравнивает его с порогом; возвращает типизированный `Result` |
| `GithubChecksReporter` | `io.github.dantte_lp.mutaktor.report` | Создаёт GitHub Check Run с предупреждающими аннотациями для каждой выжившей мутации через GitHub Checks API |

## Жизненный цикл применения плагина

```kroki-mermaid
sequenceDiagram
    participant G as Gradle
    participant MP as MutaktorPlugin
    participant EXT as MutaktorExtension
    participant CONF as конфигурация mutaktor
    participant TASK as задача mutate

    G->>MP: apply(project)
    MP->>EXT: project.extensions.create("mutaktor")
    note over EXT: Конвенции применяются в блоке init
    MP->>G: plugins.withType(JavaPlugin)
    G-->>MP: JavaPlugin присутствует
    MP->>CONF: configurations.create("mutaktor")
    note over CONF: defaultDependencies: PIT + JUnit5 plugin + filter JAR
    MP->>TASK: tasks.register("mutate", MutaktorTask)
    note over TASK: Ленивая привязка: все свойства берутся из Provider расширения
    TASK-->>G: mustRunAfter("test")
```

## Aggregate-плагин

Для многомодульных сборок примените aggregate-плагин к корневому проекту вместе с `mutaktor`-плагинами каждого подмодуля:

```kotlin
// root build.gradle.kts
plugins {
    id("io.github.dantte-lp.mutaktor.aggregate")
}
```

Задача `mutateAggregate` копирует `build/reports/mutaktor/` каждого подпроекта в `build/reports/mutaktor-aggregate/<subprojectName>/` и автоматически выполняется после задачи `mutate` каждого подпроекта.

## Граф задач Gradle

```kroki-mermaid
flowchart LR
    compileKotlin --> compileTestKotlin
    compileTestKotlin --> test
    test --> mutate
    mutate --> mutateAggregate
```

## Совместимость с кэшем конфигурации

Все свойства `MutaktorTask` используют Provider API Gradle (`Property`, `SetProperty`, `ListProperty`, `MapProperty`, `DirectoryProperty`, `RegularFileProperty`). Ссылки на `Project` не хранятся в полях задачи. Задача аннотирована `@CacheableTask`, а все файловые входные данные имеют соответствующие аннотации `@PathSensitive`.

## См. также

- [Справочник по конфигурационному DSL](./02-configuration.md)
- [Фильтр мусорных мутаций Kotlin](./03-kotlin-filters.md)
- [Анализ в рамках git-диффа](./04-git-integration.md)
- [Форматы отчётов и Quality Gate](./05-reporting.md)
