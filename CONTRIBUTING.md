# Contributing to mutaktor

Thank you for your interest in contributing. This document covers the development environment, code standards, testing requirements, and pull request process.

By participating in this project you agree to abide by the [Code of Conduct](CODE_OF_CONDUCT.md).

---

## Table of Contents

- [Quick Start](#quick-start)
- [Development Environment](#development-environment)
- [Project Structure](#project-structure)
- [Code Standards](#code-standards)
- [Testing Requirements](#testing-requirements)
- [Pull Request Process](#pull-request-process)
- [PR Checklist](#pr-checklist)
- [Commit Message Format](#commit-message-format)
- [Issue Templates](#issue-templates)
- [Release Process](#release-process)
- [License](#license)

---

## Quick Start

```bash
git clone https://github.com/dantte-lp/mutaktor.git
cd mutaktor
./gradlew check    # build + unit tests + functional tests
```

All tests must pass before opening a pull request. The `check` task runs both unit tests and Gradle TestKit functional tests.

---

## Development Environment

The recommended development environment uses the project's devcontainer, which provides a consistent JDK 25 + Gradle 9.4.1 toolchain. Native builds are also supported on any machine with JDK 17+.

**Minimum requirements:**

| Tool | Version |
|---|---|
| JDK | 17 (build); 25 recommended |
| Gradle | Wrapper provided — do not install separately |
| Git | 2.x |

**Build commands:**

| Command | What it does |
|---|---|
| `./gradlew check` | Build + unit tests + functional tests (full CI equivalent) |
| `./gradlew test` | Unit tests only (all modules) |
| `./gradlew functionalTest` | Gradle TestKit end-to-end tests |
| `./gradlew :mutaktor-pitest-filter:test` | Filter module unit tests only |
| `./gradlew :mutaktor-gradle-plugin:test` | Plugin module unit tests only |
| `./gradlew assemble` | Compile all modules, no tests |

Do not run Gradle builds outside the devcontainer on shared machines — PIT spawns child JVMs and the build is resource-intensive.

---

## Project Structure

The repository is a multi-module Gradle build:

```
mutaktor/
├── mutaktor-gradle-plugin/     # Gradle plugin (applied to consumer projects)
│   └── src/main/kotlin/.../
│       ├── MutaktorPlugin.kt            # Plugin entry point — wires extension, task, deps
│       ├── MutaktorExtension.kt         # 25 abstract managed properties + conventions
│       ├── MutaktorTask.kt              # @CacheableTask — JavaExec wrapper, PIT CLI args
│       ├── MutaktorAggregatePlugin.kt   # Multi-module report aggregation
│       ├── git/GitDiffAnalyzer.kt       # git diff → targetClasses filter
│       ├── report/
│       │   ├── MutationElementsConverter.kt  # PIT XML → mutation-testing-elements JSON
│       │   ├── SarifConverter.kt             # PIT XML → SARIF 2.1.0
│       │   ├── QualityGate.kt                # Score evaluation + threshold check
│       │   └── GithubChecksReporter.kt       # GitHub Checks API annotations
│       ├── extreme/ExtremeMutationConfig.kt  # Method-body removal mutator list
│       ├── ratchet/                          # Per-package score ratchet
│       └── toolchain/                        # GraalVM / JDK auto-detection
│
├── mutaktor-pitest-filter/     # PIT plugin JAR (loaded by PIT runtime via SPI)
│   └── src/main/kotlin/.../
│       └── pitest/KotlinJunkFilter.kt   # MutationInterceptor — 5 filter patterns
│
├── mutaktor-annotations/       # Zero-dependency annotation JAR
│   └── src/main/kotlin/.../annotations/
│       ├── MutationCritical.kt    # @MutationCritical — require 100% score
│       └── SuppressMutations.kt   # @SuppressMutations — exclude from analysis
│
├── build-logic/                # Internal convention plugins
│   └── src/main/kotlin/
│       └── kotlin-conventions.gradle.kts  # Shared Kotlin + JVM toolchain config
│
└── docs/
    ├── en/     # English documentation (8 docs)
    └── ru/     # Russian documentation (8 docs)
```

**Module responsibilities:**

- `mutaktor-gradle-plugin` — The primary artifact. Consumers apply this plugin. Contains all Gradle integration, task logic, git analysis, and post-processing pipeline.
- `mutaktor-pitest-filter` — Packaged separately because it is loaded by PIT's own classloader at runtime, not by Gradle. Must not depend on Gradle API.
- `mutaktor-annotations` — An optional consumer dependency. Zero transitive dependencies. Retained at runtime so PIT's bytecode scanner can read annotation values.
- `build-logic` — Applies consistent Kotlin compiler settings, JVM target, and test configuration across all modules. Not published.

---

## Code Standards

### Language

All production code is Kotlin. Do not introduce Groovy or Java in production sources. Build scripts (`build.gradle.kts`, `settings.gradle.kts`) are Kotlin DSL.

### Gradle Provider API

All task inputs and outputs must use the Provider API:

```kotlin
// Correct
abstract val targetClasses: SetProperty<String>
abstract val reportDir: DirectoryProperty

// Wrong — causes configuration-cache incompatibility
var targetClasses: Set<String> = emptySet()
```

Never call `.get()` on a `Provider` during the configuration phase. Use `.map {}`, `.flatMap {}`, and `.convention()` to build lazy chains. Evaluate values only inside task actions.

### Zero external dependencies

The plugin has zero external runtime dependencies beyond the Gradle API and PIT itself. Do not add third-party libraries. Use JDK stdlib equivalents:

| Instead of | Use |
|---|---|
| Gson / Jackson | `StringBuilder`-based JSON construction |
| OkHttp | `java.net.http.HttpClient` (JDK 11+) |
| DOM4J / JDOM | `javax.xml.parsers.DocumentBuilderFactory` |
| Apache Commons IO | `java.nio.file` |

### `@CacheableTask`

`MutaktorTask` is annotated `@CacheableTask`. All task fields must be serializable for the configuration cache. Never store a `Project` reference or any non-serializable type in a task field. Use `@Internal` for fields that should be excluded from cache key computation.

### Kotlin conventions

- Packages use underscores: `io.github.dantte_lp.mutaktor`
- Public API members must have explicit visibility modifiers (`public`, `internal`, `private`)
- Prefer `abstract` properties on `@CacheableTask` classes and extension objects (Gradle managed properties)
- Use `layout.buildDirectory` — never `project.buildDir` (removed in Gradle 9)
- Use `tasks.register` — never `tasks.create` (eager API)
- Use `project.javaexec` replacement: `JavaExec` task type via `tasks.register<JavaExec>` — never `project.exec()` or `project.javaexec()` (removed in Gradle 9)

### XML parsing hardening

All XML parsing of PIT output must disable DTD processing:

```kotlin
val factory = DocumentBuilderFactory.newInstance()
factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
```

---

## Testing Requirements

### Unit tests

Located in `src/test/kotlin/`. Use JUnit 5 with Kotest assertions:

```kotlin
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class QualityGateTest {
    @Test
    fun `passes when score meets threshold`() {
        val gate = QualityGate(threshold = 80)
        gate.evaluate(score = 85) shouldBe GateResult.PASSED
    }
}
```

Every new class or function requires a corresponding unit test. The following have zero tests and are priority targets for new contributors (tracked in the v0.2.0 backlog):

- `MutaktorTask.buildPitArguments()` — core CLI argument construction
- `GithubChecksReporter` — GitHub API client

### Functional tests

Located in `mutaktor-gradle-plugin/src/functionalTest/`. Use Gradle TestKit to run full builds against a synthetic project. Every new user-visible feature requires a functional test:

```kotlin
@Test
fun `quality gate fails build when score is below threshold`() {
    // arrange: write build.gradle.kts with mutationScoreThreshold = 90
    // act: GradleRunner.create().withPluginClasspath().build()
    // assert: result.task(":mutate")?.outcome == TaskOutcome.FAILED
}
```

Functional tests are slow. Scope them tightly — test one behavior per test, use the smallest possible synthetic project.

### Minimum coverage

New code should not decrease overall test coverage. The CI pipeline enforces a minimum line coverage threshold (configured in `build-logic`).

---

## Pull Request Process

1. **Fork** the repository and create a feature branch from `main`:
   ```bash
   git checkout -b feat/my-feature
   ```

2. **Make changes** with tests. Run `./gradlew check` frequently.

3. **Update CHANGELOG.md** under `[Unreleased]` for any user-facing change. Follow [Keep a Changelog 1.1.0](https://keepachangelog.com/en/1.1.0/) format.

4. **Update documentation** in `docs/en/` if the change affects user-visible behavior. Russian documentation in `docs/ru/` should also be updated if you are able.

5. **Verify the full suite passes:**
   ```bash
   ./gradlew check
   ```

6. **Open a Pull Request** against `main`. Fill in the PR template completely.

7. A maintainer will review within a reasonable time. Address review comments in the same branch. Do not force-push after review has started.

---

## PR Checklist

Before marking a PR ready for review, confirm every item:

- [ ] `./gradlew check` passes locally
- [ ] Unit tests added or updated for changed logic
- [ ] Functional test added for new user-visible behavior
- [ ] `CHANGELOG.md` updated under `[Unreleased]`
- [ ] `docs/en/` updated if user-visible behavior changed
- [ ] No `Project` references in task fields (configuration cache)
- [ ] No new external dependencies introduced
- [ ] DTD disabled for any new XML parsing
- [ ] Deprecation warnings added for any breaking change (with migration path)
- [ ] PR description explains what and why, not just what

---

## Commit Message Format

This project uses [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/):

```
<type>(<scope>): <subject>

[optional body]

[optional footer]
```

**Types:** `feat`, `fix`, `docs`, `refactor`, `test`, `chore`, `perf`, `ci`

**Scope:** module name or area — `plugin`, `filter`, `annotations`, `git`, `sarif`, `json`, `gate`, `ratchet`, `toolchain`, `docs`, `ci`

**Examples:**

```
feat(gate): add per-package score ratchet with auto-update

fix(filter): suppress coroutine state machine mutations in Kotlin 2.3

docs(en): add configuration reference for javaLauncher property

test(plugin): add functional test for git-diff scoped analysis

chore(deps): bump PIT to 1.23.0
```

Subject line: imperative mood, no period, 72 characters maximum.

---

## Issue Templates

Use the appropriate issue template when filing a report:

- **Bug report** — unexpected behavior, build failures, incorrect output
- **Feature request** — new capability or enhancement
- **Documentation** — inaccurate or missing documentation

When filing a bug report, include the information described in [SUPPORT.md](SUPPORT.md).

---

## Release Process

Releases are driven by CI. Maintainers only:

1. Update `version` in `gradle.properties` (e.g., `0.1.0-SNAPSHOT` → `0.1.0`)
2. Move `[Unreleased]` entries in `CHANGELOG.md` to a new version section with the release date
3. Commit: `chore(release): prepare v0.1.0`
4. Tag: `git tag v0.1.0`
5. Push tag: `git push origin v0.1.0`

The GitHub Actions release workflow triggers on `v*` tags, runs the full test matrix (JDK 17 / 21 / 25), and publishes to the Gradle Plugin Portal.

---

## Versioning

- [Semantic Versioning 2.0.0](https://semver.org/)
- [Keep a Changelog 1.1.0](https://keepachangelog.com/en/1.1.0/)
- `0.x.y` — pre-1.0, public API may change between minor versions
- `1.0.0` — stable public API, breaking changes require major version bump

---

## License

By contributing, you agree that your contributions will be licensed under the [Apache License 2.0](LICENSE).
