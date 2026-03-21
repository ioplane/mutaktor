# Final PM & QA Assessment — Mutaktor v0.1.0

**Date:** 2026-03-21
**Project:** mutaktor — https://github.com/ioplane/mutaktor

---

## PM Assessment

### Executive Summary

8 sprints delivered in a single session. All planned features implemented.
57 tests green. Full bilingual documentation (EN+RU). Zero external dependencies.

### Sprint Burndown

| Sprint | Goal | Tests | Status |
|--------|------|-------|--------|
| 1. Scaffold | Kotlin 2.3 + Gradle 9.4.1 + CI | 5 | DONE |
| 2. Plugin Core | Type-safe DSL + PIT execution | +9 = 14 | DONE |
| 3. Kotlin Filter | 5 junk mutation patterns | +13 = 27 | DONE |
| 4. Git-Diff | `since.set("main")` scoped analysis | +10 = 37 | DONE |
| 5. Reporting | mutation-testing-elements JSON + SARIF | +9 = 46 | DONE |
| 6. CI/CD | Quality gate + GitHub Checks API | +5 = 51 | DONE |
| 7. Extreme | Method-body removal mode | +4 = 55 | DONE |
| 8. Multi-module | Aggregation + release + README + docs | +2 = **57** | DONE |

### Codebase Metrics

| Metric | Value |
|--------|-------|
| Production Kotlin | ~2,200 LOC |
| Test Kotlin | ~800 LOC |
| Documentation (EN+RU) | ~6,200 LOC |
| **Total** | **~9,200 LOC** |
| Modules | 3 (plugin, filter, build-logic) |
| DSL properties | 25 |
| PIT filter patterns | 5 |
| Report formats | 3 (HTML, JSON, SARIF) |
| External dependencies | **0** |

### Feature Matrix

| Feature | Sprint | Unique vs competitors? |
|---------|--------|----------------------|
| Type-safe Kotlin DSL | 2 | Yes (vs Groovy szpak) |
| PIT via JavaExec + @CacheableTask | 2 | Standard |
| Kotlin junk filter (5 patterns) | 3 | Yes (vs paid ArcMutate) |
| Git-diff scoped (`since`) | 4 | Yes (vs paid ArcMutate) |
| mutation-testing-elements JSON | 5 | Yes (built-in) |
| SARIF 2.1.0 | 5 | Yes (nobody else) |
| Quality gate | 6 | Yes (built-in) |
| GitHub Checks API | 6 | Yes (nobody else) |
| Extreme mutation | 7 | Yes (vs paid ArcMutate) |
| Multi-module aggregation | 8 | Standard |

### Post-1.0 Roadmap

| Version | Theme | Key features |
|---------|-------|-------------|
| v0.2.0 | QA + GraalVM | `javaLauncher`, per-package ratchet, `@MutationCritical`, QA fixes |
| v0.3.0 | AI-native | MCP server, LLM mutant killer |
| v0.4.0 | Analytics | Mutation debt tracker, difficulty scoring |
| v1.0.0 | GA | Stable API, Plugin Portal, equivalent mutant detector |

---

## QA Assessment

### Test Results

| Module | Pass | Fail | Skip |
|--------|------|------|------|
| Plugin unit tests | 12 | 0 | 0 |
| Plugin funcTests | 4 | 0 | 1 |
| Filter unit tests | 13 | 0 | 0 |
| Git-diff tests | 10 | 0 | 0 |
| Report tests | 9 | 0 | 0 |
| Quality gate tests | 5 | 0 | 0 |
| Extreme tests | 3 | 0 | 0 |
| Aggregate tests | 2 | 0 | 0 |
| **Total** | **57** | **0** | **1** |

### QA Scorecard

| Area | Rating | Notes |
|------|--------|-------|
| Test coverage | **WARN** | MutaktorTask + GithubChecksReporter untested |
| Test quality | **PASS** | Meaningful tests with edge cases |
| Edge cases | **WARN** | Hardcoded paths, TIMED_OUT scoring, empty targetClasses |
| Architecture | **PASS** | Clean separation, proper Gradle patterns |
| API/DSL | **PASS** | Ergonomic, sensible defaults |
| Security | **WARN** | XML safe; git flag injection risk; fragile JSON regex |
| Config cache | **WARN** | Claimed but test disabled |
| Documentation | **PASS** | 8 docs EN + 8 docs RU, gobfd format, mermaid |

### Top 5 Issues for v0.2.0

| # | Severity | Issue |
|---|----------|-------|
| 1 | FAIL | MutaktorTask.buildPitArguments() — 0 unit tests |
| 2 | FAIL | Hardcoded `src/main/java/` — Kotlin files get wrong path |
| 3 | FAIL | Config cache test disabled — headline feature not validated |
| 4 | WARN | GithubChecksReporter — 0 tests |
| 5 | WARN | Duplicated escapeJson/XML parsing in 4 files |

### Killer Features Analysis (7 proposals evaluated)

| # | Feature | Feasibility | Impact | Priority |
|---|---------|-------------|--------|----------|
| 1 | Per-package mutation ratchet | Easy | HIGH | v0.2.0 |
| 2 | `@MutationCritical` annotation | Easy | HIGH | v0.2.0 |
| 3 | `javaLauncher` (GraalVM fix) | Easy | HIGH | v0.2.0 |
| 4 | MCP server for AI agents | Medium | HIGH | v0.3.0 |
| 5 | LLM mutant killer | Medium | HIGH | v0.3.0 |
| 6 | Mutation debt tracker | Medium | HIGH | v0.4.0 |
| 7 | Difficulty scoring | Medium | MEDIUM | v0.4.0 |

### GraalVM jrt:/ Issue

Discovered by QA testing gradle-pitest-plugin v2.0.0 on GraalVM. PIT minion
JVM fails because GraalVM stores classes in `jrt:/` module paths, not JARs.
Fix: `javaLauncher` property → Gradle Toolchain API → Temurin for PIT child.
**No other mutation testing plugin addresses this.**
