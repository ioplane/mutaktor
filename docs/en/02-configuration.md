---
id: configuration
title: Configuration DSL Reference
sidebar_label: Configuration
---

# Configuration DSL Reference

![Gradle](https://img.shields.io/badge/Gradle-9.4.1-02303A?logo=gradle&logoColor=white)
![Kotlin DSL](https://img.shields.io/badge/DSL-Kotlin-7F52FF?logo=kotlin&logoColor=white)
![Config Cache](https://img.shields.io/badge/Configuration%20Cache-compatible-brightgreen)

## Overview

All plugin options are declared in the `mutaktor` extension block. Every property is backed by the Gradle Provider API, which means values are resolved lazily at task execution time — not at configuration time. This makes the plugin fully compatible with Gradle's configuration cache and isolated projects mode.

## Quick Start

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

## Property Reference

### Core

These properties control which classes are mutated, how many mutants are generated, and which version of PIT is used.

| Property | Type | Default | Description |
|---|---|---|---|
| `pitVersion` | `Property<String>` | `"1.23.0"` | PIT version resolved from Maven Central |
| `targetClasses` | `SetProperty<String>` | `setOf("$group.*")` | Glob patterns selecting classes to mutate |
| `targetTests` | `SetProperty<String>` | _(PIT auto-detect)_ | Glob patterns selecting test classes to run |
| `threads` | `Property<Int>` | `availableProcessors()` | Number of parallel mutation analysis threads |
| `mutators` | `SetProperty<String>` | `setOf("DEFAULTS")` | Mutator groups or individual mutator names |
| `timeoutFactor` | `Property<BigDecimal>` | `1.25` | Multiplier applied to normal test execution time to compute per-mutant timeout |
| `timeoutConstant` | `Property<Int>` | `4000` | Constant (ms) added to the computed timeout for each mutant |

#### Mutator Groups

PIT ships with predefined mutator groups. Any combination of groups and individual mutator names can be provided.

| Group | Description |
|---|---|
| `DEFAULTS` | Standard mutation operators — the baseline for most projects |
| `STRONGER` | More aggressive operators, produces more mutants, may increase analysis time |
| `ALL` | Every available mutator; use only on small codebases |

Individual mutators can be mixed with groups:

```kotlin
mutaktor {
    mutators = setOf("DEFAULTS", "UOI", "AOR")
}
```

#### Kotlin DSL — Core Example

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

#### Groovy DSL — Core Example

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

### Filtering

Use filtering properties to exclude generated code, framework boilerplate, and infrastructure classes that are not meaningful targets for mutation testing.

| Property | Type | Default | Description |
|---|---|---|---|
| `excludedClasses` | `SetProperty<String>` | _(empty)_ | Glob patterns for classes excluded from mutation |
| `excludedMethods` | `SetProperty<String>` | _(empty)_ | Method-name patterns excluded from mutation; supports simple wildcards |
| `excludedTestClasses` | `SetProperty<String>` | _(empty)_ | Glob patterns for test classes excluded from test execution |
| `avoidCallsTo` | `SetProperty<String>` | _(empty)_ | Fully-qualified package prefixes whose method calls are replaced with NO-OPs during analysis (e.g. logging) |

#### Kotlin DSL — Filtering Example

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

#### Groovy DSL — Filtering Example

```groovy
mutaktor {
    excludedClasses = ['com.example.generated.*', 'com.example.config.*'] as Set
    excludedMethods = ['toString', 'hashCode', 'equals'] as Set
    avoidCallsTo = ['kotlin.jvm.internal', 'org.slf4j'] as Set
}
```

### Reporting

| Property | Type | Default | Description |
|---|---|---|---|
| `reportDir` | `DirectoryProperty` | `build/reports/mutaktor` | Directory where PIT writes its output |
| `outputFormats` | `SetProperty<String>` | `setOf("HTML", "XML")` | Output formats to generate; see table below |
| `timestampedReports` | `Property<Boolean>` | `false` | When `true`, PIT creates a timestamped sub-directory per run rather than overwriting |

#### Output Formats

| Value | Description |
|---|---|
| `HTML` | Interactive HTML report with line-level mutation highlighting — the default PIT report |
| `XML` | `mutations.xml` machine-readable report; required for SARIF and Stryker Dashboard conversion |
| `CSV` | Tab-separated summary file |

#### Kotlin DSL — Reporting Example

```kotlin
mutaktor {
    reportDir = layout.buildDirectory.dir("reports/mutation")
    outputFormats = setOf("HTML", "XML")
    timestampedReports = false
}
```

### Test Configuration

| Property | Type | Default | Description |
|---|---|---|---|
| `junit5PluginVersion` | `Property<String>` | `"1.2.3"` | Version of `org.pitest:pitest-junit5-plugin` resolved from Maven Central |
| `includedGroups` | `SetProperty<String>` | _(empty)_ | JUnit 5 tag expressions for tests to include |
| `excludedGroups` | `SetProperty<String>` | _(empty)_ | JUnit 5 tag expressions for tests to exclude |
| `fullMutationMatrix` | `Property<Boolean>` | `false` | When `true`, every mutant is tested against every test without early exit; significantly increases run time |

#### Kotlin DSL — Test Configuration Example

```kotlin
mutaktor {
    junit5PluginVersion = "1.2.3"
    includedGroups = setOf("unit", "integration")
    excludedGroups = setOf("slow", "e2e")
    fullMutationMatrix = false
}
```

### Advanced / JVM

| Property | Type | Default | Description |
|---|---|---|---|
| `jvmArgs` | `ListProperty<String>` | _(empty)_ | Extra JVM arguments passed to the **forked** child test processes (e.g. `--add-opens`, `-Xmx`) |
| `mainProcessJvmArgs` | `ListProperty<String>` | _(empty)_ | Extra JVM arguments passed to the **main** PIT analysis process (not the child workers) |
| `pluginConfiguration` | `MapProperty<String, String>` | _(empty)_ | Key-value pairs forwarded to PIT plugins via `--pluginConfiguration`; keys follow `pluginName.key` pattern |
| `features` | `ListProperty<String>` | _(empty)_ | PIT feature flags to enable (`+flagName`) or disable (`-flagName`) |
| `verbose` | `Property<Boolean>` | `false` | Enable verbose PIT console output; useful for debugging classpath or configuration problems |
| `useClasspathFile` | `Property<Boolean>` | `true` | When `true`, writes the classpath to `build/mutaktor/pitClasspath` and passes it via `--classPathFile`, avoiding OS argument-length limits |

#### Kotlin DSL — Advanced Example

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

### Git-Aware Analysis

| Property | Type | Default | Description |
|---|---|---|---|
| `since` | `Property<String>` | _(not set)_ | Git ref to diff against (branch name, tag, or commit SHA). When set, only classes changed since this ref are mutated. |

When `since` is set and `git diff` returns no changed source files, the plugin falls back to the configured `targetClasses` and logs a lifecycle message.

```kotlin
mutaktor {
    // Mutate only classes changed since the main branch
    since = "main"

    // Or scope to the last 5 commits
    // since = "HEAD~5"

    // Or a specific commit SHA
    // since = "a1b2c3d"
}
```

See [Git-Diff Scoped Analysis](./04-git-integration.md) for full details.

### Kotlin Junk Mutation Filter

| Property | Type | Default | Description |
|---|---|---|---|
| `kotlinFilters` | `Property<Boolean>` | `true` | Enable the built-in `KotlinJunkFilter` that suppresses mutations in Kotlin compiler-generated bytecode |

When enabled, the `mutaktor-pitest-filter` JAR is added to the PIT classpath. In a multi-module build that includes the `:mutaktor-pitest-filter` subproject, the local project dependency is used. In a standalone build, the JAR must be available as a published artifact.

```kotlin
mutaktor {
    kotlinFilters = true  // default; suppress data-class, coroutine, null-check noise
}
```

See [Kotlin Junk Mutation Filter](./03-kotlin-filters.md) for a description of all 5 filter patterns.

### Extreme Mutation Mode

| Property | Type | Default | Description |
|---|---|---|---|
| `extreme` | `Property<Boolean>` | `false` | Replace fine-grained mutators with method-body removal operators. Produces ~1 mutant per method instead of ~10, making analysis practical on large codebases. |

When `extreme = true`, the `mutators` property is overridden with the 6 method-body removal operators regardless of what is configured:

| Mutator | Effect |
|---|---|
| `VOID_METHOD_CALLS` | Removes calls to void methods |
| `EMPTY_RETURNS` | Replaces object returns with empty/default values |
| `FALSE_RETURNS` | Replaces boolean returns with `false` |
| `TRUE_RETURNS` | Replaces boolean returns with `true` |
| `NULL_RETURNS` | Replaces object returns with `null` |
| `PRIMITIVE_RETURNS` | Replaces primitive returns with `0` |

```kotlin
mutaktor {
    extreme = true  // overrides mutators; ignores any mutators = setOf(...) configuration
}
```

### Incremental Analysis

| Property | Type | Default | Description |
|---|---|---|---|
| `historyInputLocation` | `RegularFileProperty` | _(not set)_ | File to read previous mutation analysis state from; enables incremental analysis |
| `historyOutputLocation` | `RegularFileProperty` | _(not set)_ | File to write mutation analysis state to after a run |

```kotlin
mutaktor {
    val historyFile = layout.projectDirectory.file(".mutation-history")
    historyInputLocation = historyFile
    historyOutputLocation = historyFile
}
```

## Complete Configuration Example

### Kotlin DSL

```kotlin
// build.gradle.kts
plugins {
    kotlin("jvm") version "2.3.0"
    id("io.github.dantte-lp.mutaktor") version "x.y.z"
}

mutaktor {
    // Core
    pitVersion = "1.23.0"
    targetClasses = setOf("com.example.*")
    threads = Runtime.getRuntime().availableProcessors()
    mutators = setOf("DEFAULTS")
    timeoutFactor = java.math.BigDecimal("1.25")
    timeoutConstant = 4000

    // Filtering
    excludedClasses = setOf("com.example.generated.*")
    excludedMethods = setOf("toString", "hashCode", "equals")
    avoidCallsTo = setOf("kotlin.jvm.internal", "org.slf4j")

    // Reporting
    outputFormats = setOf("HTML", "XML")
    timestampedReports = false

    // Test
    junit5PluginVersion = "1.2.3"

    // Advanced
    jvmArgs = listOf("-Xmx2g")
    useClasspathFile = true
    verbose = false

    // Git-aware analysis
    since = providers.environmentVariable("MUTATION_SINCE").orNull

    // Kotlin filter
    kotlinFilters = true

    // Incremental
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

## Defaults Table

| Property | Default Value |
|---|---|
| `pitVersion` | `"1.23.0"` |
| `targetClasses` | `setOf("$project.group.*")` |
| `targetTests` | _(PIT auto-detect from targetClasses)_ |
| `threads` | `Runtime.getRuntime().availableProcessors()` |
| `mutators` | `setOf("DEFAULTS")` |
| `timeoutFactor` | `BigDecimal("1.25")` |
| `timeoutConstant` | `4000` |
| `excludedClasses` | _(empty)_ |
| `excludedMethods` | _(empty)_ |
| `excludedTestClasses` | _(empty)_ |
| `avoidCallsTo` | _(empty)_ |
| `reportDir` | `build/reports/mutaktor` |
| `outputFormats` | `setOf("HTML", "XML")` |
| `timestampedReports` | `false` |
| `junit5PluginVersion` | `"1.2.3"` |
| `includedGroups` | _(empty)_ |
| `excludedGroups` | _(empty)_ |
| `fullMutationMatrix` | `false` |
| `jvmArgs` | _(empty)_ |
| `mainProcessJvmArgs` | _(empty)_ |
| `pluginConfiguration` | _(empty)_ |
| `features` | _(empty)_ |
| `verbose` | `false` |
| `since` | _(not set)_ |
| `kotlinFilters` | `true` |
| `extreme` | `false` |
| `historyInputLocation` | _(not set)_ |
| `historyOutputLocation` | _(not set)_ |
| `useClasspathFile` | `true` |

## See Also

- [Plugin Architecture](./01-architecture.md)
- [Kotlin Junk Mutation Filter](./03-kotlin-filters.md)
- [Git-Diff Scoped Analysis](./04-git-integration.md)
- [Report Formats and Quality Gate](./05-reporting.md)
