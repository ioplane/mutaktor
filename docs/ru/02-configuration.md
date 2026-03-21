---
id: configuration
title: Справочник по конфигурационному DSL
sidebar_label: Конфигурация
---

# Справочник по конфигурационному DSL

![Gradle](https://img.shields.io/badge/Gradle-9.4.1-02303A?logo=gradle&logoColor=white)
![Kotlin DSL](https://img.shields.io/badge/DSL-Kotlin-7F52FF?logo=kotlin&logoColor=white)
![Config Cache](https://img.shields.io/badge/Configuration%20Cache-совместим-brightgreen)
![Lang](https://img.shields.io/badge/Lang-Русский-blue)

## Обзор

Все параметры плагина объявляются в блоке расширения `mutaktor`. Каждое свойство поддерживается Provider API Gradle, что означает ленивое разрешение значений во время выполнения задачи, а не во время конфигурации. Это обеспечивает полную совместимость плагина с кэшем конфигурации Gradle и режимом изолированных проектов.

## Быстрый старт

### Kotlin DSL

```kotlin
// build.gradle.kts
plugins {
    id("io.github.dantte-lp.mutaktor") version "x.y.z"
}

mutaktor {
    targetClasses = setOf("com.example.*")
    threads = Runtime.getRuntime().availableProcessors()
    since = "main"
    kotlinFilters = true
    outputFormats = setOf("HTML", "XML")
}
```

### Groovy DSL

```groovy
// build.gradle
plugins {
    id 'io.github.dantte-lp.mutaktor' version 'x.y.z'
}

mutaktor {
    targetClasses = ['com.example.*'] as Set
    threads = Runtime.runtime.availableProcessors()
    since = 'main'
    kotlinFilters = true
    outputFormats = ['HTML', 'XML'] as Set
}
```

## Справочник свойств

### Основные

Эти свойства управляют тем, какие классы мутируются, сколько мутантов генерируется и какая версия PIT используется.

| Свойство | Тип | По умолчанию | Описание |
|---|---|---|---|
| `pitVersion` | `Property<String>` | `"1.23.0"` | Версия PIT, разрешаемая из Maven Central |
| `targetClasses` | `SetProperty<String>` | `setOf("$group.*")` | Glob-паттерны, определяющие классы для мутирования |
| `targetTests` | `SetProperty<String>` | _(автоопределение PIT)_ | Glob-паттерны, определяющие тестовые классы для запуска |
| `threads` | `Property<Int>` | `availableProcessors()` | Количество параллельных потоков анализа мутаций |
| `mutators` | `SetProperty<String>` | `setOf("DEFAULTS")` | Группы мутаторов или имена отдельных мутаторов |
| `timeoutFactor` | `Property<BigDecimal>` | `1.25` | Множитель, применяемый к нормальному времени выполнения тестов для вычисления таймаута каждого мутанта |
| `timeoutConstant` | `Property<Int>` | `4000` | Константа (мс), добавляемая к вычисленному таймауту для каждого мутанта |

#### Группы мутаторов

PIT поставляется с предопределёнными группами мутаторов. Можно использовать любую комбинацию групп и имён отдельных мутаторов.

| Группа | Описание |
|---|---|
| `DEFAULTS` | Стандартные операторы мутации — базовый набор для большинства проектов |
| `STRONGER` | Более агрессивные операторы, генерируют больше мутантов, могут увеличить время анализа |
| `ALL` | Все доступные мутаторы; используйте только на небольших кодовых базах |

Отдельные мутаторы можно смешивать с группами:

```kotlin
mutaktor {
    mutators = setOf("DEFAULTS", "UOI", "AOR")
}
```

#### Kotlin DSL — пример (основные свойства)

```kotlin
mutaktor {
    pitVersion = "1.23.0"
    targetClasses = setOf("com.example.service.*", "com.example.domain.*")
    targetTests = setOf("com.example.*Test", "com.example.*Spec")
    threads = 4
    mutators = setOf("DEFAULTS")
    timeoutFactor = java.math.BigDecimal("1.50")
    timeoutConstant = 5000
}
```

#### Groovy DSL — пример (основные свойства)

```groovy
mutaktor {
    pitVersion = '1.23.0'
    targetClasses = ['com.example.service.*', 'com.example.domain.*'] as Set
    targetTests = ['com.example.*Test', 'com.example.*Spec'] as Set
    threads = 4
    mutators = ['DEFAULTS'] as Set
    timeoutFactor = 1.50
    timeoutConstant = 5000
}
```

### Фильтрация

Используйте свойства фильтрации для исключения генерируемого кода, шаблонного кода фреймворков и инфраструктурных классов, которые не являются значимыми целями для мутационного тестирования.

| Свойство | Тип | По умолчанию | Описание |
|---|---|---|---|
| `excludedClasses` | `SetProperty<String>` | _(пусто)_ | Glob-паттерны для классов, исключённых из мутирования |
| `excludedMethods` | `SetProperty<String>` | _(пусто)_ | Паттерны имён методов, исключённых из мутирования; поддерживает простые маски |
| `excludedTestClasses` | `SetProperty<String>` | _(пусто)_ | Glob-паттерны для тестовых классов, исключённых из выполнения тестов |
| `avoidCallsTo` | `SetProperty<String>` | _(пусто)_ | Полные префиксы пакетов, вызовы методов которых заменяются NO-OP во время анализа (например, логирование) |

#### Kotlin DSL — пример (фильтрация)

```kotlin
mutaktor {
    excludedClasses = setOf(
        "com.example.generated.*",
        "com.example.config.*",
        "*\$Companion",
    )
    excludedMethods = setOf("toString", "hashCode", "equals")
    avoidCallsTo = setOf(
        "kotlin.jvm.internal",
        "org.slf4j",
        "org.apache.logging",
    )
}
```

#### Groovy DSL — пример (фильтрация)

```groovy
mutaktor {
    excludedClasses = ['com.example.generated.*', 'com.example.config.*'] as Set
    excludedMethods = ['toString', 'hashCode', 'equals'] as Set
    avoidCallsTo = ['kotlin.jvm.internal', 'org.slf4j'] as Set
}
```

### Отчётность

| Свойство | Тип | По умолчанию | Описание |
|---|---|---|---|
| `reportDir` | `DirectoryProperty` | `build/reports/mutaktor` | Директория, в которую PIT записывает вывод |
| `outputFormats` | `SetProperty<String>` | `setOf("HTML", "XML")` | Генерируемые форматы вывода; см. таблицу ниже |
| `timestampedReports` | `Property<Boolean>` | `false` | При значении `true` PIT создаёт субдиректорию с временной меткой для каждого запуска, а не перезаписывает результаты |

#### Форматы вывода

| Значение | Описание |
|---|---|
| `HTML` | Интерактивный HTML-отчёт с подсветкой мутаций на уровне строк кода — стандартный отчёт PIT |
| `XML` | Машиночитаемый отчёт `mutations.xml`; обязателен для конвертации в SARIF и Stryker Dashboard |
| `CSV` | Сводный файл в формате TSV |

#### Kotlin DSL — пример (отчётность)

```kotlin
mutaktor {
    reportDir = layout.buildDirectory.dir("reports/mutation")
    outputFormats = setOf("HTML", "XML")
    timestampedReports = false
}
```

### Конфигурация тестов

| Свойство | Тип | По умолчанию | Описание |
|---|---|---|---|
| `junit5PluginVersion` | `Property<String>` | `"1.2.3"` | Версия `org.pitest:pitest-junit5-plugin`, разрешаемая из Maven Central |
| `includedGroups` | `SetProperty<String>` | _(пусто)_ | Выражения тегов JUnit 5 для включаемых тестов |
| `excludedGroups` | `SetProperty<String>` | _(пусто)_ | Выражения тегов JUnit 5 для исключаемых тестов |
| `fullMutationMatrix` | `Property<Boolean>` | `false` | При значении `true` каждый мутант проверяется всеми тестами без досрочного выхода; значительно увеличивает время выполнения |

#### Kotlin DSL — пример (конфигурация тестов)

```kotlin
mutaktor {
    junit5PluginVersion = "1.2.3"
    includedGroups = setOf("unit", "integration")
    excludedGroups = setOf("slow", "e2e")
    fullMutationMatrix = false
}
```

### Расширенные настройки / JVM

| Свойство | Тип | По умолчанию | Описание |
|---|---|---|---|
| `jvmArgs` | `ListProperty<String>` | _(пусто)_ | Дополнительные JVM-аргументы, передаваемые в **дочерние** форкнутые тестовые процессы (например, `--add-opens`, `-Xmx`) |
| `mainProcessJvmArgs` | `ListProperty<String>` | _(пусто)_ | Дополнительные JVM-аргументы, передаваемые в **основной** процесс анализа PIT (не в дочерние воркеры) |
| `pluginConfiguration` | `MapProperty<String, String>` | _(пусто)_ | Пары ключ-значение, передаваемые в плагины PIT через `--pluginConfiguration`; ключи следуют шаблону `pluginName.key` |
| `features` | `ListProperty<String>` | _(пусто)_ | Флаги функций PIT для включения (`+flagName`) или отключения (`-flagName`) |
| `verbose` | `Property<Boolean>` | `false` | Включить подробный вывод PIT в консоль; полезно для отладки проблем с classpath или конфигурацией |
| `useClasspathFile` | `Property<Boolean>` | `true` | При значении `true` записывает classpath в `build/mutaktor/pitClasspath` и передаёт его через `--classPathFile`, избегая ограничений длины аргументов ОС |

#### Kotlin DSL — расширенный пример

```kotlin
mutaktor {
    jvmArgs = listOf(
        "--add-opens=java.base/java.lang=ALL-UNNAMED",
        "-Xmx2g",
    )
    features = listOf("+auto_threads", "-FLOGIC")
    pluginConfiguration = mapOf(
        "ARCMUTATE_ENGINE.limit" to "100",
    )
    verbose = false
    useClasspathFile = true
}
```

### Git-aware-анализ

| Свойство | Тип | По умолчанию | Описание |
|---|---|---|---|
| `since` | `Property<String>` | _(не задано)_ | Git-реф для сравнения (имя ветки, тег или SHA коммита). Если задано, мутируются только классы, изменившиеся с этого рефа. |

Если `since` задан, но `git diff` не возвращает изменённых исходных файлов, плагин откатывается к настроенному `targetClasses` и записывает сообщение в лог жизненного цикла.

```kotlin
mutaktor {
    // Мутировать только классы, изменившиеся с ветки main
    since = "main"

    // Или ограничиться последними 5 коммитами
    // since = "HEAD~5"

    // Или конкретным SHA коммита
    // since = "a1b2c3d"
}
```

Подробности см. в разделе [Анализ в рамках git-диффа](./04-git-integration.md).

### Фильтр мусорных мутаций Kotlin

| Свойство | Тип | По умолчанию | Описание |
|---|---|---|---|
| `kotlinFilters` | `Property<Boolean>` | `true` | Включить встроенный `KotlinJunkFilter`, подавляющий мутации в байткоде, генерируемом компилятором Kotlin |

При включении JAR `mutaktor-pitest-filter` добавляется в classpath PIT. В многомодульной сборке, включающей подпроект `:mutaktor-pitest-filter`, используется локальная зависимость проекта. В автономной сборке JAR должен быть доступен как опубликованный артефакт.

```kotlin
mutaktor {
    kotlinFilters = true  // по умолчанию; подавляет шум от data-class, coroutine, null-check
}
```

Описание всех 5 паттернов фильтрации см. в разделе [Фильтр мусорных мутаций Kotlin](./03-kotlin-filters.md).

### Режим экстремальных мутаций

| Свойство | Тип | По умолчанию | Описание |
|---|---|---|---|
| `extreme` | `Property<Boolean>` | `false` | Заменить точечные мутаторы операторами удаления тела метода. Генерирует ~1 мутант на метод вместо ~10, что делает анализ практичным для больших кодовых баз. |

При `extreme = true` свойство `mutators` переопределяется 6 операторами удаления тела метода независимо от того, что настроено:

| Мутатор | Эффект |
|---|---|
| `VOID_METHOD_CALLS` | Удаляет вызовы void-методов |
| `EMPTY_RETURNS` | Заменяет возвраты объектов пустыми/дефолтными значениями |
| `FALSE_RETURNS` | Заменяет возвраты boolean значением `false` |
| `TRUE_RETURNS` | Заменяет возвраты boolean значением `true` |
| `NULL_RETURNS` | Заменяет возвраты объектов значением `null` |
| `PRIMITIVE_RETURNS` | Заменяет возвраты примитивов значением `0` |

```kotlin
mutaktor {
    extreme = true  // переопределяет mutators; игнорирует любую конфигурацию mutators = setOf(...)
}
```

### Инкрементальный анализ

| Свойство | Тип | По умолчанию | Описание |
|---|---|---|---|
| `historyInputLocation` | `RegularFileProperty` | _(не задано)_ | Файл, из которого читается предыдущее состояние анализа мутаций; включает инкрементальный анализ |
| `historyOutputLocation` | `RegularFileProperty` | _(не задано)_ | Файл, в который записывается состояние анализа мутаций после запуска |

```kotlin
mutaktor {
    val historyFile = layout.projectDirectory.file(".mutation-history")
    historyInputLocation = historyFile
    historyOutputLocation = historyFile
}
```

## Полный пример конфигурации

### Kotlin DSL

```kotlin
// build.gradle.kts
plugins {
    kotlin("jvm") version "2.3.0"
    id("io.github.dantte-lp.mutaktor") version "x.y.z"
}

mutaktor {
    // Основные
    pitVersion = "1.23.0"
    targetClasses = setOf("com.example.*")
    threads = Runtime.getRuntime().availableProcessors()
    mutators = setOf("DEFAULTS")
    timeoutFactor = java.math.BigDecimal("1.25")
    timeoutConstant = 4000

    // Фильтрация
    excludedClasses = setOf("com.example.generated.*")
    excludedMethods = setOf("toString", "hashCode", "equals")
    avoidCallsTo = setOf("kotlin.jvm.internal", "org.slf4j")

    // Отчётность
    outputFormats = setOf("HTML", "XML")
    timestampedReports = false

    // Тесты
    junit5PluginVersion = "1.2.3"

    // Расширенные
    jvmArgs = listOf("-Xmx2g")
    useClasspathFile = true
    verbose = false

    // Git-aware анализ
    since = providers.environmentVariable("MUTATION_SINCE").orNull

    // Kotlin-фильтр
    kotlinFilters = true

    // Инкрементальный
    val historyFile = layout.projectDirectory.file(".mutation-history")
    historyInputLocation = historyFile
    historyOutputLocation = historyFile
}
```

### Groovy DSL

```groovy
// build.gradle
plugins {
    id 'org.jetbrains.kotlin.jvm' version '2.3.0'
    id 'io.github.dantte-lp.mutaktor' version 'x.y.z'
}

mutaktor {
    pitVersion = '1.23.0'
    targetClasses = ['com.example.*'] as Set
    threads = Runtime.runtime.availableProcessors()
    mutators = ['DEFAULTS'] as Set
    timeoutFactor = 1.25
    timeoutConstant = 4000

    excludedClasses = ['com.example.generated.*'] as Set
    excludedMethods = ['toString', 'hashCode', 'equals'] as Set
    avoidCallsTo = ['kotlin.jvm.internal', 'org.slf4j'] as Set

    outputFormats = ['HTML', 'XML'] as Set
    timestampedReports = false

    junit5PluginVersion = '1.2.3'

    jvmArgs = ['--add-opens=java.base/java.lang=ALL-UNNAMED', '-Xmx2g']
    useClasspathFile = true
    verbose = false

    since = System.getenv('MUTATION_SINCE')
    kotlinFilters = true
}
```

## Таблица значений по умолчанию

| Свойство | Значение по умолчанию |
|---|---|
| `pitVersion` | `"1.23.0"` |
| `targetClasses` | `setOf("$project.group.*")` |
| `targetTests` | _(автоопределение PIT по targetClasses)_ |
| `threads` | `Runtime.getRuntime().availableProcessors()` |
| `mutators` | `setOf("DEFAULTS")` |
| `timeoutFactor` | `BigDecimal("1.25")` |
| `timeoutConstant` | `4000` |
| `excludedClasses` | _(пусто)_ |
| `excludedMethods` | _(пусто)_ |
| `excludedTestClasses` | _(пусто)_ |
| `avoidCallsTo` | _(пусто)_ |
| `reportDir` | `build/reports/mutaktor` |
| `outputFormats` | `setOf("HTML", "XML")` |
| `timestampedReports` | `false` |
| `junit5PluginVersion` | `"1.2.3"` |
| `includedGroups` | _(пусто)_ |
| `excludedGroups` | _(пусто)_ |
| `fullMutationMatrix` | `false` |
| `jvmArgs` | _(пусто)_ |
| `mainProcessJvmArgs` | _(пусто)_ |
| `pluginConfiguration` | _(пусто)_ |
| `features` | _(пусто)_ |
| `verbose` | `false` |
| `since` | _(не задано)_ |
| `kotlinFilters` | `true` |
| `extreme` | `false` |
| `historyInputLocation` | _(не задано)_ |
| `historyOutputLocation` | _(не задано)_ |
| `useClasspathFile` | `true` |

## См. также

- [Архитектура плагина](./01-architecture.md)
- [Фильтр мусорных мутаций Kotlin](./03-kotlin-filters.md)
- [Анализ в рамках git-диффа](./04-git-integration.md)
- [Форматы отчётов и Quality Gate](./05-reporting.md)
