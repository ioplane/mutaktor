# CLAUDE.md — mutaktor

## Project Overview

Kotlin-first Gradle plugin for PIT mutation testing with git-aware analysis, Kotlin junk filtering, and CI/CD integration.

**Plugin ID:** `io.github.dantte-lp.mutaktor`
**Aggregate Plugin ID:** `io.github.dantte-lp.mutaktor.aggregate`
**Group:** `io.github.dantte-lp.mutaktor`
**Language:** Kotlin 2.3 | **Build:** Gradle 9.4.1 (Kotlin DSL)
**Test:** JUnit 5 + Kotest assertions + Gradle TestKit
**Min JDK:** 17 | **Target JDK:** 25 | **PIT:** 1.23.0
**Version:** 0.1.0-SNAPSHOT (SemVer 2.0.0)
**External deps:** 0 (only Gradle API + PIT)

## Modules

```
mutaktor-gradle-plugin/     # Plugin: DSL, task, git-diff, reporting, CI/CD
  MutaktorPlugin.kt              # Plugin entry — wires extension→task, manages deps
  MutaktorExtension.kt           # 25 abstract managed properties, conventions
  MutaktorTask.kt                # @CacheableTask JavaExec, PIT CLI arg builder
  MutaktorAggregatePlugin.kt     # Multi-module report aggregation
  git/GitDiffAnalyzer.kt         # git diff → targetClasses filter
  report/MutationElementsConverter.kt  # PIT XML → mutation-testing-elements JSON
  report/SarifConverter.kt       # PIT XML → SARIF 2.1.0
  report/QualityGate.kt          # Score evaluation + threshold check
  report/GithubChecksReporter.kt # GitHub Checks API annotations
  extreme/ExtremeMutationConfig.kt # Method-body removal mutators

mutaktor-pitest-filter/     # PIT plugin JAR (loaded by PIT runtime)
  pitest/KotlinJunkFilter.kt     # MutationInterceptor SPI — 5 filter patterns

build-logic/                # Convention plugins
```

## Commands

```bash
./gradlew check                    # unit + functional tests
./gradlew test                     # unit tests only
./gradlew functionalTest           # Gradle TestKit tests
./gradlew :mutaktor-pitest-filter:test  # filter tests only
```

## Key Architecture Decisions

- All task inputs: Provider API (`Property<T>`, `SetProperty<T>`, etc.) — lazy by design
- PIT execution: `JavaExec` child JVM, not `project.exec()`
- Reports: zero external JSON deps — `StringBuilder` based
- Git: `ProcessBuilder` → `git diff --name-only --diff-filter=ACMR`
- Kotlin filters: PIT `MutationInterceptor` SPI via `META-INF/services`
- GitHub API: `java.net.http.HttpClient` (JDK 11+)

## Versioning

- SemVer 2.0.0: version in `gradle.properties`
- Tags: `v*` format (not axion-release)
- CHANGELOG: Keep a Changelog 1.1.0

## QA Known Issues (v0.2.0 backlog)

1. MutaktorTask.buildPitArguments() — 0 unit tests (core logic!)
2. Hardcoded `src/main/java/` in report converters — breaks for Kotlin sources
3. Configuration cache test disabled — claimed feature not validated
4. GithubChecksReporter — 0 tests
5. Duplicated escapeJson/XML utils across 4 files

## GraalVM Compatibility

PIT minion JVM fails on GraalVM `jrt:/` module paths. Fix planned for v0.2.0:
`javaLauncher` property via Gradle Toolchain API (use Temurin for PIT child).

## Don'ts

- Do NOT use Groovy
- Do NOT use `project.exec()` / `project.javaexec()` — removed in Gradle 9
- Do NOT use eager task APIs (`tasks.create`) — use `tasks.register`
- Do NOT store Project references in task fields — configuration cache incompatible
- Do NOT use `buildDir` — use `layout.buildDirectory`
- Do NOT add external dependencies — zero-dep policy
- Do NOT hardcode `src/main/java/` — detect Kotlin sources too
- Do NOT run builds outside container
