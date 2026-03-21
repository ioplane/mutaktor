---
id: git-integration
title: Git-Diff Scoped Analysis
sidebar_label: Git Integration
---

# Git-Diff Scoped Analysis

![Feature](https://img.shields.io/badge/feature-since%20property-blue)
![CI](https://img.shields.io/badge/CI-GitHub%20Actions-2088FF?logo=github-actions&logoColor=white)
![Git](https://img.shields.io/badge/requires-git%20in%20PATH-F05032?logo=git&logoColor=white)

## Overview

Full mutation testing on a large codebase can take minutes or hours. On a feature branch where only a handful of files changed, running PIT against the entire `com.example.*` glob is wasteful: the vast majority of that time is spent mutating code that was not touched by the change under review.

The `since` property enables **git-diff scoped analysis**: Mutaktor runs `git diff` between the current `HEAD` and a reference commit, collects the `.kt` and `.java` files that were added, copied, modified, or renamed, converts those file paths to fully-qualified class name patterns, and passes only those patterns to PIT as `--targetClasses`.

The result is a mutation run that is proportional in cost to the size of the change rather than the size of the whole codebase.

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
    else no changed source files
        P->>T: targetClasses = extension.targetClasses (fallback)
        note over P: lifecycle log: "no changed classes since '...'"
    end
```

### git diff Command

`GitDiffAnalyzer` runs exactly this command:

```
git diff --name-only --diff-filter=ACMR sinceRef..HEAD
```

| Flag | Meaning |
|---|---|
| `--name-only` | Output only the file names, one per line |
| `--diff-filter=ACMR` | Include only Added, Copied, Modified, and Renamed files; Deleted files are excluded because there is nothing to mutate |
| `sinceRef..HEAD` | Two-dot range: all commits reachable from `HEAD` but not from `sinceRef` |

The command runs in the project directory (`projectDir`). If git exits with a non-zero code, a `RuntimeException` is thrown with the stderr output, failing the build with a clear message.

### File Path to Class Name Conversion

For each file path returned by `git diff`, `GitDiffAnalyzer.filePathToClassName` performs the following steps:

1. Checks that the file extension is `kt` or `java`. Other files (resources, build scripts, markdown) are ignored.
2. Resolves the path to an absolute canonical path.
3. Iterates over all configured source directories (`src/main/kotlin`, `src/main/java`, any custom Kotlin source roots) and finds the source directory that is a parent of the file.
4. Computes the path relative to that source directory.
5. Strips the file extension and replaces path separators with `.` to produce a fully-qualified class name.
6. Appends `*` to match the class itself and any inner classes (e.g. companion objects, nested classes).

#### Example

Given:
- Source directory: `/project/src/main/kotlin`
- Changed file: `src/main/kotlin/com/example/service/UserService.kt`

The conversion produces: `com.example.service.UserService*`

PIT receives `--targetClasses=com.example.service.UserService*`, which matches `UserService`, `UserService$Companion`, `UserService$1`, and any other inner/anonymous classes.

## Configuration

```kotlin
mutaktor {
    since = "main"
}
```

The `since` property accepts any git ref:

| Value | Meaning |
|---|---|
| `"main"` | All commits on the current branch not yet merged into `main` |
| `"develop"` | All commits not yet merged into `develop` |
| `"HEAD~5"` | The last 5 commits on the current branch |
| `"v1.2.3"` | All commits since the `v1.2.3` tag |
| `"a1b2c3d"` | All commits since a specific commit SHA |

## Performance Benefits

The performance gain scales with the ratio of changed code to total codebase size.

| Scenario | Without `since` | With `since` |
|---|---|---|
| 2 files changed in a 500-class codebase | Mutates all 500 classes | Mutates 2 classes |
| Full feature branch (20 files changed) | Mutates all 500 classes | Mutates ~20 classes |
| No source changes (only docs/config changed) | Mutates all 500 classes | Falls back to full `targetClasses` |

Typical CI run time improvement: **10x to 50x** for a PR touching a small number of files.

## CI Usage with GitHub Actions

### PR Mutation Scope

The most common pattern is to scope mutation testing to the changes introduced by a pull request. Use `origin/main` (or your default branch) as the reference.

```yaml
# .github/workflows/mutation.yml
name: Mutation Testing

on:
  pull_request:
    branches: [main]

jobs:
  mutate:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          # Fetch enough history for the diff to work
          fetch-depth: 0

      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 17

      - name: Run scoped mutation testing
        run: ./gradlew mutate
        env:
          MUTATION_SINCE: origin/main

      - name: Upload mutation report
        uses: actions/upload-artifact@v4
        with:
          name: mutation-report
          path: build/reports/mutaktor/
```

In your `build.gradle.kts`, read the environment variable:

```kotlin
mutaktor {
    since = providers.environmentVariable("MUTATION_SINCE").orNull
    targetClasses = setOf("com.example.*")  // fallback when MUTATION_SINCE is not set
}
```

### Full Scan on Main Branch

On the main branch (after merge), run a full scan without scoping. Because `since` is not set, `targetClasses` is used as-is.

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
          java-version: 17

      - name: Run full mutation testing
        run: ./gradlew mutate
        # No MUTATION_SINCE → full scan against targetClasses
```

### Scheduled Weekly Full Scan

```yaml
name: Weekly Mutation Baseline

on:
  schedule:
    - cron: '0 3 * * 1'  # Monday 03:00 UTC

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
          java-version: 17

      - name: Full mutation scan
        run: ./gradlew mutate

      - name: Upload baseline report
        uses: actions/upload-artifact@v4
        with:
          name: mutation-baseline-${{ github.run_id }}
          path: build/reports/mutaktor/
          retention-days: 90
```

### Combining with Incremental History

For even faster repeated runs on the main branch, combine `since` with incremental analysis history. PIT will re-use results from the previous run for mutants whose surrounding code has not changed.

```kotlin
mutaktor {
    since = providers.environmentVariable("MUTATION_SINCE").orNull

    // Store history between runs using the GitHub Actions cache
    val historyFile = layout.projectDirectory.file(".mutation-history")
    historyInputLocation = historyFile
    historyOutputLocation = historyFile
}
```

```yaml
- name: Restore mutation history
  uses: actions/cache@v4
  with:
    path: .mutation-history
    key: mutation-history-${{ github.ref_name }}
    restore-keys: mutation-history-main

- name: Run mutation testing
  run: ./gradlew mutate
  env:
    MUTATION_SINCE: origin/main

- name: Save mutation history
  uses: actions/cache@v4
  with:
    path: .mutation-history
    key: mutation-history-${{ github.ref_name }}
```

## Edge Cases and Fallback Behavior

| Situation | Behavior |
|---|---|
| `since` is not set | `targetClasses` from the extension is used unchanged |
| `git diff` returns no `.kt` or `.java` files (e.g. only docs changed) | Falls back to `targetClasses`; logs `"no changed classes since '...'"` |
| `git` is not in `PATH` | `RuntimeException` thrown during task execution with the error message |
| `sinceRef` does not exist (typo, deleted branch) | `git diff` exits non-zero; `RuntimeException` thrown with git's stderr |
| File is outside all configured source directories | Path is silently skipped; only files under known source roots are mapped |

## GitDiffAnalyzer API

`GitDiffAnalyzer` is a Kotlin `object` (singleton). Its public API consists of one method:

```kotlin
object GitDiffAnalyzer {

    /**
     * Returns set of glob patterns (e.g. "com.example.Foo*") for classes
     * whose source files changed between sinceRef and HEAD.
     *
     * @param projectDir  the project root directory
     * @param sinceRef    git ref to diff against (branch, tag, SHA)
     * @param sourceDirs  source directories to map file paths to class names
     * @return set of fully-qualified class name patterns for PIT targetClasses
     */
    fun changedClasses(
        projectDir: File,
        sinceRef: String,
        sourceDirs: Set<File>,
    ): Set<String>
}
```

The `filePathToClassName` function is `internal` and visible for unit testing.

## Requirements

- `git` must be available in the `PATH` of the process running Gradle.
- The project directory must be inside a git repository.
- The `sinceRef` must resolve to a commit reachable from the current HEAD. Use `fetch-depth: 0` in `actions/checkout@v4` to ensure shallow clones do not truncate the history.

## See Also

- [Plugin Architecture](./01-architecture.md)
- [Configuration DSL Reference](./02-configuration.md)
- [Report Formats and Quality Gate](./05-reporting.md)
