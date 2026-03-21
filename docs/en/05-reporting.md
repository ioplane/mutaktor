---
id: reporting
title: Report Formats and Quality Gate
sidebar_label: Reporting
---

# Report Formats and Quality Gate

![HTML](https://img.shields.io/badge/format-HTML-E34F26?logo=html5&logoColor=white)
![XML](https://img.shields.io/badge/format-XML-blue)
![SARIF](https://img.shields.io/badge/format-SARIF%202.1.0-2088FF?logo=github&logoColor=white)
![Stryker](https://img.shields.io/badge/format-Stryker%20Dashboard-purple)
![Quality Gate](https://img.shields.io/badge/quality%20gate-configurable%20threshold-brightgreen)

## Overview

After PIT finishes mutating and running tests, Mutaktor produces multiple report formats from the native PIT XML output. Each format targets a different consumer: the HTML report is for manual inspection, the mutation-testing-elements JSON feeds the Stryker Dashboard, the SARIF file integrates with GitHub Code Scanning, and the GitHub Checks API surfaces individual survived mutants as inline PR annotations.

```kroki-mermaid
flowchart LR
    PIT[PIT\nmutation analysis] --> XML[mutations.xml\nPIT native XML]
    PIT --> HTML[index.html\nPIT HTML report]
    XML --> ME[MutationElementsConverter\nStryker Dashboard JSON]
    XML --> SARIF[SarifConverter\nSARIF 2.1.0]
    XML --> QG[QualityGate\nmutation score check]
    QG --> GCR[GithubChecksReporter\nCheck Run annotations]
```

## Report Directory Structure

By default, all reports are written to `build/reports/mutaktor/`.

```
build/reports/mutaktor/
├── index.html              # PIT interactive HTML report
├── mutations.xml           # PIT machine-readable XML
└── com/
    └── example/
        └── MyClass.java.html   # Per-class line-level HTML
```

The directory is controlled by `reportDir` in the extension and can be changed:

```kotlin
mutaktor {
    reportDir = layout.buildDirectory.dir("reports/mutation")
}
```

## HTML Report (PIT Native)

The HTML report is generated directly by PIT. It requires no post-processing by Mutaktor.

Enable it by including `"HTML"` in `outputFormats` (the default):

```kotlin
mutaktor {
    outputFormats = setOf("HTML", "XML")
}
```

### What the HTML Report Contains

- A summary page (`index.html`) showing package-level mutation scores with color-coded badges
- Per-class pages showing source code lines annotated with mutation status:
  - Green highlight: all mutations on this line were killed
  - Red highlight: one or more mutations on this line survived
  - Grey: line was not mutated (excluded or not reachable)
- Drill-down to the individual mutation description and killing test name

### Opening the Report Locally

```bash
./gradlew mutate
open build/reports/mutaktor/index.html  # macOS
xdg-open build/reports/mutaktor/index.html  # Linux
start build/reports/mutaktor/index.html  # Windows
```

## Mutation-Testing-Elements JSON (Stryker Dashboard)

`MutationElementsConverter` parses `mutations.xml` and produces a JSON file conforming to the [mutation-testing-elements schema version 2](https://github.com/stryker-mutator/mutation-testing-elements/tree/master/packages/report-schema).

This format is consumed by the [Stryker Dashboard](https://dashboard.stryker-mutator.io/), a hosted service that tracks mutation scores over time.

### Invocation

```kotlin
import io.github.dantte_lp.mutaktor.report.MutationElementsConverter
import java.io.File

val json = MutationElementsConverter.convert(
    mutationsXml = File("build/reports/mutaktor/mutations.xml"),
    sourceRoot = projectDir,
)
File("build/reports/mutaktor/mutation-report.json").writeText(json)
```

### JSON Structure

```json
{
  "schemaVersion": "2",
  "thresholds": { "high": 80, "low": 60 },
  "projectRoot": ".",
  "files": {
    "src/main/java/com/example/UserService.java": {
      "language": "java",
      "source": "package com.example;\n...",
      "mutants": [
        {
          "id": "1001",
          "mutatorName": "ConditionalsBoundaryMutator",
          "replacement": "changed conditional boundary",
          "location": {
            "start": { "line": 42, "column": 1 },
            "end":   { "line": 42, "column": 100 }
          },
          "status": "Killed",
          "killedBy": ["shouldRejectNegativeAge"]
        },
        {
          "id": "1002",
          "mutatorName": "NegateConditionalsMutator",
          "replacement": "negated conditional",
          "location": {
            "start": { "line": 58, "column": 1 },
            "end":   { "line": 58, "column": 100 }
          },
          "status": "Survived",
          "killedBy": []
        }
      ]
    }
  }
}
```

### PIT Status to Stryker Status Mapping

| PIT status | Stryker status |
|---|---|
| `KILLED` | `Killed` |
| `SURVIVED` | `Survived` |
| `NO_COVERAGE` | `NoCoverage` |
| `TIMED_OUT` | `Timeout` |
| `MEMORY_ERROR` | `RuntimeError` |
| `RUN_ERROR` | `RuntimeError` |

## SARIF 2.1.0 (GitHub Code Scanning)

`SarifConverter` parses `mutations.xml` and produces a [SARIF 2.1.0](https://docs.oasis-open.org/sarif/sarif/v2.1.0/sarif-v2.1.0.html) JSON file. Only **survived** mutations are emitted as results — killed mutations indicate correct test coverage and do not warrant developer attention.

Upload the SARIF file to GitHub's Code Scanning API to surface survived mutations as annotations directly on pull request diffs.

### Invocation

```kotlin
import io.github.dantte_lp.mutaktor.report.SarifConverter
import java.io.File

val sarif = SarifConverter.convert(
    mutationsXml = File("build/reports/mutaktor/mutations.xml"),
    pitVersion = "1.23.0",
)
File("build/reports/mutaktor/mutation-results.sarif").writeText(sarif)
```

### SARIF Output Structure

```json
{
  "$schema": "https://raw.githubusercontent.com/oasis-tcs/sarif-spec/main/sarif-2.1/schema/sarif-schema-2.1.0.json",
  "version": "2.1.0",
  "runs": [{
    "tool": {
      "driver": {
        "name": "Mutaktor (PIT)",
        "version": "1.23.0",
        "informationUri": "https://github.com/dantte-lp/mutaktor"
      }
    },
    "results": [
      {
        "ruleId": "mutation/survived",
        "level": "warning",
        "message": {
          "text": "Survived mutation: negated conditional"
        },
        "locations": [{
          "physicalLocation": {
            "artifactLocation": {
              "uri": "src/main/java/com/example/UserService.java"
            },
            "region": { "startLine": 58 }
          }
        }]
      }
    ]
  }]
}
```

### Uploading to GitHub Code Scanning

```yaml
# .github/workflows/mutation.yml
- name: Run mutation testing
  run: ./gradlew mutate

- name: Generate SARIF
  run: |
    # Invoke SARIF conversion (typically wired as a post-task action)
    ./gradlew generateMutationSarif

- name: Upload SARIF to GitHub Code Scanning
  uses: github/codeql-action/upload-sarif@v3
  with:
    sarif_file: build/reports/mutaktor/mutation-results.sarif
    category: mutation-testing
  if: always()
```

The `if: always()` ensures the SARIF is uploaded even when the quality gate fails, so annotations are visible on the PR.

## Quality Gate

`QualityGate` reads `mutations.xml`, counts mutations by status, and computes the mutation score:

```
score = killedMutations * 100 / totalMutations
```

If `totalMutations == 0` (nothing was mutated), the score is `100` — a no-op run is not penalized.

### Result Data Class

```kotlin
data class Result(
    val totalMutations: Int,
    val killedMutations: Int,
    val survivedMutations: Int,
    val mutationScore: Int,    // 0–100
    val passed: Boolean,
    val threshold: Int,
)
```

### Invocation

```kotlin
import io.github.dantte_lp.mutaktor.report.QualityGate
import java.io.File

val result = QualityGate.evaluate(
    mutationsXml = File("build/reports/mutaktor/mutations.xml"),
    threshold = 80,
)

println("Mutation score: ${result.mutationScore}% (threshold: ${result.threshold}%)")
println("Total: ${result.totalMutations}, Killed: ${result.killedMutations}, Survived: ${result.survivedMutations}")

if (!result.passed) {
    throw GradleException("Quality gate failed: score ${result.mutationScore}% < threshold ${result.threshold}%")
}
```

### Quality Gate in CI

```yaml
- name: Check mutation quality gate
  run: |
    ./gradlew mutate checkMutationScore -Pmutation.threshold=80
```

### Score Calculation Examples

| Total | Killed | Score | Threshold | Passed? |
|---|---|---|---|---|
| 100 | 85 | 85% | 80% | Yes |
| 100 | 75 | 75% | 80% | No |
| 0 | 0 | 100% | 80% | Yes (no mutations) |
| 50 | 40 | 80% | 80% | Yes (exactly at threshold) |

## GitHub Checks API Reporter

`GithubChecksReporter` creates a GitHub Check Run named **Mutaktor** on the current commit and adds a warning annotation for every survived mutant. The check run conclusion is `success` if `mutationScore >= threshold`, otherwise `failure`.

### Required Environment Variables

| Variable | Source | Description |
|---|---|---|
| `GITHUB_TOKEN` | GitHub Actions secret | Personal access token or `secrets.GITHUB_TOKEN` with `checks: write` permission |
| `GITHUB_REPOSITORY` | GitHub Actions built-in | `owner/repo` format, e.g. `dantte-lp/mutaktor` |
| `GITHUB_SHA` | GitHub Actions built-in | The commit SHA that triggered the workflow |

### Invocation

```kotlin
import io.github.dantte_lp.mutaktor.report.GithubChecksReporter
import io.github.dantte_lp.mutaktor.report.QualityGate
import java.io.File

val mutationsXml = File("build/reports/mutaktor/mutations.xml")
val result = QualityGate.evaluate(mutationsXml, threshold = 80)
val survived = QualityGate.survivedMutants(mutationsXml)

GithubChecksReporter.report(
    token = System.getenv("GITHUB_TOKEN"),
    repository = System.getenv("GITHUB_REPOSITORY"),
    sha = System.getenv("GITHUB_SHA"),
    mutants = survived,
    mutationScore = result.mutationScore,
    threshold = 80,
)
```

### Annotation Batching

The GitHub Checks API accepts a maximum of 50 annotations per request. When more than 50 mutants survive, `GithubChecksReporter` automatically splits them into batches:

1. The first `POST /repos/{owner}/{repo}/check-runs` request creates the Check Run with the first 50 annotations.
2. Subsequent `PATCH /repos/{owner}/{repo}/check-runs/{id}` requests append the remaining batches.

```kroki-mermaid
sequenceDiagram
    participant R as GithubChecksReporter
    participant API as GitHub API

    R->>API: POST /repos/owner/repo/check-runs\n{ annotations: batch[0..49] }
    API-->>R: 201 Created { id: 12345, html_url: "..." }
    R->>API: PATCH /repos/owner/repo/check-runs/12345\n{ annotations: batch[50..99] }
    API-->>R: 200 OK
    R->>API: PATCH /repos/owner/repo/check-runs/12345\n{ annotations: batch[100..149] }
    API-->>R: 200 OK
```

### Check Run Output

The Check Run summary uses this template:

```
**Mutation Score:** 74% (threshold: 80%)

26 survived mutant(s) detected. Review the annotations below for details.
```

Or, when all mutants are killed:

```
**Mutation Score:** 100% (threshold: 80%)

All mutants were killed. Great test coverage!
```

### Full GitHub Actions Workflow

```yaml
name: Mutation Testing with GitHub Checks

on:
  pull_request:
    branches: [main]

permissions:
  checks: write
  contents: read

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

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Run scoped mutation testing
        run: ./gradlew mutate
        env:
          MUTATION_SINCE: origin/main

      - name: Evaluate quality gate and post GitHub Check
        run: ./gradlew checkMutationGate
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
          GITHUB_REPOSITORY: ${{ github.repository }}
          GITHUB_SHA: ${{ github.sha }}
          MUTATION_THRESHOLD: "80"
        if: always()

      - name: Upload HTML report
        uses: actions/upload-artifact@v4
        with:
          name: mutation-report
          path: build/reports/mutaktor/
        if: always()

      - name: Upload SARIF
        uses: github/codeql-action/upload-sarif@v3
        with:
          sarif_file: build/reports/mutaktor/mutation-results.sarif
          category: mutation-testing
        if: always()
```

## Report Converter Security Notes

Both `MutationElementsConverter` and `SarifConverter` disable XML external entity processing when parsing `mutations.xml`:

```kotlin
factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
```

This prevents XXE (XML External Entity) injection attacks if the `mutations.xml` file is sourced from an untrusted location.

## See Also

- [Plugin Architecture](./01-architecture.md)
- [Configuration DSL Reference](./02-configuration.md)
- [Kotlin Junk Mutation Filter](./03-kotlin-filters.md)
- [Git-Diff Scoped Analysis](./04-git-integration.md)
- [mutation-testing-elements schema](https://github.com/stryker-mutator/mutation-testing-elements/tree/master/packages/report-schema)
- [SARIF 2.1.0 specification](https://docs.oasis-open.org/sarif/sarif/v2.1.0/sarif-v2.1.0.html)
- [GitHub Checks API](https://docs.github.com/en/rest/checks)
