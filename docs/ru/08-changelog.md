---
id: changelog
title: Руководство по changelog
sidebar_label: Changelog
---

# Руководство по changelog

![Keep a Changelog](https://img.shields.io/badge/Keep_a_Changelog-1.1.0-orange?style=flat-square)
![SemVer](https://img.shields.io/badge/SemVer-2.0.0-blue?style=flat-square)
![GitHub Releases](https://img.shields.io/badge/GitHub-Releases-181717?style=flat-square&logo=github)
![Lang](https://img.shields.io/badge/Lang-Русский-blue)

Этот документ объясняет формат changelog, политику версионирования и автоматизированный процесс релиза для mutaktor.

---

## Формат

`CHANGELOG.md` mutaktor следует стандарту [Keep a Changelog 1.1.0](https://keepachangelog.com/en/1.1.0/).

### Структура

```markdown
# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- New feature or enhancement

### Changed
- Change to existing behavior

### Deprecated
- Features that will be removed in a future release

### Removed
- Features removed in this release

### Fixed
- Bug fixes

### Security
- Security fixes

## [1.2.0] — 2026-04-01

### Added
- ...

[Unreleased]: https://github.com/dantte-lp/mutaktor/compare/v1.2.0...HEAD
[1.2.0]: https://github.com/dantte-lp/mutaktor/compare/v1.1.0...v1.2.0
```

### Правила использования разделов

| Раздел | Когда использовать |
|---------|-------------|
| `Added` | Новые функции, новые DSL-свойства, новые форматы отчётов |
| `Changed` | Изменения поведения существующих функций, изменения значений по умолчанию |
| `Deprecated` | Свойства или задачи, запланированные к удалению |
| `Removed` | Свойства или задачи, которые ранее были помечены как устаревшие |
| `Fixed` | Исправления ошибок — ссылайтесь на номер issue, если применимо |
| `Security` | Любые исправления с последствиями для безопасности |

Каждое изменение, видимое пользователю, требует записи в `CHANGELOG.md`. Внутренний рефакторинг, не затрагивающий API плагина, записи не требует.

---

## Политика версионирования

mutaktor использует [Semantic Versioning 2.0.0](https://semver.org/spec/v2.0.0.html).

### Формат версии

```
MAJOR.MINOR.PATCH[-SNAPSHOT]
```

Примеры:

| Версия | Значение |
|---------|---------|
| `0.1.0-SNAPSHOT` | Pre-release сборка в разработке (текущая) |
| `0.1.0` | Первый публичный релиз |
| `0.2.0` | Новая функция с обратной совместимостью |
| `1.0.0` | Стабильный публичный API, первый major-релиз |
| `1.1.0` | Добавлено новое DSL-свойство (обратная совместимость) |
| `2.0.0` | Ломающее изменение DSL или API задач |

### Ломающие и не ломающие изменения

| Тип изменения | Версия |
|-------------|-------------|
| Добавить новое опциональное DSL-свойство с конвенцией | MINOR |
| Добавить новую задачу | MINOR |
| Удалить или переименовать существующее DSL-свойство | MAJOR |
| Изменить значение по умолчанию существующего свойства | MAJOR (если меняется поведение) |
| Исправление ошибки, не меняющее API | PATCH |
| Новый формат отчёта как opt-in | MINOR |
| Повышение минимальной версии Gradle/JDK | MAJOR |

### Политика до 1.0

Пока версия равна `0.x.y`, публичный API ещё не стабилен. Повышения MINOR-версии (`0.1.0` → `0.2.0`) могут включать ломающие изменения. DSL стабилизируется на версии `1.0.0`.

---

## Текущая версия

Версия объявлена в `gradle.properties`:

```properties
version=0.1.0-SNAPSHOT
group=io.github.dantte-lp.mutaktor
```

Snapshot-сборки не публикуются на Gradle Plugin Portal. Только теговые релизы создают опубликованные артефакты.

---

## Процесс релиза

### Обзор

```kroki-mermaid
flowchart TD
    dev["Разработчик сливает PR в main"] --> update["Обновить CHANGELOG.md:\nперенести [Unreleased] → [X.Y.Z]"]
    update --> bump["Обновить gradle.properties:\nversion=X.Y.Z"]
    bump --> commit["git commit -m 'Release X.Y.Z'"]
    commit --> tag["git tag vX.Y.Z"]
    tag --> push["git push origin main --tags"]
    push --> trigger["release.yml срабатывает\nна тег v*"]

    subgraph "GitHub Actions: release.yml"
        trigger --> matrix["Матричная сборка\nJDK 17 + 25"]
        matrix --> testok{Все тесты пройдены?}
        testok -- Нет --> fail["Воркфлоу завершается с ошибкой\nРелиз не создаётся"]
        testok -- Да --> jars["Сбор JAR'ов\n(сборка JDK 17)"]
        jars --> extract["Извлечение заметок о выпуске\nиз CHANGELOG.md (awk)"]
        extract --> ghrel["gh release create vX.Y.Z\n+ прикрепление JAR'ов\n+ заметки о выпуске"]
    end

    ghrel --> done["GitHub Release опубликован"]

    style trigger fill:#e37400,color:#fff
    style ghrel fill:#181717,color:#fff
    style fail fill:#cc3333,color:#fff
    style done fill:#2d8a4e,color:#fff
```

### Пошаговые инструкции

#### 1. Подготовить changelog

Перенести все записи из `[Unreleased]` в новый раздел с датой:

```markdown
## [Unreleased]

## [0.2.0] — 2026-04-15

### Added
- Quality gate: fail build if mutation score below threshold
- GitHub Checks API reporter with inline PR annotations

### Fixed
- SARIF converter handles mutations with no source file gracefully

[Unreleased]: https://github.com/dantte-lp/mutaktor/compare/v0.2.0...HEAD
[0.2.0]: https://github.com/dantte-lp/mutaktor/compare/v0.1.0...v0.2.0
```

Оставляйте `[Unreleased]` вверху — всегда пустым после релиза.

#### 2. Обновить версию

```properties
# gradle.properties
version=0.2.0
```

Удалите суффикс `-SNAPSHOT`. Release-воркфлоу убирает префикс `v` из тега и передаёт его в Gradle через `-Pversion="${VERSION}"`.

#### 3. Сделать коммит и тег

```bash
git add CHANGELOG.md gradle.properties
git commit -m "Release 0.2.0"
git tag v0.2.0
git push origin main --tags
```

Тег должен строго соответствовать паттерну `v*`. Триггер воркфлоу:

```yaml
on:
  push:
    tags:
      - "v*"
```

#### 4. Проверить release-воркфлоу

Перейдите в **Actions → Release** в GitHub-репозитории. Воркфлоу:

1. Запускает `./gradlew check -Pversion="0.2.0"` на JDK 17 и 25
2. Загружает JAR'ы из сборки JDK 17 как артефакт воркфлоу
3. Извлекает раздел `[0.2.0]` из `CHANGELOG.md` с помощью `awk`-скрипта
4. Создаёт GitHub Release с именем `mutaktor v0.2.0` с извлечёнными заметками и прикреплёнными JAR'ами

Если `awk`-скрипт не находит соответствующего раздела, он откатывается к ссылке на `CHANGELOG.md`.

#### 5. После релиза: восстановить SNAPSHOT

После завершения release-воркфлоу верните версию к следующему SNAPSHOT:

```bash
# gradle.properties
version=0.3.0-SNAPSHOT
```

```bash
git add gradle.properties
git commit -m "Begin 0.3.0-SNAPSHOT development"
git push origin main
```

---

## Извлечение заметок о выпуске

Release-воркфлоу извлекает соответствующий раздел changelog автоматически:

```bash
VERSION="${GITHUB_REF_NAME#v}"   # убирает ведущий 'v'

awk -v ver="$VERSION" '
  /^## / { if (found) exit; if ($0 ~ ver) { found=1; next } }
  found { print }
' CHANGELOG.md > release-notes.md
```

Скрипт выводит все строки между заголовком `## [X.Y.Z]` и следующим заголовком `## `. Вывод используется дословно как тело GitHub Release.

Пример — для тега `v0.2.0` и следующего changelog:

```markdown
## [0.2.0] — 2026-04-15

### Added
- Quality gate

## [0.1.0] — 2026-03-21
```

Скрипт выдаёт:

```markdown

### Added
- Quality gate

```

---

## Лучшие практики ведения changelog

### Пишите записи как описания для пользователя

```markdown
# Хорошо — объясняет, что получает пользователь
- Git-diff scoped analysis: `mutaktor { since.set("main") }` — only mutates changed classes

# Слишком внутреннее — описывает реализацию, а не эффект для пользователя
- Added `GitDiffAnalyzer.changedClasses()` method
```

### Ссылайтесь на номера спринтов или issue для отслеживаемости

```markdown
### Added
- Extreme mutation mode: 6 method-body removal mutators, `extreme.set(true)` (Sprint 7)
- GitHub Checks API reporter with inline PR annotations for survived mutants (Sprint 6, #42)
```

### Группируйте связанные записи

Держите все изменения под правильным заголовком раздела внутри одного блока версии. Не добавляйте произвольный текст вне разделов.

### Не редактируйте выпущенные разделы

Как только версия помечена тегом и выпущена, её раздел changelog неизменен. Если в выпущенной записи содержится ошибка, добавьте корректирующую запись в следующую версию.

---

## Текущие нереализованные записи

Из `CHANGELOG.md` на момент написания этой документации:

| Раздел | Запись |
|---------|-------|
| Added | Quality gate: fail build if mutation score below threshold (Sprint 6) |
| Added | Multi-module aggregation: `mutateAggregate` task (Sprint 8) |
| Added | Release workflow: GitHub Actions with JDK 17+25 matrix (Sprint 8) |
| Added | Extreme mutation mode: 6 method-body removal mutators (Sprint 7) |
| Added | GitHub Checks API reporter with inline PR annotations (Sprint 6) |
| Added | mutation-testing-elements JSON report converter (Sprint 5) |
| Added | SARIF 2.1.0 report converter (Sprint 5) |
| Added | Git-diff scoped analysis: `since` property (Sprint 4) |
| Added | Type-safe Kotlin DSL with 24 managed properties (Sprint 2) |
| Added | PIT execution via JavaExec (Sprint 2) |
| Added | Kotlin junk mutation filter with 5 filter patterns (Sprint 3) |
| Added | Project scaffold: Kotlin 2.3, Gradle 9.4.1, JDK 25 (Sprint 1) |
| Added | GitHub Actions CI workflow with JDK 17/21/25 matrix (Sprint 1) |

Все записи переедут в `[0.1.0]`, когда будет отправлен первый тег релиза.

---

## См. также

- [07-ci-cd.md](07-ci-cd.md) — Детали реализации release-воркфлоу
- `CHANGELOG.md` — Фактический changelog
- `gradle.properties` — Текущее объявление версии
- [Keep a Changelog 1.1.0](https://keepachangelog.com/en/1.1.0/)
- [Semantic Versioning 2.0.0](https://semver.org/spec/v2.0.0.html)
