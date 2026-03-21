---
id: ci-cd
title: CI/CD-интеграция
sidebar_label: CI/CD
---

# CI/CD-интеграция

![GitHub Actions](https://img.shields.io/badge/GitHub_Actions-2088FF?style=flat-square&logo=github-actions&logoColor=white)
![SARIF](https://img.shields.io/badge/SARIF-2.1.0-green?style=flat-square)
![Checks API](https://img.shields.io/badge/GitHub_Checks-API-181717?style=flat-square&logo=github)
![Lang](https://img.shields.io/badge/Lang-Русский-blue)

Этот документ описывает, как mutaktor сам собирается и выпускается в CI, и как интегрировать плагин в ваши собственные воркфлоу GitHub Actions.

---

## Собственные CI/CD-воркфлоу mutaktor

### CI-воркфлоу (`.github/workflows/ci.yml`)

CI-воркфлоу запускается при каждом push в `main` и при каждом pull request'е, нацеленном на `main`. Он проверяет плагин на матрице из трёх версий JDK.

```yaml
name: CI

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        java: [17, 21, 25]
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK ${{ matrix.java }}
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: ${{ matrix.java }}

      - name: Set up Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Build and test
        run: ./gradlew check --no-daemon --warning-mode=all

      - name: Upload test reports
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: test-reports-jdk${{ matrix.java }}
          path: "**/build/reports/tests/"
          retention-days: 14
```

Ключевые решения:

| Решение | Обоснование |
|----------|-----------|
| Матрица JDK: 17, 21, 25 | 17 — минимум, 21 — текущий LTS, 25 — максимум тестирования |
| `--no-daemon` | Исключает загрязнение состояния daemon'а между задачами матрицы |
| `--warning-mode=all` | Рано обнаруживает устаревшее использование API |
| Отчёты тестов хранятся 14 дней | Позволяет расследовать причины сбоев без повторного запуска |
| `if: always()` при загрузке | Отчёты загружаются даже при сбое тестов |

### Обзор CI-воркфлоу

```kroki-mermaid
graph TD
    push["push / pull_request"] --> matrix["Матрица: JDK 17 / 21 / 25"]
    matrix --> checkout["actions/checkout@v4"]
    checkout --> jdk["actions/setup-java@v4\n(Temurin)"]
    jdk --> gradle["gradle/actions/setup-gradle@v4"]
    gradle --> check["./gradlew check\n--no-daemon --warning-mode=all"]
    check --> upload["Загрузка отчётов тестов\n(всегда, 14 дней)"]

    style check fill:#02303A,color:#fff
    style matrix fill:#7F52FF,color:#fff
```

---

### Release-воркфлоу (`.github/workflows/release.yml`)

Release-воркфлоу срабатывает на любом теге, соответствующем паттерну `v*`. Он запускает сборку и тестирование на JDK 17 и 25, затем публикует GitHub Release с скомпилированными JAR'ами и извлечёнными заметками о выпуске.

```yaml
name: Release

on:
  push:
    tags:
      - "v*"

jobs:
  build-and-test:
    runs-on: ubuntu-latest
    permissions:
      contents: read
    strategy:
      matrix:
        java: [17, 25]
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: ${{ matrix.java }}
      - uses: gradle/actions/setup-gradle@v4
      - name: Build and test
        run: |
          VERSION="${GITHUB_REF_NAME#v}"
          ./gradlew check -Pversion="${VERSION}" --no-daemon
      - name: Upload JARs (JDK 17 only)
        if: matrix.java == 17
        uses: actions/upload-artifact@v4
        with:
          name: plugin-jars
          path: |
            mutaktor-gradle-plugin/build/libs/*.jar
            mutaktor-pitest-filter/build/libs/*.jar

  release:
    runs-on: ubuntu-latest
    needs: build-and-test
    permissions:
      contents: write
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0
      - uses: actions/download-artifact@v4
        with:
          name: plugin-jars
          path: artifacts/
      - name: Extract release notes
        run: |
          VERSION="${GITHUB_REF_NAME#v}"
          awk -v ver="$VERSION" '
            /^## / { if (found) exit; if ($0 ~ ver) { found=1; next } }
            found { print }
          ' CHANGELOG.md > release-notes.md
      - name: Create GitHub Release
        run: |
          gh release create "$GITHUB_REF_NAME" \
            --title "mutaktor ${GITHUB_REF_NAME}" \
            --notes-file release-notes.md \
            artifacts/**/*.jar
        env:
          GH_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

Ключевые решения:

| Решение | Обоснование |
|----------|-----------|
| Версия извлекается из тега (`v*` → `*`) | Версия в `gradle.properties` должна совпадать с тегом без префикса `v` |
| JAR'ы берутся только из сборки JDK 17 | Воспроизводимый артефакт; версия JDK не должна влиять на содержимое JAR |
| `fetch-depth: 0` в release-задании | `awk`-скрипту нужна полная история, чтобы найти правильный раздел `CHANGELOG.md` |
| Заметки о выпуске извлекаются через `awk` | Полная автоматизация — никакого ручного копирования из CHANGELOG в тело релиза |

### Обзор release-воркфлоу

```kroki-mermaid
graph TD
    tag["git push tag v*"] --> matrix2["Матрица: JDK 17 / 25\nbuild-and-test"]
    matrix2 --> jar["Загрузка JAR'ов\n(только JDK 17)"]
    jar --> rel["задание release\n(needs: build-and-test)"]
    rel --> notes["Извлечение заметок о выпуске\nиз CHANGELOG.md"]
    notes --> ghrel["gh release create\n+ прикрепление JAR'ов"]

    style tag fill:#e37400,color:#fff
    style ghrel fill:#181717,color:#fff
```

---

## Использование Mutaktor в вашем CI

### Минимальный пример

Следующий воркфлоу запускает мутационное тестирование при каждом pull request'е и загружает HTML-отчёт как артефакт:

```yaml
name: Mutation Testing

on:
  pull_request:
    branches: [main]

jobs:
  mutation:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 21

      - uses: gradle/actions/setup-gradle@v4

      - name: Run mutation tests
        run: ./gradlew mutate --no-daemon

      - name: Upload mutation report
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: mutation-report
          path: build/reports/mutaktor/
          retention-days: 7
```

### Git-diff-анализ с ограниченной областью

Для больших кодовых баз ограничивайте мутирование классами, изменёнными в ветке pull request'а:

```yaml
- name: Run mutation tests (changed classes only)
  run: ./gradlew mutate --no-daemon
  env:
    # Плагин читает это через mutaktor { since.set(...) } или --mutaktor-since
    MUTAKTOR_SINCE: ${{ github.base_ref }}
```

Или настройте статически в `build.gradle.kts`:

```kotlin
mutaktor {
    since.set("origin/main")
}
```

---

## GitHub Checks API

Когда `GithubChecksReporter` вызывается после задачи `mutate`, выжившие мутанты появляются как встроенные предупреждения в диффе pull request'а.

### Необходимые переменные окружения

| Переменная | Источник | Описание |
|----------|--------|-------------|
| `GITHUB_TOKEN` | `${{ secrets.GITHUB_TOKEN }}` | Аутентификация API |
| `GITHUB_REPOSITORY` | Устанавливается автоматически | Формат `owner/repo` |
| `GITHUB_SHA` | Устанавливается автоматически | SHA коммита для check run |

### Необходимое разрешение

Задание воркфлоу нуждается в доступе на запись к checks:

```yaml
jobs:
  mutation:
    runs-on: ubuntu-latest
    permissions:
      checks: write
      contents: read
```

### Полный пример с Checks API

```yaml
name: Mutation Testing with Checks

on:
  pull_request:
    branches: [main]

jobs:
  mutation:
    runs-on: ubuntu-latest
    permissions:
      checks: write
      contents: read

    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0    # необходимо для git-diff-анализа с ограниченной областью

      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 21

      - uses: gradle/actions/setup-gradle@v4

      - name: Run mutation tests
        run: ./gradlew mutate --no-daemon
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

`GithubChecksReporter` разбивает аннотации на группы по 50 (ограничение GitHub API за один запрос) и создаёт дополнительные PATCH-запросы для больших наборов результатов.

### Как работают аннотации

```kroki-mermaid
sequenceDiagram
    participant T as MutaktorTask
    participant Q as QualityGate
    participant R as GithubChecksReporter
    participant GH as GitHub API

    T->>Q: evaluate(mutations.xml, threshold)
    Q-->>T: Result(score=72, passed=false)
    T->>R: report(token, repo, sha, mutants, score, threshold)
    R->>GH: POST /repos/{owner}/{repo}/check-runs
    GH-->>R: { id: 12345, html_url: "..." }
    loop пакеты по 50
        R->>GH: PATCH /repos/{owner}/{repo}/check-runs/12345
    end
    Note over GH: Аннотации видны в диффе PR
```

---

## Загрузка SARIF в Code Scanning

Вывод SARIF позволяет выжившим мутантам появляться как предупреждения Code Scanning на вкладке Security вашего репозитория. Предупреждения сохраняются между запусками и могут быть отклонены с указанием причины.

### Включение вывода SARIF

```kotlin
// build.gradle.kts
mutaktor {
    outputFormats.set(setOf("HTML", "XML"))  // XML обязателен как входные данные для SARIF
}
```

`SarifConverter` читает `mutations.xml` и выводит только **выжившие** мутации — уничтоженные мутации работают корректно и не попадают в отчёт.

### Шаг загрузки

```yaml
- name: Run mutation tests
  run: ./gradlew mutate --no-daemon

- name: Convert to SARIF
  run: |
    ./gradlew generateMutationSarif --no-daemon   # задача, подключённая плагином

- name: Upload SARIF to Code Scanning
  uses: github/codeql-action/upload-sarif@v3
  if: always()
  with:
    sarif_file: build/reports/mutaktor/mutations.sarif
    category: mutation-testing
```

### Структура SARIF

Каждая выжившая мутация становится SARIF-результатом со следующими полями:

| Поле SARIF | Значение |
|-------------|-------|
| `ruleId` | `mutation/survived` |
| `level` | `warning` |
| `message.text` | `Survived mutation: <описание PIT>` |
| `artifactLocation.uri` | Относительный путь к исходному файлу |
| `region.startLine` | Номер строки из XML PIT |

В записи tool driver указывается `"name": "Mutaktor (PIT)"` и строка версии PIT для отслеживаемости.

---

## Quality Gate

Quality Gate завершает сборку с ошибкой, когда mutation score опускается ниже настроенного порога.

```kotlin
// build.gradle.kts
mutaktor {
    // Явного свойства threshold пока нет — вычисляется после задачи через QualityGate.evaluate()
}
```

`QualityGate.evaluate()` вычисляет:

```
mutationScore = killedMutations * 100 / totalMutations
passed        = mutationScore >= threshold
```

Если `totalMutations == 0`, score равен 100 (нечего тестировать — считается пройденным).

### Типичная настройка порога в CI

```yaml
- name: Check quality gate
  run: |
    SCORE=$(./gradlew mutationScore --quiet)
    if [ "$SCORE" -lt 80 ]; then
      echo "Mutation score $SCORE% is below threshold 80%"
      exit 1
    fi
```

---

## Кэширование и инкрементальный анализ

`MutaktorTask` Mutaktor аннотирован `@CacheableTask`. Кэш сборки Gradle позволяет избежать повторного запуска PIT, когда входные данные не изменились:

```kotlin
@CacheableTask
public abstract class MutaktorTask : JavaExec() {
    @get:InputFiles
    @get:PathSensitive(RELATIVE)
    public abstract val sourceDirs: ConfigurableFileCollection

    @get:Classpath
    public abstract val additionalClasspath: ConfigurableFileCollection

    @get:OutputDirectory
    public abstract val reportDir: DirectoryProperty
}
```

Для совместного использования кэша сборки между запусками CI:

```yaml
- uses: gradle/actions/setup-gradle@v4
  with:
    cache-read-only: ${{ github.ref != 'refs/heads/main' }}
```

Для инкрементального анализа PIT между запусками настройте файлы истории:

```kotlin
mutaktor {
    historyInputLocation.set(layout.projectDirectory.file(".mutation-history"))
    historyOutputLocation.set(layout.projectDirectory.file(".mutation-history"))
}
```

Сохраняйте `.mutation-history` в кэш-артефакт между запусками CI:

```yaml
- name: Restore mutation history
  uses: actions/cache@v4
  with:
    path: .mutation-history
    key: mutation-history-${{ github.ref }}-${{ github.sha }}
    restore-keys: |
      mutation-history-${{ github.ref }}-
      mutation-history-
```

---

## См. также

- [06-development.md](06-development.md) — Локальная настройка сборки и команды тестирования
- [08-changelog.md](08-changelog.md) — Процесс релиза: теги и триггер воркфлоу
- `SarifConverter.kt` — Реализация генерации SARIF
- `GithubChecksReporter.kt` — Реализация GitHub Checks API
- `QualityGate.kt` — Логика оценки порога
