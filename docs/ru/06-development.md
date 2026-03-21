---
id: development
title: Руководство по разработке
sidebar_label: Разработка
---

# Руководство по разработке

![Kotlin](https://img.shields.io/badge/Kotlin-2.3-7F52FF?style=flat-square&logo=kotlin&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-9.4.1-02303A?style=flat-square&logo=gradle&logoColor=white)
![JDK](https://img.shields.io/badge/JDK-17%2B-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![License](https://img.shields.io/badge/License-Apache_2.0-blue?style=flat-square)
![Lang](https://img.shields.io/badge/Lang-Русский-blue)

Это руководство охватывает всё необходимое для локальной сборки, тестирования и расширения mutaktor.

---

## Требования

| Инструмент | Минимум | Примечания |
|------|---------|-------|
| JDK | 17 | Протестировано с JDK 17, 21 и 25 (дистрибутив Temurin) |
| Git | любая | Необходим для функциональных тестов, использующих `GitDiffAnalyzer` |
| Docker / Podman | опционально | Требуется только для запуска dev-контейнера |

Других инструментов не требуется — Gradle и Kotlin управляются Gradle wrapper'ом.

---

## Начало работы

```bash
git clone https://github.com/dantte-lp/mutaktor.git
cd mutaktor
./gradlew check
```

`./gradlew check` запускает компиляцию, юнит-тесты и функциональные тесты. Чистое состояние репозитория даёт результат `BUILD SUCCESSFUL` менее чем за две минуты на типичном железе разработчика.

---

## Структура проекта

```
mutaktor/
├── mutaktor-gradle-plugin/          # Модуль Gradle-плагина
│   ├── build.gradle.kts
│   └── src/
│       ├── main/kotlin/io/github/dantte_lp/mutaktor/
│       │   ├── MutaktorPlugin.kt          # Plugin entry point
│       │   ├── MutaktorExtension.kt       # Type-safe DSL (24 properties)
│       │   ├── MutaktorTask.kt            # Main task — JavaExec wrapper
│       │   ├── MutaktorAggregatePlugin.kt # Multi-module aggregation
│       │   ├── git/
│       │   │   └── GitDiffAnalyzer.kt     # git diff → targetClasses
│       │   ├── report/
│       │   │   ├── MutationElementsConverter.kt
│       │   │   ├── SarifConverter.kt
│       │   │   ├── GithubChecksReporter.kt
│       │   │   └── QualityGate.kt
│       │   └── extreme/
│       │       └── ExtremeMutationConfig.kt
│       ├── test/                          # Юнит-тесты (JUnit 5 + Kotest)
│       └── functionalTest/                # Gradle TestKit тесты
│
├── mutaktor-pitest-filter/          # JAR плагина PIT
│   └── src/main/kotlin/io/github/dantte_lp/mutaktor/pitest/
│       └── KotlinJunkFilter.kt     # Реализация MutationInterceptor SPI
│
├── build-logic/                     # Convention-плагины (общая конфигурация сборки)
│   └── src/main/kotlin/
│       └── kotlin-conventions.gradle.kts
│
├── gradle/
│   └── libs.versions.toml           # Version catalog
├── gradle.properties                # version, group
├── settings.gradle.kts
├── CHANGELOG.md
└── .github/workflows/
    ├── ci.yml
    └── release.yml
```

### Ответственность модулей

| Модуль | Артефакт | Назначение |
|--------|----------|---------|
| `mutaktor-gradle-plugin` | `io.github.dantte-lp.mutaktor` | Gradle-плагин, применяемый в сборках потребителей |
| `mutaktor-pitest-filter` | `mutaktor-pitest-filter.jar` | JAR плагина PIT, загружаемый во время мутационного тестирования |
| `build-logic` | (внутренний) | Общие соглашения по Kotlin + JVM toolchain |

---

## Команды сборки

```bash
# Полная проверка: компиляция + юнит-тесты + функциональные тесты
./gradlew check

# Только юнит-тесты (быстрая обратная связь)
./gradlew test

# Только функциональные тесты Gradle TestKit
./gradlew functionalTest

# Только тесты модуля filter
./gradlew :mutaktor-pitest-filter:test

# Компиляция без запуска тестов
./gradlew build

# Очистка артефактов сборки
./gradlew clean
```

Задача `check` подключена следующим образом:

```kroki-mermaid
graph LR
    check --> test
    check --> functionalTest
    functionalTest -.->|shouldRunAfter| test
```

`functionalTest` использует `shouldRunAfter(test)`, а не `dependsOn`, поэтому обе задачи выполняются параллельно при наличии достаточной мощности.

---

## Версии зависимостей

Все версии объявлены в `gradle/libs.versions.toml`:

```toml
[versions]
kotlin         = "2.3.0"
pitest         = "1.23.0"
pitest-junit5  = "1.2.3"
junit          = "5.12.2"
kotest         = "6.0.0.M4"
gradle-testkit = "9.4.1"

[libraries]
pitest-command-line   = { module = "org.pitest:pitest-command-line",   version.ref = "pitest" }
pitest-entry          = { module = "org.pitest:pitest-entry",           version.ref = "pitest" }
pitest-junit5-plugin  = { module = "org.pitest:pitest-junit5-plugin",  version.ref = "pitest-junit5" }
junit-jupiter         = { module = "org.junit.jupiter:junit-jupiter",  version.ref = "junit" }
kotest-assertions     = { module = "io.kotest:kotest-assertions-core", version.ref = "kotest" }
```

---

## Соглашения о коде

### Язык

- **Только Kotlin.** Никакого Groovy, никакой Java в production-коде.
- В пакетах используются подчёркивания из-за имени пользователя GitHub: `io.github.dantte_lp.mutaktor`.

### Gradle Provider API

Все свойства задач должны использовать Provider API для ленивых вычислений и совместимости с кэшем конфигурации:

```kotlin
// Правильно — ленивый, безопасный для кэша конфигурации
public abstract val threads: Property<Int>
public abstract val targetClasses: SetProperty<String>

// Неправильно — жадный, ломает кэш конфигурации
var threads: Int = 4
var targetClasses: MutableSet<String> = mutableSetOf()
```

Ключевые типы Provider API, используемые в этом проекте:

| Тип | Случай использования |
|------|----------|
| `Property<T>` | Единственное скалярное значение |
| `SetProperty<T>` | Неупорядоченное множество (например, паттерны классов) |
| `ListProperty<T>` | Упорядоченный список (например, JVM-аргументы) |
| `MapProperty<K, V>` | Пары ключ-значение (например, конфигурация плагина) |
| `DirectoryProperty` | Выходная/входная директория |
| `RegularFileProperty` | Одиночный файл |

### Task API

```kotlin
// Правильно — ленивая регистрация
tasks.register("mutate", MutaktorTask::class.java) { task -> ... }

// Неправильно — жадное создание, удалено в Gradle 9
tasks.create("mutate", MutaktorTask::class.java) { ... }
```

Никогда не храните ссылки на `Project` в полях задачи — это нарушает сериализацию кэша конфигурации:

```kotlin
// Неправильно — Project не сериализуется
@get:Internal
val project: Project = ...

// Правильно — захватывать только то, что нужно во время конфигурации
@get:Input
val projectGroup: Property<String> = ...
```

### Директория сборки

```kotlin
// Правильно
task.reportDir.set(project.layout.buildDirectory.dir("reports/mutaktor"))

// Неправильно — устарело и удалено в Gradle 9
task.reportDir = project.buildDir.resolve("reports/mutaktor")
```

### Ноль внешних зависимостей

Production-код в `mutaktor-gradle-plugin` имеет ровно **одну** compile-зависимость: `org.pitest:pitest-command-line`. Всё остальное использует только стандартную библиотеку JDK:

- HTTP-запросы: `java.net.http.HttpClient` (JDK 11+)
- Разбор XML: `javax.xml.parsers.DocumentBuilderFactory`
- Генерация JSON: `StringBuilder` с ручным экранированием
- Файловый ввод/вывод: `java.io.File`

Не добавляйте сторонние зависимости (Jackson, OkHttp, Gson и т.д.) в `mutaktor-gradle-plugin`.

---

## Написание тестов

### Юнит-тесты

Юнит-тесты находятся в `mutaktor-gradle-plugin/src/test/` и используют JUnit 5 с assertions Kotest:

```kotlin
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SarifConverterTest {

    @Test
    fun `convert produces valid SARIF version field`() {
        val xml = buildMutationsXml(status = "SURVIVED")
        val sarif = SarifConverter.convert(xml, pitVersion = "1.23.0")
        sarif shouldContain """"version": "2.1.0""""
    }
}
```

### Функциональные тесты

Функциональные тесты находятся в `mutaktor-gradle-plugin/src/functionalTest/` и используют Gradle TestKit для запуска реальных Gradle-сборок во временной директории:

```kotlin
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class MutaktorPluginFunctionalTest {

    @TempDir
    lateinit var projectDir: File

    @Test
    fun `mutate task runs PIT and produces HTML report`() {
        // Создать минимальный проект
        projectDir.resolve("settings.gradle.kts").writeText("""rootProject.name = "test-project"""")
        projectDir.resolve("build.gradle.kts").writeText("""
            plugins {
                java
                id("io.github.dantte-lp.mutaktor")
            }
            mutaktor {
                targetClasses.set(setOf("com.example.*"))
            }
        """.trimIndent())

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("mutate", "--stacktrace")
            .withPluginClasspath()
            .build()

        result.output shouldContain "BUILD SUCCESSFUL"
    }
}
```

Исходный набор `functionalTest` настроен в `mutaktor-gradle-plugin/build.gradle.kts` и подключён к жизненному циклу `check`.

---

## Добавление новых паттернов фильтрации

Фильтры мусорных мутаций Kotlin находятся в `KotlinJunkFilter.kt` в модуле `mutaktor-pitest-filter`. Фильтр реализует SPI `MutationInterceptor` PIT.

### Шаг 1 — Определить паттерн

Запустите PIT без фильтров на Kotlin-проекте и изучите отчёт `mutations.xml`. Ищите мутации со статусом `SURVIVED`, находящиеся в сгенерированном компилятором коде. Запишите значения `<mutatedClass>`, `<method>` и `<description>`.

### Шаг 2 — Добавить предикат в `KotlinJunkFilter`

Откройте `KotlinJunkFilter.kt` и добавьте приватный метод-предикат:

```kotlin
/**
 * Паттерн 6 — внутренний класс $WhenMappings sealed-класса.
 *
 * Kotlin компилирует `when` на sealed-классах в синтетический класс `$WhenMappings`
 * с массивом int. Мутации внутри этого класса неуничтожимы.
 */
private fun isWhenMappingsClass(mutation: MutationDetails): Boolean {
    val className = mutation.className.asJavaName()
    return className.endsWith("\$WhenMappings")
}
```

### Шаг 3 — Подключить предикат в `isKotlinJunk`

```kotlin
private fun isKotlinJunk(mutation: MutationDetails): Boolean =
    isDefaultImplsClass(mutation)      ||
    isIntrinsicsNullCheck(mutation)    ||
    isDataClassGeneratedMethod(mutation) ||
    isCoroutineStateMachine(mutation)  ||
    isWhenHashcodeDispatch(mutation)   ||
    isWhenMappingsClass(mutation)        // <-- новый паттерн
```

### Шаг 4 — Написать юнит-тест

```kotlin
@Test
fun `isWhenMappingsClass filters WhenMappings synthetic class`() {
    val mutation = fakeMutation(className = "com.example.Status\$WhenMappings")
    KotlinJunkFilter().intercept(listOf(mutation), fakeMutater()) shouldBe emptyList()
}
```

### Шаг 5 — Обновить документацию

Добавьте строку в таблицу фильтров в `docs/ru/03-kotlin-filters.md` и обновите `CHANGELOG.md` в разделе `[Unreleased] > Added`.

---

## Добавление новых форматов отчётов

Конвертеры отчётов находятся в `mutaktor-gradle-plugin/src/main/kotlin/io/github/dantte_lp/mutaktor/report/`.

### Шаг 1 — Создать объект-конвертер

```kotlin
package io.github.dantte_lp.mutaktor.report

import java.io.File

/**
 * Конвертирует PIT `mutations.xml` в сводный JUnit XML для интеграции с результатами тестов.
 */
public object JUnitXmlConverter {

    public fun convert(mutationsXml: File, pitVersion: String): String {
        // Разобрать XML PIT (повторно использовать паттерн из SarifConverter)
        // Построить выходной формат
        TODO("implement")
    }
}
```

Используйте только стандартную библиотеку JDK — никаких сторонних JSON/XML-библиотек.

### Шаг 2 — Добавить свойство расширения в `MutaktorExtension`

```kotlin
// В MutaktorExtension.kt — раздел Reporting

/**
 * Генерировать сводный JUnit XML отчёт вместе с HTML.
 */
public abstract val junitXmlReport: Property<Boolean>

// В блоке init:
junitXmlReport.convention(false)
```

### Шаг 3 — Вызвать конвертер из `MutaktorTask.exec()`

```kotlin
override fun exec() {
    // ... существующее выполнение PIT ...
    super.exec()

    // Пост-обработка
    if (junitXmlReport.getOrElse(false)) {
        val mutationsXml = reportDir.get().file("mutations.xml").asFile
        if (mutationsXml.exists()) {
            val output = JUnitXmlConverter.convert(mutationsXml, pitVersion.getOrElse("unknown"))
            reportDir.get().file("junit-summary.xml").asFile.writeText(output)
        }
    }
}
```

### Шаг 4 — Написать юнит-тесты для конвертера

Следуйте паттерну `SarifConverterTest` — создайте минимальную строку `mutations.xml` и проверьте структурные свойства вывода.

### Шаг 5 — Обновить документацию по DSL

Добавьте новое свойство в таблицу конфигурации в `docs/ru/02-configuration.md`.

---

## Сводка соглашений

| Правило | Детали |
|------|--------|
| Язык | Только Kotlin, никакого Groovy, никакой Java в production |
| Gradle API | `tasks.register`, никогда `tasks.create` |
| Provider API | Все входные/выходные данные задач используют `Property<T>`, `SetProperty<T>` и т.д. |
| Ссылки на Project | Никогда не хранить `Project` в полях задачи |
| Директория сборки | `layout.buildDirectory`, никогда `project.buildDir` |
| Внешние зависимости | Ноль в production-коде `mutaktor-gradle-plugin` |
| Тестовый фреймворк | JUnit 5 + Kotest assertions |
| Функциональные тесты | Gradle TestKit |
| Кэш конфигурации | Все свойства задач должны быть сериализуемы |

---

## См. также

- [07-ci-cd.md](07-ci-cd.md) — Воркфлоу GitHub Actions и CI-интеграция
- [08-changelog.md](08-changelog.md) — Процесс релиза и формат changelog
- `CONTRIBUTING.md` — Чеклист PR и модель ветвления
- `CLAUDE.md` — Ограничения и соглашения проекта для разработки с помощью AI
