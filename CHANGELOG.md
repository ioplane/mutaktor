# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.0] - 2026-03-22

First public release under `io.github.ioplane.mutaktor`.

CE feature-complete release. 135 tests, 4 modules, zero external dependencies.

### Added
- Type-safe Kotlin DSL extension with 25+ managed properties
- PIT execution via `JavaExec` with full CLI argument builder and `@CacheableTask`
- `mutaktor` dependency configuration with auto-resolved PIT + JUnit5 plugin
- Kotlin junk mutation filter — PIT `MutationInterceptor` SPI (5 patterns: DefaultImpls, Intrinsics null-checks, data class methods, coroutine state machine, when-hashcode dispatch)
- Git-diff scoped analysis: `mutaktor { since.set("main") }`
- mutation-testing-elements JSON report — Stryker Dashboard compatible
- SARIF 2.1.0 report — GitHub Code Scanning compatible (survived mutations only)
- Quality gate: fail build if mutation score below threshold (`mutationScoreThreshold`)
- Per-package mutation score ratchet with `.mutaktor-baseline.json`
- GitHub Checks API reporter with inline PR annotations for survived mutants
- Extreme mutation mode: 6 method-body removal mutators (`extreme.set(true)`)
- Multi-module aggregation: `mutateAggregate` task collects subproject reports
- `@MutationCritical` annotation — enforce 100% mutation score on annotated code
- `@SuppressMutations` annotation — exclude annotated code from mutation analysis
- `mutaktor-annotations` module — zero-dependency annotation JAR
- `javaLauncher` property — Gradle Toolchain API for PIT child JVM (GraalVM fix)
- GraalVM + Quarkus auto-detect: auto-resolves standard JDK for PIT via Gradle Toolchain
- Empty targetClasses validation with actionable error message
- Auto-infer targetClasses from `project.group`
- Post-processing pipeline: JSON + SARIF + quality gate + ratchet + GitHub Checks in single `./gradlew mutate`
- Shared XML/JSON utilities (DRY: `XmlParser`, `JsonBuilder`, `SourcePathResolver`)
- Git flag injection prevention (sinceRef validation)
- `TIMED_OUT` and `MEMORY_ERROR` counted as killed mutations in quality gate
- Full EN + RU documentation (8 docs each, mermaid diagrams)
- Enterprise GitHub configuration: CI (JDK 17/21/25 + CodeQL), release pipeline, security scanning (Trivy), OpenSSF Scorecard, SonarCloud, Codecov, dependabot, issue/PR templates

### Security
- All XML parsing disables DTD processing (XXE prevention)
- All GitHub Actions pinned to full commit SHAs
- All workflow permissions at job level (least privilege)

[Unreleased]: https://github.com/ioplane/mutaktor/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/ioplane/mutaktor/releases/tag/v0.1.0
