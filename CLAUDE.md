# CLAUDE.md — mutaktor

## Project Overview

Kotlin-first Gradle plugin for PIT mutation testing.
Plugin ID: `io.github.dantte-lp.mutaktor`

**Language:** Kotlin 2.3
**Build:** Gradle 9.4.1 (Kotlin DSL)
**Test:** JUnit 5 + Kotest assertions + Gradle TestKit
**Min JDK:** 17 | **Target JDK:** 25

## Modules

- `mutaktor-gradle-plugin` — Gradle plugin (DSL, task, reporting)
- `mutaktor-pitest-filter` — PIT plugin JAR (Kotlin junk mutation filters)
- `build-logic` — Convention plugins (shared build config)

## Commands (inside container)

```bash
./gradlew build                    # compile + unit tests
./gradlew test                     # unit tests only
./gradlew functionalTest           # Gradle TestKit tests
./gradlew check                    # test + functionalTest
```

## Versioning

- SemVer 2.0.0: version in `gradle.properties`
- Tags: `v*` format
- CHANGELOG: Keep a Changelog 1.1.0

## Don'ts

- Do NOT use Groovy
- Do NOT use `project.exec()` / `project.javaexec()` — removed in Gradle 9
- Do NOT use eager task APIs (`tasks.create`) — use `tasks.register`
- Do NOT store Project references in task fields — configuration cache incompatible
- Do NOT use `buildDir` — use `layout.buildDirectory`
