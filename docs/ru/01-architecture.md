---
id: architecture
title: Архитектура плагина
sidebar_label: Архитектура
---

# Архитектура плагина

![Version](https://img.shields.io/badge/version-0.2.0-7F52FF?style=for-the-badge)
![Kotlin](https://img.shields.io/badge/Kotlin-2.3-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-9.4.1-02303A?style=for-the-badge&logo=gradle&logoColor=white)
![PIT](https://img.shields.io/badge/PIT-1.23.0-E37400?style=for-the-badge)
![Config Cache](https://img.shields.io/badge/Config_Cache-compatible-brightgreen?style=for-the-badge)
![Lang-Русский](https://img.shields.io/badge/Lang-Русский-blue?style=for-the-badge)

## Обзор

Mutaktor — это Kotlin-first плагин для Gradle, предназначенный для мутационного тестирования с помощью [PIT](https://pitest.org/). Он оборачивает командную строку PIT в полностью ленивую, совместимую с configuration cache задачу Gradle, добавляет фильтрацию мусорных мутаций, специфичных для Kotlin, анализ в рамках git-diff, автоопределение GraalVM и полный конвейер постобработки (JSON, SARIF, quality gate, покомпонентный ratchet, GitHub Checks API) — без каких-либо внешних зависимостей во время выполнения.

Плагин состоит из четырёх публикуемых или сопутствующих модулей плюс общего конвенционного плагина build-logic.

---

## Структура модулей

| Модуль | Артефакт | Назначение |
|--------|----------|------------|
| `mutaktor-gradle-plugin` | `io.github.ioplane.mutaktor` | Плагин Gradle, DSL-расширение, задача `mutate`, конвертеры отчётов, ratchet, определение toolchain |
| `mutaktor-pitest-filter` | сопутствующий JAR на classpath PIT | SPI `MutationInterceptor` для PIT, фильтрующий мусорные мутации, генерируемые компилятором Kotlin |
| `mutaktor-annotations` | `mutaktor-annotations.jar` | Аннотации уровня исходного кода `@MutationCritical` и `@SuppressMutations` |
| `build-logic` | только внутренний | Конвенционные плагины для общей конфигурации Kotlin и публикации |

### Дерево исходников

```
mutaktor/
├── mutaktor-gradle-plugin/
│   └── src/main/kotlin/io/github/ioplane/mutaktor/
│       ├── MutaktorPlugin.kt              # Точка входа плагина
│       ├── MutaktorExtension.kt           # DSL-расширение (32 свойства)
│       ├── MutaktorTask.kt                # Задача JavaExec + конвейер постобработки
│       ├── MutaktorAggregatePlugin.kt     # Агрегирование отчётов multi-module
│       ├── git/
│       │   └── GitDiffAnalyzer.kt         # git diff → шаблоны FQN классов
│       ├── extreme/
│       │   └── ExtremeMutationConfig.kt   # Мутаторы удаления тела метода
│       ├── toolchain/
│       │   └── GraalVmDetector.kt         # Определение GraalVM + Quarkus
│       ├── ratchet/
│       │   ├── MutationRatchet.kt         # Покомпонентный нижний порог оценки
│       │   └── RatchetBaseline.kt         # Сохранение базового JSON
│       ├── report/
│       │   ├── MutationElementsConverter.kt  # XML → JSON mutation-testing-elements
│       │   ├── SarifConverter.kt             # XML → SARIF 2.1.0
│       │   ├── QualityGate.kt                # Проверка порогового значения оценки мутаций
│       │   └── GithubChecksReporter.kt       # Аннотации через GitHub Checks API
│       └── util/
│           ├── XmlParser.kt               # Утилиты безопасного разбора SAX/DOM
│           ├── JsonBuilder.kt             # Построение JSON без зависимостей
│           └── SourcePathResolver.kt      # Преобразование пути файла в FQN
│
├── mutaktor-pitest-filter/
│   └── src/main/kotlin/io/github/ioplane/mutaktor/pitest/
│       └── KotlinJunkFilter.kt            # MutationInterceptorFactory + 5 фильтров
│
├── mutaktor-annotations/
│   └── src/main/kotlin/io/github/ioplane/mutaktor/annotations/
│       ├── MutationCritical.kt            # Требует 100% оценки мутаций
│       └── SuppressMutations.kt           # Исключает код из анализа
│
└── build-logic/
    └── src/main/kotlin/
        └── kotlin-conventions.gradle.kts  # Общая конфигурация Kotlin + JVM toolchain
```

---

## Поток данных

Следующая диаграмма показывает, как конфигурация передаётся из `build.gradle.kts` через плагин в PIT и затем через конвейер постобработки.

```mermaid
flowchart TD
    A["build.gradle.kts\n(mutaktor { ... })"] --> B["MutaktorExtension\n32 свойства Provider"]
    B --> C{since задан?}
    C -- да --> D["GitDiffAnalyzer\ngit diff --name-only\nsinceRef..HEAD"]
    D --> E["Шаблоны FQN изменённых классов\ncom.example.Foo*"]
    C -- нет --> F["targetClasses из расширения\ncom.example.*"]
    E --> G["MutaktorTask\nJavaExec"]
    F --> G
    B --> G
    B --> GVM["GraalVmDetector\nisGraalVm() + hasQuarkus()"]
    GVM -->|"авто-выбор toolchain"| G
    G --> H["buildPitArguments()\n--targetClasses --mutators\n--reportDir ..."]
    H --> I["PIT CLI\nMutationCoverageReport\n(дочерний JVM)"]
    I --> J["mutations.xml\nРодной XML PIT"]
    I --> K["index.html\nHTML отчёт PIT"]

    J --> PP["postProcess()"]
    PP --> L["MutationElementsConverter\nmutations.json"]
    PP --> M["SarifConverter\nmutations.sarif.json"]
    PP --> N["QualityGate\nоценка >= порог?"]
    PP --> O["MutationRatchet\nпокомпонентная проверка нижнего порога"]
    N --> P["GithubChecksReporter\nCheck Run + аннотации"]
    O --> P

    style G fill:#02303A,color:#fff
    style I fill:#e37400,color:#fff
    style P fill:#181717,color:#fff
    style O fill:#2d8a4e,color:#fff
```

---

## Архитектура classpath

Mutaktor создаёт выделенную конфигурацию Gradle `mutaktor` для управления classpath PIT. Это полностью отделяет зависимости PIT от собственных зависимостей компиляции и выполнения проекта.

```mermaid
flowchart LR
    subgraph "конфигурация mutaktor (разрешается при выполнении задачи)"
        P1["org.pitest:pitest-command-line:1.23.0"]
        P2["org.pitest:pitest-junit5-plugin:1.2.3"]
        P3["mutaktor-pitest-filter (зависимость проекта или опубликованный JAR)"]
    end

    subgraph "Входные данные classpath MutaktorTask"
        C1["launchClasspath\n(pitest-command-line + filter JAR)"]
        C2["additionalClasspath\n(test runtimeClasspath)"]
        C3["mutableCodePaths\n(build/classes/kotlin/main)"]
    end

    P1 --> C1
    P2 --> C1
    P3 --> C1
    C2 -->|"--classPathFile или --classPath"| PIT["PIT CLI"]
    C3 -->|"--mutableCodePaths"| PIT
    C1 -->|"classpath исполнения"| PIT
```

Когда `useClasspathFile = true` (значение по умолчанию), записи `additionalClasspath` и `mutableCodePaths` записываются в `build/mutaktor/pitClasspath` (по одному пути в строке) и передаются через `--classPathFile`. Это позволяет избежать ограничений длины командной строки ОС в Windows и крупных монорепозиторных сборках.

---

## Конвейер постобработки

После завершения PIT выполняются пять последовательных шагов `MutaktorTask.postProcess()`. Каждый шаг защищён: если `mutations.xml` не существует (PIT не произвёл вывода или завершился с ошибкой), весь этап постобработки пропускается с предупреждением.

```mermaid
flowchart TD
    Start["PIT exec() завершён"] --> Check{"mutations.xml\nсуществует?"}
    Check -- Нет --> Warn["logger.warn: пропуск постобработки"]
    Check -- Да --> S1

    S1{"jsonReport\n== true?"} -- Да --> J["MutationElementsConverter\n→ mutations.json"]
    S1 -- Нет --> S2
    J --> S2

    S2{"sarifReport\n== true?"} -- Да --> SAR["SarifConverter\n→ mutations.sarif.json"]
    S2 -- Нет --> S3
    SAR --> S3

    S3{"mutationScoreThreshold\nзадан?"} -- Да --> QG["QualityGate.evaluate()\nоценка < порог\n→ GradleException"]
    S3 -- Нет --> S4
    QG --> S4

    S4{"ratchetEnabled\n== true?"} -- Да --> RAT["MutationRatchet.evaluate()\nрегрессия пакета\n→ GradleException"]
    S4 -- Нет --> S5
    RAT --> S5

    S5{"GITHUB_TOKEN\nGITHUB_REPOSITORY\nGITHUB_SHA заданы?"} -- Да --> GH["GithubChecksReporter\nPOST Check Run\nPATCH аннотации"]
    S5 -- Нет --> Done["Готово"]
    GH --> Done

    style QG fill:#cc3333,color:#fff
    style RAT fill:#cc3333,color:#fff
    style GH fill:#181717,color:#fff
```

---

## Ключевые классы

| Класс | Пакет | Роль |
|-------|-------|------|
| `MutaktorPlugin` | `io.github.ioplane.mutaktor` | Точка входа `Plugin<Project>`; создаёт конфигурацию `mutaktor` и регистрирует задачу `mutate` с ленивым связыванием |
| `MutaktorExtension` | `io.github.ioplane.mutaktor` | Типобезопасный DSL; все 32 свойства используют Provider API для ленивого вычисления и совместимости с configuration cache |
| `MutaktorTask` | `io.github.ioplane.mutaktor` | `@CacheableTask`, расширяющий `JavaExec`; собирает список аргументов PIT CLI из значений Provider, делегирует `super.exec()`, затем запускает конвейер постобработки |
| `MutaktorAggregatePlugin` | `io.github.ioplane.mutaktor` | Опциональный плагин корневого проекта; регистрирует `mutateAggregate` (задача `Copy`), собирающую отчёты подпроектов |
| `GitDiffAnalyzer` | `io.github.ioplane.mutaktor.git` | Выполняет `git diff --name-only --diff-filter=ACMR sinceRef..HEAD` и преобразует пути файлов в glob-шаблоны FQN |
| `GraalVmDetector` | `io.github.ioplane.mutaktor.toolchain` | Определяет комбинацию GraalVM + Quarkus; автоматически разрешает стандартный JDK через `JavaToolchainService` для дочернего процесса PIT |
| `ExtremeMutationConfig` | `io.github.ioplane.mutaktor.extreme` | Содержит 6 мутаторов удаления тела метода, используемых в экстремальном режиме |
| `KotlinJunkFilter` | `io.github.ioplane.mutaktor.pitest` | `MutationInterceptor` для PIT с 5 предикатами, отбрасывающими шумовые мутации, генерируемые компилятором |
| `KotlinJunkFilterFactory` | `io.github.ioplane.mutaktor.pitest` | `MutationInterceptorFactory`, обнаруживаемая через `META-INF/services`; регистрирует флаг фичи `KOTLIN_JUNK` |
| `MutationElementsConverter` | `io.github.ioplane.mutaktor.report` | Разбирает `mutations.xml` и генерирует JSON mutation-testing-elements (схема Stryker Dashboard v2) |
| `SarifConverter` | `io.github.ioplane.mutaktor.report` | Разбирает `mutations.xml` и генерирует SARIF 2.1.0; только выжившие мутации включаются в результаты |
| `QualityGate` | `io.github.ioplane.mutaktor.report` | Вычисляет коэффициент уничтожения и сравнивает с пороговым значением; возвращает типизированный `Result` |
| `MutationRatchet` | `io.github.ioplane.mutaktor.ratchet` | Вычисляет покомпонентные оценки из `mutations.xml`; завершается с ошибкой, если любой пакет опустился ниже базовой оценки |
| `RatchetBaseline` | `io.github.ioplane.mutaktor.ratchet` | Читает и записывает файл базовой оценки в формате JSON (`.mutaktor-baseline.json`) |
| `GithubChecksReporter` | `io.github.ioplane.mutaktor.report` | Публикует Check Run в GitHub с предупреждающими аннотациями для каждого выжившего мутанта через GitHub Checks API |

---

## Жизненный цикл применения плагина

```mermaid
sequenceDiagram
    participant G as Gradle
    participant MP as MutaktorPlugin
    participant EXT as MutaktorExtension
    participant GVM as GraalVmDetector
    participant CONF as конфигурация mutaktor
    participant TASK as задача mutate

    G->>MP: apply(project)
    MP->>EXT: project.extensions.create("mutaktor")
    note over EXT: Соглашения применяются в блоке init
    MP->>G: plugins.withType(JavaPlugin)
    G-->>MP: JavaPlugin присутствует
    MP->>CONF: configurations.create("mutaktor")
    note over CONF: defaultDependencies: PIT + JUnit5 + filter JAR
    MP->>TASK: tasks.register("mutate", MutaktorTask)
    note over TASK: Ленивое связывание: все свойства задаются из Provider расширения
    MP->>GVM: isGraalVm() && hasQuarkus(project)?
    GVM-->>MP: true / false
    alt GraalVM + Quarkus обнаружены, javaLauncher не задан
        MP->>TASK: javaLauncher.set(resolveStandardJdk(...))
    end
    TASK-->>G: mustRunAfter("test")
```

---

## Граф задач Gradle

```mermaid
flowchart LR
    compileKotlin --> compileTestKotlin
    compileTestKotlin --> test
    test -->|mustRunAfter| mutate
    mutate --> mutateAggregate
```

`mustRunAfter` (не `dependsOn`) означает, что `mutate` не запускает `test` автоматически. В большинстве рабочих процессов вы вызываете `./gradlew test mutate` или встраиваете `mutate` в шаг CI, который выполняется после тестов.

---

## Совместимость с configuration cache

Все свойства в `MutaktorTask` используют Provider API Gradle (`Property`, `SetProperty`, `ListProperty`, `MapProperty`, `DirectoryProperty`, `RegularFileProperty`, `ConfigurableFileCollection`). Ни одна ссылка на `Project` не хранится в полях задачи. Задача аннотирована `@CacheableTask`, и все файловые входные данные имеют аннотации `@PathSensitive` с соответствующим уровнем чувствительности.

| Тип Provider | Сценарий использования |
|--------------|------------------------|
| `Property<T>` | Одиночное скалярное значение (количество потоков, булевы флаги, строки) |
| `SetProperty<T>` | Неупорядоченное множество (шаблоны классов, имена мутаторов) |
| `ListProperty<T>` | Упорядоченный список (аргументы JVM, флаги фичей PIT) |
| `MapProperty<K, V>` | Пары ключ-значение (конфигурация плагинов) |
| `DirectoryProperty` | Выходная/входная директория |
| `RegularFileProperty` | Одиночный файл (файлы истории, базовая оценка, файл classpath) |
| `ConfigurableFileCollection` | Несколько файлов (исходные директории, classpath, пути к коду) |

> **Предупреждение:** Никогда не храните ссылки на `Project` в полях задачи. `Project` не сериализуется для configuration cache и вызовет промах кэша или жёсткий сбой на Gradle 9+.

---

## Отсутствие внешних зависимостей

Производственный код в `mutaktor-gradle-plugin` имеет ровно **одну** compile-зависимость: `org.pitest:pitest-command-line`. Всё остальное использует стандартную библиотеку JDK:

| Операция | Реализация |
|----------|-----------|
| HTTP-запросы | `java.net.http.HttpClient` (JDK 11+) |
| Разбор XML | `javax.xml.parsers.DocumentBuilderFactory` (SAX/DOM) |
| Генерация JSON | `StringBuilder` с ручным экранированием через `JsonBuilder` |
| Файловый ввод-вывод | `java.io.File` |
| Выполнение процессов | Тип задачи Gradle `JavaExec` |

> **Примечание:** Это ограничение намеренное. Добавление Jackson, OkHttp, Gson или любой другой сторонней библиотеки в JAR плагина увеличило бы риск конфликтов зависимостей с classpath проектов-потребителей.

---

## Агрегирующий плагин

Для multi-module сборок примените агрегирующий плагин к корневому проекту:

```kotlin
// root build.gradle.kts
plugins {
    id("io.github.ioplane.mutaktor.aggregate")
}
```

Задача `mutateAggregate` копирует `build/reports/mutaktor/` каждого подпроекта в `build/reports/mutaktor-aggregate/<subprojectName>/` и автоматически выполняется после задачи `mutate` каждого подпроекта.

```mermaid
flowchart LR
    A["subproject-a: mutate"] --> AGG["root: mutateAggregate\nbuild/reports/mutaktor-aggregate/"]
    B["subproject-b: mutate"] --> AGG
    C["subproject-c: mutate"] --> AGG
```

---

## См. также

- [Справочник по конфигурационному DSL](./02-configuration.md)
- [Фильтр мусорных мутаций Kotlin](./03-kotlin-filters.md)
- [Анализ в рамках git-diff](./04-git-integration.md)
- [Форматы отчётов и Quality Gate](./05-reporting.md)
- [Руководство разработчика](./06-development.md)
