# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Quality gate: fail build if mutation score below threshold (Sprint 6)
- Per-package mutation ratchet with `.mutaktor-baseline.json` (Sprint 10)
- GraalVM + Quarkus auto-detect: auto-resolves standard JDK for PIT via Gradle Toolchain (Sprint 10)
- `@MutationCritical` and `@SuppressMutations` annotations (Sprint 10)
- `mutaktor-annotations` module — zero-dep annotation JAR (Sprint 10)
- Annotation-based PIT interceptor (Sprint 10)
- Multi-module aggregation: `mutateAggregate` task collects subproject reports (Sprint 8)
- Full documentation (8 docs EN + RU, gobfd format) with mermaid diagrams
- CONTRIBUTING.md, SECURITY.md, .gitleaks.toml
- Post-1.0 roadmap: GraalVM jrt:// fix, MCP server, LLM mutant killer, mutation ratchet
- Release workflow: GitHub Actions with JDK 17+25 matrix (Sprint 8)
- Extreme mutation mode: 6 method-body removal mutators, `extreme.set(true)` (Sprint 7)
- GitHub Checks API reporter with inline PR annotations for survived mutants (Sprint 6)
- mutation-testing-elements JSON report converter — Stryker Dashboard compatible (Sprint 5)
- SARIF 2.1.0 report converter — GitHub Code Scanning compatible, survived-only (Sprint 5)
- Git-diff scoped analysis: `mutaktor { since.set("main") }` (Sprint 4)
- Type-safe Kotlin DSL extension with 24 managed properties (Sprint 2)
- PIT execution via JavaExec with full CLI argument builder (Sprint 2)
- `mutaktor` dependency configuration with auto-resolved PIT + JUnit5 plugin (Sprint 2)
- Kotlin junk mutation filter — PIT MutationInterceptor SPI (Sprint 3):
  - DefaultImpls, Intrinsics null-checks, data class methods, coroutine state machine, when-hashcode dispatch
- Auto-infer targetClasses from project.group (Sprint 2)
- Project scaffold: Kotlin 2.3, Gradle 9.4.1, JDK 25 (Sprint 1)
- Multi-module build: `mutaktor-gradle-plugin`, `mutaktor-pitest-filter`, `build-logic` (Sprint 1)
- GitHub Actions CI workflow with JDK 17/21/25 matrix (Sprint 1)
- CLAUDE.md, CHANGELOG.md, LICENSE, .editorconfig (Sprint 1)

[Unreleased]: https://github.com/dantte-lp/mutaktor/commits/main
