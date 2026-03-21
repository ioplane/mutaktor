---
id: architecture
title: Plugin Architecture
sidebar_label: Architecture
---

# Plugin Architecture

![Kotlin](https://img.shields.io/badge/Kotlin-2.3-7F52FF?logo=kotlin&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-9.4.1-02303A?logo=gradle&logoColor=white)
![PIT](https://img.shields.io/badge/PIT-1.23.0-orange)
![JDK](https://img.shields.io/badge/JDK-17%2B-blue?logo=openjdk&logoColor=white)

## Overview

Mutaktor is a Kotlin-first Gradle plugin for [PIT](https://pitest.org/) mutation testing. It wraps the PIT command-line runner in a fully lazy, configuration-cache-compatible Gradle task, adds Kotlin-specific junk mutation filtering, git-diff scoped analysis, and multiple report output formats including SARIF for GitHub Code Scanning.

The plugin is composed of two published modules plus a shared build-logic module:

| Module | Artifact | Purpose |
|---|---|---|
| `mutaktor-gradle-plugin` | `io.github.dantte-lp.mutaktor` | Gradle plugin, DSL extension, `mutate` task, report converters |
| `mutaktor-pitest-filter` | companion JAR on PIT classpath | PIT `MutationInterceptor` that filters Kotlin compiler-generated junk |
| `build-logic` | internal only | Convention plugins for shared Kotlin/publishing config |

## Module Structure

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

## Data Flow

The following diagram shows how configuration flows from the build script through to PIT and then back into the report converters.

```kroki-mermaid
flowchart TD
    A["build.gradle.kts\n(mutaktor { ... })"] --> B[MutaktorExtension\n25 Provider properties]
    B --> C{since set?}
    C -- yes --> D[GitDiffAnalyzer\ngit diff --name-only\nsinceRef..HEAD]
    D --> E[Changed class FQN patterns]
    C -- no --> F[targetClasses from extension]
    E --> G[MutaktorTask\nJavaExec]
    F --> G
    B --> G
    G --> H[buildPitArguments\n--targetClasses --mutators\n--reportDir ...]
    H --> I[PIT CLI\nMutationCoverageReport]
    I --> J[mutations.xml\nPIT native report]
    I --> K[index.html\nPIT HTML report]
    J --> L[MutationElementsConverter\nStryker Dashboard JSON]
    J --> M[SarifConverter\nSARIF 2.1.0]
    J --> N[QualityGate\nmutation score check]
    N --> O[GithubChecksReporter\nCheck Run annotations]
```

## Classpath Architecture

Mutaktor creates a dedicated `mutaktor` Gradle configuration to manage the PIT classpath. This keeps PIT dependencies completely separate from the project's own dependencies.

```kroki-mermaid
flowchart LR
    subgraph "mutaktor configuration"
        P1["org.pitest:pitest-command-line:1.23.0"]
        P2["org.pitest:pitest-junit5-plugin:1.2.3"]
        P3["mutaktor-pitest-filter (local project dep)"]
    end
    subgraph "MutaktorTask inputs"
        C1["launchClasspath\n(pitest-command-line JAR)"]
        C2["additionalClasspath\n(test runtimeClasspath)"]
        C3["mutableCodePaths\n(build/classes/kotlin/main)"]
    end
    P1 --> C1
    P2 --> C1
    P3 --> C1
```

When `useClasspathFile = true` (the default), the `additionalClasspath` and `mutableCodePaths` entries are written to `build/mutaktor/pitClasspath` (one path per line) and passed to PIT via `--classPathFile`, avoiding OS command-line length limits.

## Key Classes

| Class | Package | Role |
|---|---|---|
| `MutaktorPlugin` | `io.github.dantte_lp.mutaktor` | `Plugin<Project>` entry point; creates the `mutaktor` configuration and registers the `mutate` task with lazy wiring |
| `MutaktorExtension` | `io.github.dantte_lp.mutaktor` | Type-safe DSL; all 25 properties use the Gradle Provider API for lazy evaluation and configuration-cache compatibility |
| `MutaktorTask` | `io.github.dantte_lp.mutaktor` | `@CacheableTask` extending `JavaExec`; assembles the PIT CLI argument list from Provider values and delegates to `super.exec()` |
| `MutaktorAggregatePlugin` | `io.github.dantte_lp.mutaktor` | Optional root-project plugin; registers `mutateAggregate` (`Copy` task) that collects subproject reports into one directory |
| `GitDiffAnalyzer` | `io.github.dantte_lp.mutaktor.git` | Runs `git diff --name-only --diff-filter=ACMR sinceRef..HEAD` and converts file paths to FQN glob patterns |
| `ExtremeMutationConfig` | `io.github.dantte_lp.mutaktor.extreme` | Holds the 6 method-body removal mutators used in extreme mode |
| `KotlinJunkFilter` | `io.github.dantte_lp.mutaktor.pitest` | PIT `MutationInterceptor` with 5 predicates that discard compiler-generated noise mutations |
| `KotlinJunkFilterFactory` | `io.github.dantte_lp.mutaktor.pitest` | `MutationInterceptorFactory` discovered via `META-INF/services`; registers the `KOTLIN_JUNK` feature flag |
| `MutationElementsConverter` | `io.github.dantte_lp.mutaktor.report` | Parses `mutations.xml` and emits mutation-testing-elements JSON (Stryker Dashboard schema v2) |
| `SarifConverter` | `io.github.dantte_lp.mutaktor.report` | Parses `mutations.xml` and emits SARIF 2.1.0; only survived mutations are included as results |
| `QualityGate` | `io.github.dantte_lp.mutaktor.report` | Computes kill ratio and compares against a threshold; returns a typed `Result` |
| `GithubChecksReporter` | `io.github.dantte_lp.mutaktor.report` | Posts a GitHub Check Run with warning annotations for each survived mutant via the GitHub Checks API |

## Plugin Application Lifecycle

```kroki-mermaid
sequenceDiagram
    participant G as Gradle
    participant MP as MutaktorPlugin
    participant EXT as MutaktorExtension
    participant CONF as mutaktor Configuration
    participant TASK as mutate Task

    G->>MP: apply(project)
    MP->>EXT: project.extensions.create("mutaktor")
    note over EXT: Conventions applied in init block
    MP->>G: plugins.withType(JavaPlugin)
    G-->>MP: JavaPlugin is present
    MP->>CONF: configurations.create("mutaktor")
    note over CONF: defaultDependencies: PIT + JUnit5 plugin + filter JAR
    MP->>TASK: tasks.register("mutate", MutaktorTask)
    note over TASK: Lazy wiring: all properties set from extension Providers
    TASK-->>G: mustRunAfter("test")
```

## Aggregate Plugin

For multi-module builds, apply the aggregate plugin to the root project alongside per-submodule `mutaktor` plugins:

```kotlin
// root build.gradle.kts
plugins {
    id("io.github.dantte-lp.mutaktor.aggregate")
}
```

The `mutateAggregate` task copies each subproject's `build/reports/mutaktor/` into `build/reports/mutaktor-aggregate/<subprojectName>/` and automatically runs after each subproject's `mutate` task.

## Gradle Task Graph

```kroki-mermaid
flowchart LR
    compileKotlin --> compileTestKotlin
    compileTestKotlin --> test
    test --> mutate
    mutate --> mutateAggregate
```

## Configuration Cache Compatibility

All properties in `MutaktorTask` use the Gradle Provider API (`Property`, `SetProperty`, `ListProperty`, `MapProperty`, `DirectoryProperty`, `RegularFileProperty`). No `Project` references are stored in task fields. The task is annotated `@CacheableTask` and all file inputs carry appropriate `@PathSensitive` annotations.

## See Also

- [Configuration DSL Reference](./02-configuration.md)
- [Kotlin Junk Mutation Filter](./03-kotlin-filters.md)
- [Git-Diff Scoped Analysis](./04-git-integration.md)
- [Report Formats and Quality Gate](./05-reporting.md)
