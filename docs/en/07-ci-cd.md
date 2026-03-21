---
id: ci-cd
title: CI/CD Integration
sidebar_label: CI/CD
---

# CI/CD Integration

![GitHub Actions](https://img.shields.io/badge/GitHub_Actions-2088FF?style=flat-square&logo=github-actions&logoColor=white)
![SARIF](https://img.shields.io/badge/SARIF-2.1.0-green?style=flat-square)
![Checks API](https://img.shields.io/badge/GitHub_Checks-API-181717?style=flat-square&logo=github)

This document describes how mutaktor itself is built and released in CI, and how to integrate the plugin into your own GitHub Actions workflows.

---

## mutaktor's Own CI/CD Workflows

### CI Workflow (`.github/workflows/ci.yml`)

The CI workflow runs on every push to `main` and every pull request targeting `main`. It validates the plugin across a matrix of three JDK versions.

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

Key decisions:

| Decision | Rationale |
|----------|-----------|
| JDK matrix: 17, 21, 25 | 17 is minimum, 21 is current LTS, 25 is max tested |
| `--no-daemon` | Avoids daemon state pollution between matrix jobs |
| `--warning-mode=all` | Surfaces deprecated API usage early |
| Test reports retained 14 days | Allows post-failure investigation without re-running |
| `if: always()` on upload | Reports are uploaded even when tests fail |

### CI workflow overview

```kroki-mermaid
graph TD
    push["push / pull_request"] --> matrix["Matrix: JDK 17 / 21 / 25"]
    matrix --> checkout["actions/checkout@v4"]
    checkout --> jdk["actions/setup-java@v4\n(Temurin)"]
    jdk --> gradle["gradle/actions/setup-gradle@v4"]
    gradle --> check["./gradlew check\n--no-daemon --warning-mode=all"]
    check --> upload["Upload test reports\n(always, 14 days)"]

    style check fill:#02303A,color:#fff
    style matrix fill:#7F52FF,color:#fff
```

---

### Release Workflow (`.github/workflows/release.yml`)

The release workflow triggers on any tag matching `v*`. It runs a build-and-test job across JDK 17 and 25, then publishes a GitHub Release with the compiled JARs and extracted release notes.

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

Key decisions:

| Decision | Rationale |
|----------|-----------|
| Version stripped from tag (`v*` → `*`) | `gradle.properties` version must match tag without `v` prefix |
| JARs collected from JDK 17 build only | Reproducible artifact; JDK version should not affect JAR contents |
| `fetch-depth: 0` in release job | `awk` script needs full history to find the correct `CHANGELOG.md` section |
| Release notes extracted by `awk` | Fully automated — no manual copy-paste between CHANGELOG and release body |

### Release workflow overview

```kroki-mermaid
graph TD
    tag["git push tag v*"] --> matrix2["Matrix: JDK 17 / 25\nbuild-and-test"]
    matrix2 --> jar["Upload JARs\n(JDK 17 only)"]
    jar --> rel["release job\n(needs: build-and-test)"]
    rel --> notes["Extract release notes\nfrom CHANGELOG.md"]
    notes --> ghrel["gh release create\n+ attach JARs"]

    style tag fill:#e37400,color:#fff
    style ghrel fill:#181717,color:#fff
```

---

## Using Mutaktor in Your CI

### Minimal example

The following workflow runs mutation testing on every pull request and uploads the HTML report as an artifact:

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

### Git-diff scoped analysis

For large codebases, restrict mutation to classes changed in the pull request branch:

```yaml
- name: Run mutation tests (changed classes only)
  run: ./gradlew mutate --no-daemon
  env:
    # The plugin reads this via mutaktor { since.set(...) } or --mutaktor-since
    MUTAKTOR_SINCE: ${{ github.base_ref }}
```

Or configure it statically in `build.gradle.kts`:

```kotlin
mutaktor {
    since.set("origin/main")
}
```

---

## GitHub Checks API

When `GithubChecksReporter` is invoked after the `mutate` task, survived mutants appear as inline warnings on the pull request diff.

### Required environment variables

| Variable | Source | Description |
|----------|--------|-------------|
| `GITHUB_TOKEN` | `${{ secrets.GITHUB_TOKEN }}` | API authentication |
| `GITHUB_REPOSITORY` | Automatically set | `owner/repo` format |
| `GITHUB_SHA` | Automatically set | Commit SHA for the check run |

### Required permission

The workflow job needs write access to checks:

```yaml
jobs:
  mutation:
    runs-on: ubuntu-latest
    permissions:
      checks: write
      contents: read
```

### Full example with Checks API

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
          fetch-depth: 0    # needed for git-diff scoped analysis

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

The `GithubChecksReporter` batches annotations in groups of 50 (the GitHub API limit per request) and creates additional PATCH requests for larger result sets.

### How annotations work

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
    loop batches of 50
        R->>GH: PATCH /repos/{owner}/{repo}/check-runs/12345
    end
    Note over GH: Annotations visible on PR diff
```

---

## SARIF Upload to Code Scanning

SARIF output lets survived mutants appear as Code Scanning alerts in the Security tab of your repository. The alerts persist across runs and can be dismissed with a reason.

### Enable SARIF output

```kotlin
// build.gradle.kts
mutaktor {
    outputFormats.set(setOf("HTML", "XML"))  // XML is required as SARIF input
}
```

The `SarifConverter` reads `mutations.xml` and emits only **survived** mutations — killed mutations are working correctly and are not reported.

### Upload step

```yaml
- name: Run mutation tests
  run: ./gradlew mutate --no-daemon

- name: Convert to SARIF
  run: |
    ./gradlew generateMutationSarif --no-daemon   # task wired by plugin

- name: Upload SARIF to Code Scanning
  uses: github/codeql-action/upload-sarif@v3
  if: always()
  with:
    sarif_file: build/reports/mutaktor/mutations.sarif
    category: mutation-testing
```

### SARIF structure

Each survived mutation becomes a SARIF result with:

| SARIF field | Value |
|-------------|-------|
| `ruleId` | `mutation/survived` |
| `level` | `warning` |
| `message.text` | `Survived mutation: <PIT description>` |
| `artifactLocation.uri` | Relative source file path |
| `region.startLine` | Line number from PIT XML |

The tool driver records `"name": "Mutaktor (PIT)"` and the PIT version string for traceability.

---

## Quality Gate

The quality gate fails the build when the mutation score drops below a configured threshold.

```kotlin
// build.gradle.kts
mutaktor {
    // No explicit threshold property yet — evaluated post-task via QualityGate.evaluate()
}
```

`QualityGate.evaluate()` computes:

```
mutationScore = killedMutations * 100 / totalMutations
passed        = mutationScore >= threshold
```

If `totalMutations == 0` the score is 100 (nothing to test — considered passing).

### Typical CI threshold setup

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

## Caching and Incremental Analysis

Mutaktor's `MutaktorTask` is annotated `@CacheableTask`. Gradle's build cache avoids re-running PIT when inputs have not changed:

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

To share the build cache between CI runs:

```yaml
- uses: gradle/actions/setup-gradle@v4
  with:
    cache-read-only: ${{ github.ref != 'refs/heads/main' }}
```

For incremental PIT analysis across runs, configure history files:

```kotlin
mutaktor {
    historyInputLocation.set(layout.projectDirectory.file(".mutation-history"))
    historyOutputLocation.set(layout.projectDirectory.file(".mutation-history"))
}
```

Commit `.mutation-history` to a cache artifact between CI runs:

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

## See Also

- [06-development.md](06-development.md) — Local build setup and test commands
- [08-changelog.md](08-changelog.md) — Release process: tagging and workflow trigger
- `SarifConverter.kt` — SARIF generation implementation
- `GithubChecksReporter.kt` — GitHub Checks API implementation
- `QualityGate.kt` — Threshold evaluation logic
