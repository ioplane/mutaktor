---
id: architecture
title: Plugin Architecture
sidebar_label: Architecture
---

# Plugin Architecture

![Version](https://img.shields.io/badge/version-0.2.0-7F52FF?style=for-the-badge)
![Kotlin](https://img.shields.io/badge/Kotlin-2.3-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-9.4.1-02303A?style=for-the-badge&logo=gradle&logoColor=white)
![PIT](https://img.shields.io/badge/PIT-1.23.0-E37400?style=for-the-badge)
![Config Cache](https://img.shields.io/badge/Config_Cache-compatible-brightgreen?style=for-the-badge)

## Overview

Mutaktor is a Kotlin-first Gradle plugin for [PIT](https://pitest.org/) mutation testing. It wraps the PIT command-line runner in a fully lazy, configuration-cache-compatible Gradle task, adds Kotlin-specific junk mutation filtering, git-diff scoped analysis, GraalVM auto-detection, and a complete post-processing pipeline (JSON, SARIF, quality gate, per-package ratchet, GitHub Checks API) — all with zero external runtime dependencies.

The plugin is composed of four published or companion modules plus a shared build-logic convention plugin.

---

## Module Structure

| Module | Artifact | Purpose |
|--------|----------|---------|
| `mutaktor-gradle-plugin` | `io.github.ioplane.mutaktor` | Gradle plugin, DSL extension, `mutate` task, report converters, ratchet, toolchain detection |
| `mutaktor-pitest-filter` | companion JAR on PIT classpath | PIT `MutationInterceptor` SPI that filters Kotlin compiler-generated junk |
| `mutaktor-annotations` | `mutaktor-annotations.jar` | `@MutationCritical` and `@SuppressMutations` source-level annotations |
| `build-logic` | internal only | Convention plugins for shared Kotlin/publishing config |

### Source Tree

```
mutaktor/
├── mutaktor-gradle-plugin/
│   └── src/main/kotlin/io/github/ioplane/mutaktor/
│       ├── MutaktorPlugin.kt              # Plugin entry point
│       ├── MutaktorExtension.kt           # DSL extension (32 properties)
│       ├── MutaktorTask.kt                # JavaExec task + post-processing pipeline
│       ├── MutaktorAggregatePlugin.kt     # Multi-module report aggregation
│       ├── git/
│       │   └── GitDiffAnalyzer.kt         # git diff → FQN class patterns
│       ├── extreme/
│       │   └── ExtremeMutationConfig.kt   # Method-body removal mutators
│       ├── toolchain/
│       │   └── GraalVmDetector.kt         # GraalVM + Quarkus detection
│       ├── ratchet/
│       │   ├── MutationRatchet.kt         # Per-package score floor
│       │   └── RatchetBaseline.kt         # JSON baseline persistence
│       ├── report/
│       │   ├── MutationElementsConverter.kt  # XML → mutation-testing-elements JSON
│       │   ├── SarifConverter.kt             # XML → SARIF 2.1.0
│       │   ├── QualityGate.kt                # Mutation score threshold check
│       │   └── GithubChecksReporter.kt       # GitHub Checks API annotations
│       └── util/
│           ├── XmlParser.kt               # Secure SAX/DOM parsing utilities
│           ├── JsonBuilder.kt             # Zero-dependency JSON construction
│           └── SourcePathResolver.kt      # File path → FQN conversion
│
├── mutaktor-pitest-filter/
│   └── src/main/kotlin/io/github/ioplane/mutaktor/pitest/
│       └── KotlinJunkFilter.kt            # MutationInterceptorFactory + 5 filters
│
├── mutaktor-annotations/
│   └── src/main/kotlin/io/github/ioplane/mutaktor/annotations/
│       ├── MutationCritical.kt            # Enforces 100% mutation score
│       └── SuppressMutations.kt           # Excludes code from analysis
│
└── build-logic/
    └── src/main/kotlin/
        └── kotlin-conventions.gradle.kts  # Shared Kotlin + JVM toolchain config
```

---

## Data Flow

The following diagram shows how configuration flows from `build.gradle.kts` through the plugin into PIT and then through the post-processing pipeline.

```mermaid
flowchart TD
    A["build.gradle.kts\n(mutaktor { ... })"] --> B["MutaktorExtension\n32 Provider properties"]
    B --> C{since set?}
    C -- yes --> D["GitDiffAnalyzer\ngit diff --name-only\nsinceRef..HEAD"]
    D --> E["Changed class FQN patterns\ncom.example.Foo*"]
    C -- no --> F["targetClasses from extension\ncom.example.*"]
    E --> G["MutaktorTask\nJavaExec"]
    F --> G
    B --> G
    B --> GVM["GraalVmDetector\nisGraalVm() + hasQuarkus()"]
    GVM -->|"auto-select toolchain"| G
    G --> H["buildPitArguments()\n--targetClasses --mutators\n--reportDir ..."]
    H --> I["PIT CLI\nMutationCoverageReport\n(child JVM)"]
    I --> J["mutations.xml\nPIT native XML"]
    I --> K["index.html\nPIT HTML report"]

    J --> PP["postProcess()"]
    PP --> L["MutationElementsConverter\nmutations.json"]
    PP --> M["SarifConverter\nmutations.sarif.json"]
    PP --> N["QualityGate\nscore >= threshold?"]
    PP --> O["MutationRatchet\nper-package floor check"]
    N --> P["GithubChecksReporter\nCheck Run + annotations"]
    O --> P

    style G fill:#02303A,color:#fff
    style I fill:#e37400,color:#fff
    style P fill:#181717,color:#fff
    style O fill:#2d8a4e,color:#fff
```

---

## Classpath Architecture

Mutaktor creates a dedicated `mutaktor` Gradle configuration to manage the PIT classpath. This keeps PIT dependencies completely separate from the project's own compile and runtime dependencies.

```mermaid
flowchart LR
    subgraph "mutaktor configuration (resolved at task execution)"
        P1["org.pitest:pitest-command-line:1.23.0"]
        P2["org.pitest:pitest-junit5-plugin:1.2.3"]
        P3["mutaktor-pitest-filter (project dep or published JAR)"]
    end

    subgraph "MutaktorTask classpath inputs"
        C1["launchClasspath\n(pitest-command-line + filter JAR)"]
        C2["additionalClasspath\n(test runtimeClasspath)"]
        C3["mutableCodePaths\n(build/classes/kotlin/main)"]
    end

    P1 --> C1
    P2 --> C1
    P3 --> C1
    C2 -->|"--classPathFile or --classPath"| PIT["PIT CLI"]
    C3 -->|"--mutableCodePaths"| PIT
    C1 -->|"exec classpath"| PIT
```

When `useClasspathFile = true` (the default), the `additionalClasspath` and `mutableCodePaths` entries are written to `build/mutaktor/pitClasspath` (one path per line) and passed via `--classPathFile`. This avoids OS command-line length limits on Windows and large monorepo builds.

---

## Post-Processing Pipeline

After PIT completes, `MutaktorTask.postProcess()` runs five sequential steps. Each step is guarded: if `mutations.xml` does not exist (PIT produced no output or failed), the entire post-processing phase is skipped with a warning.

```mermaid
flowchart TD
    Start["PIT exec() completes"] --> Check{"mutations.xml\nexists?"}
    Check -- No --> Warn["logger.warn: skipping post-processing"]
    Check -- Yes --> S1

    S1{"jsonReport\n== true?"} -- Yes --> J["MutationElementsConverter\n→ mutations.json"]
    S1 -- No --> S2
    J --> S2

    S2{"sarifReport\n== true?"} -- Yes --> SAR["SarifConverter\n→ mutations.sarif.json"]
    S2 -- No --> S3
    SAR --> S3

    S3{"mutationScoreThreshold\nset?"} -- Yes --> QG["QualityGate.evaluate()\nscore < threshold\n→ GradleException"]
    S3 -- No --> S4
    QG --> S4

    S4{"ratchetEnabled\n== true?"} -- Yes --> RAT["MutationRatchet.evaluate()\npackage regression\n→ GradleException"]
    S4 -- No --> S5
    RAT --> S5

    S5{"GITHUB_TOKEN\nGITHUB_REPOSITORY\nGITHUB_SHA set?"} -- Yes --> GH["GithubChecksReporter\nPOST Check Run\nPATCH annotations"]
    S5 -- No --> Done["Done"]
    GH --> Done

    style QG fill:#cc3333,color:#fff
    style RAT fill:#cc3333,color:#fff
    style GH fill:#181717,color:#fff
```

---

## Key Classes

| Class | Package | Role |
|-------|---------|------|
| `MutaktorPlugin` | `io.github.ioplane.mutaktor` | `Plugin<Project>` entry point; creates the `mutaktor` configuration and registers the `mutate` task with lazy wiring |
| `MutaktorExtension` | `io.github.ioplane.mutaktor` | Type-safe DSL; all 32 properties use the Gradle Provider API for lazy evaluation and configuration-cache compatibility |
| `MutaktorTask` | `io.github.ioplane.mutaktor` | `@CacheableTask` extending `JavaExec`; assembles the PIT CLI argument list from Provider values, delegates to `super.exec()`, then runs the post-processing pipeline |
| `MutaktorAggregatePlugin` | `io.github.ioplane.mutaktor` | Optional root-project plugin; registers `mutateAggregate` (`Copy` task) that collects subproject reports |
| `GitDiffAnalyzer` | `io.github.ioplane.mutaktor.git` | Runs `git diff --name-only --diff-filter=ACMR sinceRef..HEAD` and converts file paths to FQN glob patterns |
| `GraalVmDetector` | `io.github.ioplane.mutaktor.toolchain` | Detects GraalVM + Quarkus combination; auto-resolves a standard JDK via `JavaToolchainService` for PIT child process |
| `ExtremeMutationConfig` | `io.github.ioplane.mutaktor.extreme` | Holds the 6 method-body removal mutators used in extreme mode |
| `KotlinJunkFilter` | `io.github.ioplane.mutaktor.pitest` | PIT `MutationInterceptor` with 5 predicates that discard compiler-generated noise mutations |
| `KotlinJunkFilterFactory` | `io.github.ioplane.mutaktor.pitest` | `MutationInterceptorFactory` discovered via `META-INF/services`; registers the `KOTLIN_JUNK` feature flag |
| `MutationElementsConverter` | `io.github.ioplane.mutaktor.report` | Parses `mutations.xml` and emits mutation-testing-elements JSON (Stryker Dashboard schema v2) |
| `SarifConverter` | `io.github.ioplane.mutaktor.report` | Parses `mutations.xml` and emits SARIF 2.1.0; only survived mutations are included as results |
| `QualityGate` | `io.github.ioplane.mutaktor.report` | Computes kill ratio and compares against a threshold; returns a typed `Result` |
| `MutationRatchet` | `io.github.ioplane.mutaktor.ratchet` | Computes per-package scores from `mutations.xml`; fails if any package drops below its baseline |
| `RatchetBaseline` | `io.github.ioplane.mutaktor.ratchet` | Reads and writes the JSON baseline file (`.mutaktor-baseline.json`) |
| `GithubChecksReporter` | `io.github.ioplane.mutaktor.report` | Posts a GitHub Check Run with warning annotations for each survived mutant via the GitHub Checks API |

---

## Plugin Application Lifecycle

```mermaid
sequenceDiagram
    participant G as Gradle
    participant MP as MutaktorPlugin
    participant EXT as MutaktorExtension
    participant GVM as GraalVmDetector
    participant CONF as mutaktor Configuration
    participant TASK as mutate Task

    G->>MP: apply(project)
    MP->>EXT: project.extensions.create("mutaktor")
    note over EXT: Conventions applied in init block
    MP->>G: plugins.withType(JavaPlugin)
    G-->>MP: JavaPlugin is present
    MP->>CONF: configurations.create("mutaktor")
    note over CONF: defaultDependencies: PIT + JUnit5 + filter JAR
    MP->>TASK: tasks.register("mutate", MutaktorTask)
    note over TASK: Lazy wiring: all properties set from extension Providers
    MP->>GVM: isGraalVm() && hasQuarkus(project)?
    GVM-->>MP: true / false
    alt GraalVM + Quarkus detected and javaLauncher not set
        MP->>TASK: javaLauncher.set(resolveStandardJdk(...))
    end
    TASK-->>G: mustRunAfter("test")
```

---

## Gradle Task Graph

```mermaid
flowchart LR
    compileKotlin --> compileTestKotlin
    compileTestKotlin --> test
    test -->|mustRunAfter| mutate
    mutate --> mutateAggregate
```

`mustRunAfter` (not `dependsOn`) means `mutate` does not automatically trigger `test`. In most workflows you invoke `./gradlew test mutate` or wire `mutate` into a CI step that runs after tests.

---

## Configuration Cache Compatibility

All properties in `MutaktorTask` use the Gradle Provider API (`Property`, `SetProperty`, `ListProperty`, `MapProperty`, `DirectoryProperty`, `RegularFileProperty`, `ConfigurableFileCollection`). No `Project` references are stored in task fields. The task is annotated `@CacheableTask` and all file inputs carry `@PathSensitive` annotations with the appropriate sensitivity level.

| Provider Type | Use Case |
|---------------|----------|
| `Property<T>` | Single scalar value (thread count, boolean flags, strings) |
| `SetProperty<T>` | Unordered set (class patterns, mutator names) |
| `ListProperty<T>` | Ordered list (JVM args, PIT feature flags) |
| `MapProperty<K, V>` | Key-value pairs (plugin configuration) |
| `DirectoryProperty` | Output/input directory |
| `RegularFileProperty` | Single file (history files, baseline, classpath file) |
| `ConfigurableFileCollection` | Multiple files (source dirs, classpath, code paths) |

> **Warning:** Never store `Project` references in task fields. `Project` is not serializable for the configuration cache and will cause a cache miss or a hard failure on Gradle 9+.

---

## Zero External Dependencies

The production code in `mutaktor-gradle-plugin` has exactly **one** compile dependency: `org.pitest:pitest-command-line`. Everything else uses JDK standard library:

| Operation | Implementation |
|-----------|---------------|
| HTTP requests | `java.net.http.HttpClient` (JDK 11+) |
| XML parsing | `javax.xml.parsers.DocumentBuilderFactory` (SAX/DOM) |
| JSON generation | `StringBuilder` with manual escaping via `JsonBuilder` |
| File I/O | `java.io.File` |
| Process execution | Gradle `JavaExec` task type |

> **Note:** This constraint is intentional. Adding Jackson, OkHttp, Gson, or any other third-party library to the plugin JAR would increase the risk of dependency conflicts with consumer project classpaths.

---

## Aggregate Plugin

For multi-module builds, apply the aggregate plugin to the root project:

```kotlin
// root build.gradle.kts
plugins {
    id("io.github.ioplane.mutaktor.aggregate")
}
```

The `mutateAggregate` task copies each subproject's `build/reports/mutaktor/` into `build/reports/mutaktor-aggregate/<subprojectName>/` and automatically runs after each subproject's `mutate` task.

```mermaid
flowchart LR
    A["subproject-a: mutate"] --> AGG["root: mutateAggregate\nbuild/reports/mutaktor-aggregate/"]
    B["subproject-b: mutate"] --> AGG
    C["subproject-c: mutate"] --> AGG
```

---

## See Also

- [Configuration DSL Reference](./02-configuration.md)
- [Kotlin Junk Mutation Filter](./03-kotlin-filters.md)
- [Git-Diff Scoped Analysis](./04-git-integration.md)
- [Report Formats and Quality Gate](./05-reporting.md)
- [Development Guide](./06-development.md)
