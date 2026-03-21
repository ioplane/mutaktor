---
id: git-integration
title: Git-Diff Scoped Analysis
sidebar_label: Git Integration
---

# Git-Diff Scoped Analysis

![Feature](https://img.shields.io/badge/property-since-7F52FF?style=for-the-badge)
![Git](https://img.shields.io/badge/requires-git_in_PATH-F05032?style=for-the-badge&logo=git&logoColor=white)
![CI](https://img.shields.io/badge/CI-GitHub_Actions-2088FF?style=for-the-badge&logo=github-actions&logoColor=white)

## Overview

Full mutation testing on a large codebase can take minutes or hours. On a feature branch where only a handful of files changed, running PIT against the entire `com.example.*` glob is wasteful: the vast majority of that time is spent mutating code that was not touched by the change under review.

The `since` property enables **git-diff scoped analysis**: Mutaktor runs `git diff` between the current `HEAD` and a reference commit, collects the `.kt` and `.java` files that were added, copied, modified, or renamed, converts those file paths to fully-qualified class name patterns, and passes only those patterns to PIT as `--targetClasses`.

The result is a mutation run whose cost is proportional to the **size of the change**, not the size of the whole codebase.

---

## How It Works

```kroki-mermaid
sequenceDiagram
    participant P as MutaktorPlugin
    participant G as GitDiffAnalyzer
    participant GIT as git process
    participant T as MutaktorTask

    P->>G: changedClasses(projectDir, sinceRef, sourceDirs)
    G->>GIT: git diff --name-only --diff-filter=ACMR sinceRef..HEAD
    GIT-->>G: src/main/kotlin/com/example/Foo.kt\nsrc/main/java/com/example/Bar.java
    G->>G: filePathToClassName for each path\n→ com.example.Foo*\n→ com.example.Bar*
    G-->>P: Set<String> of FQN patterns
    alt changed set is non-empty
        P->>T: targetClasses = changed FQN patterns
        note over P: log: "scoping to N changed classes since '...'"
    else no changed .kt or .java files
        P->>T: targetClasses = extension.targetClasses (fallback)
        note over P: log: "no changed classes since '...'"
    end
```

### The git diff Command

`GitDiffAnalyzer` runs exactly this command in the project directory:

```
git diff --name-only --diff-filter=ACMR sinceRef..HEAD
```

| Flag | Meaning |
|------|---------|
| `--name-only` | Output only file names, one per line |
| `--diff-filter=ACMR` | Include Added, Copied, Modified, and Renamed files; Deleted files are excluded (nothing left to mutate) |
| `sinceRef..HEAD` | Two-dot range: all commits reachable from `HEAD` but not from `sinceRef` |

If git exits with a non-zero code, a `RuntimeException` is thrown with the stderr output, failing the build with a clear error message.

### File Path to Class Name Conversion

For each file path returned by `git diff`, `GitDiffAnalyzer.filePathToClassName` performs the following steps:

1. Checks that the file extension is `kt` or `java`. Other files (resources, build scripts, markdown) are silently skipped.
2. Resolves the path to an absolute canonical path.
3. Iterates over all configured source directories (`src/main/kotlin`, `src/main/java`, and any custom Kotlin source roots) and finds the source directory that is a parent of the file.
4. Computes the path relative to that source directory.
5. Strips the file extension and replaces path separators with `.` to produce a fully-qualified class name.
6. Appends `*` to match the class itself and any inner classes (companion objects, nested classes, anonymous classes).

#### Conversion Example

```
Source directory:  /project/src/main/kotlin
Changed file:      src/main/kotlin/com/example/service/UserService.kt

Produced pattern:  com.example.service.UserService*
```

PIT receives `--targetClasses=com.example.service.UserService*`, which matches:
- `UserService`
- `UserService$Companion`
- `UserService$1` (anonymous class)
- `UserService$Builder` (inner class)

---

## Configuration

```kotlin
// Kotlin DSL
mutaktor {
    since = "main"
}
```

```groovy
// Groovy DSL
mutaktor {
    since = 'main'
}
```

The `since` property accepts any git ref:

| Value | Meaning |
|-------|---------|
| `"main"` | All commits on the current branch not yet merged into `main` |
| `"develop"` | All commits not yet merged into `develop` |
| `"HEAD~5"` | The last 5 commits on the current branch |
| `"v1.2.3"` | All commits since the `v1.2.3` tag |
| `"a1b2c3d"` | All commits since a specific commit SHA (7 or 40 chars) |
| `"origin/main"` | All commits since the remote main branch (useful in CI with `fetch-depth: 0`) |

---

## Performance Benefits

The performance gain scales with the ratio of changed code to total codebase size.

| Scenario | Without `since` | With `since` | Typical speedup |
|----------|----------------|--------------|-----------------|
| 2 files changed in 500-class codebase | Mutates all 500 classes | Mutates 2 classes | ~250x |
| Feature branch, 20 files changed | Mutates all 500 classes | Mutates ~20 classes | ~25x |
| No source changes (only docs/config) | Mutates all 500 classes | Falls back to full `targetClasses` | 1x |
| Single-class hotfix PR | Mutates all N classes | Mutates 1 class | ~N x |

> **Tip:** A typical CI run on a PR touching 3–5 files will complete in 30–90 seconds instead of 10–30 minutes on a medium-sized codebase.

---

## CI Usage with GitHub Actions

### PR Mutation Scope

The most common pattern is to scope mutation testing to the changes introduced by a pull request, using `origin/main` as the reference:

```yaml
# .github/workflows/mutation.yml
name: Mutation Testing

on:
  pull_request:
    branches: [main]

jobs:
  mutate:
    runs-on: ubuntu-latest
    permissions:
      checks: write
      contents: read

    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0     # required: git diff needs full history

      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 21

      - uses: gradle/actions/setup-gradle@v4

      - name: Run scoped mutation testing
        run: ./gradlew mutate --no-daemon
        env:
          MUTATION_SINCE: origin/main
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
          GITHUB_REPOSITORY: ${{ github.repository }}
          GITHUB_SHA: ${{ github.sha }}

      - name: Upload mutation report
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: mutation-report
          path: build/reports/mutaktor/
          retention-days: 7
```

In `build.gradle.kts`, read the environment variable:

```kotlin
mutaktor {
    since = providers.environmentVariable("MUTATION_SINCE").orNull
    targetClasses = setOf("com.example.*")   // fallback when MUTATION_SINCE is not set
    mutationScoreThreshold = 80
}
```

> **Note:** `fetch-depth: 0` is **required**. Without it, `actions/checkout` performs a shallow clone and `git diff sinceRef..HEAD` may fail because `sinceRef` is not in the local history.

### Full Scan on Main Branch

On the main branch (after merge), run a complete scan without scoping:

```yaml
name: Full Mutation Scan

on:
  push:
    branches: [main]

jobs:
  mutate:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0

      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 21

      - uses: gradle/actions/setup-gradle@v4

      - name: Run full mutation scan
        run: ./gradlew mutate --no-daemon
        # No MUTATION_SINCE → full scan against targetClasses
```

### Scheduled Weekly Full Scan

```yaml
name: Weekly Mutation Baseline

on:
  schedule:
    - cron: '0 3 * * 1'   # Monday 03:00 UTC

jobs:
  baseline:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0

      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 21

      - uses: gradle/actions/setup-gradle@v4

      - name: Full mutation scan
        run: ./gradlew mutate --no-daemon

      - name: Upload baseline report
        uses: actions/upload-artifact@v4
        with:
          name: mutation-baseline-${{ github.run_id }}
          path: build/reports/mutaktor/
          retention-days: 90
```

---

## Combining with Incremental History

For even faster repeated runs on the main branch, combine `since` with PIT's incremental analysis history. PIT re-uses results from the previous run for mutants whose surrounding code has not changed.

```kotlin
// build.gradle.kts
mutaktor {
    since = providers.environmentVariable("MUTATION_SINCE").orNull

    val historyFile = layout.projectDirectory.file(".mutation-history")
    historyInputLocation = historyFile
    historyOutputLocation = historyFile
}
```

```yaml
# .github/workflows/mutation.yml
- name: Restore mutation history
  uses: actions/cache@v4
  with:
    path: .mutation-history
    key: mutation-history-${{ github.ref_name }}-${{ github.sha }}
    restore-keys: |
      mutation-history-${{ github.ref_name }}-
      mutation-history-main-

- name: Run mutation testing
  run: ./gradlew mutate --no-daemon
  env:
    MUTATION_SINCE: origin/main

- name: Save mutation history
  uses: actions/cache@v4
  with:
    path: .mutation-history
    key: mutation-history-${{ github.ref_name }}-${{ github.sha }}
```

---

## Edge Cases and Fallback Behavior

| Situation | Behavior |
|-----------|----------|
| `since` is not set | `targetClasses` from the extension is used unchanged |
| `git diff` returns no `.kt` or `.java` files (only docs, config, or build scripts changed) | Falls back to `targetClasses`; logs `"no changed classes since '...'"` |
| Changed file is outside all configured source directories | File is silently skipped; only files under known source roots are mapped to class patterns |
| `git` is not in `PATH` | `RuntimeException` is thrown during task execution with the error message |
| `sinceRef` does not exist (typo, deleted branch) | `git diff` exits non-zero; `RuntimeException` is thrown with git's stderr |
| Shallow clone without `fetch-depth: 0` | `git diff` may fail because `sinceRef` commit is not present in the local history |

---

## GitDiffAnalyzer API

`GitDiffAnalyzer` is a Kotlin `object` (singleton). Its public surface consists of one method:

```kotlin
object GitDiffAnalyzer {

    /**
     * Returns a set of glob patterns (e.g. "com.example.Foo*") for classes
     * whose source files changed between sinceRef and HEAD.
     *
     * Returns an empty set if no relevant source files changed — the caller
     * should fall back to the extension's targetClasses in this case.
     *
     * @param projectDir  the project root directory (git working tree)
     * @param sinceRef    git ref to diff against (branch name, tag, or commit SHA)
     * @param sourceDirs  source directories used to map file paths to class names
     */
    fun changedClasses(
        projectDir: File,
        sinceRef: String,
        sourceDirs: Set<File>,
    ): Set<String>
}
```

The `filePathToClassName` function is `internal` and is covered by unit tests in `GitDiffAnalyzerTest`.

---

## Requirements

- `git` must be available in the `PATH` of the process running Gradle.
- The project directory must be inside a git repository.
- The `sinceRef` must resolve to a commit reachable from the current HEAD.
- Use `fetch-depth: 0` in `actions/checkout@v4` to ensure shallow clones do not truncate the relevant history.

---

## See Also

- [Plugin Architecture](./01-architecture.md)
- [Configuration DSL Reference](./02-configuration.md#git-aware-analysis)
- [Report Formats and Quality Gate](./05-reporting.md)
- [CI/CD Integration](./07-ci-cd.md)
